
# Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
# gericht is op transparantie en niet op hergebruik. Hergebruik van 
# de broncode is toegestaan onder de EUPL licentie, met uitzondering 
# van broncode waarvoor een andere licentie is aangegeven.
# 
# Het archief waar dit bestand deel van uitmaakt is te vinden op:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
# 
# Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
# 
# This code has been disclosed in response to a request under the Dutch
# Open Government Act ("Wet open Overheid"). This implies that publication 
# is primarily driven by the need for transparence, not re-use.
# Re-use is permitted under the EUPL-license, with the exception 
# of source files that contain a different license.
# 
# The archive that this file originates from can be found at:
#   https://github.com/MinBZK/woo-besluit-broncode-digid
# 
# Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
#   https://www.ncsc.nl/contact/kwetsbaarheid-melden
# using the reference "Logius, publicly disclosed source code DigiD" 
# 
# Other questions regarding this Open Goverment Act decision may be
# directed via email to open@logius.nl

# frozen_string_literal: true

# model AccountAdmin implements the following webservice endpoints:
# - AanvragenSectorAccountOnderwater
# - RevocerenAccount

class AccountAdmin
  include ActiveModel::Model

  validates :gebruikersnaam, presence: true, length: { in: 6..32 }
  validate :gebruikersnaam_uniek?

  validates :wachtwoord,
            length: { in: 8..32 },
            presence: true,
            format: { with: Regexp.only(CharacterClass::PASSWORD) }

  # CharacterClasses the wachtwoord should include
  validates :wachtwoord, format: { with: CharacterClass::CAPITALS }
  validates :wachtwoord, format: { with: CharacterClass::DIGITS }
  validates :wachtwoord, format: { with: CharacterClass::MINUSCULES }
  validates :wachtwoord, format: { with: CharacterClass::SPECIAL_CHARACTERS }

  validate :password_not_username?, if: -> { wachtwoord.present? }

  validates :emailadres,
            format: { with: Regexp.only(CharacterClass::EMAIL, ignore_case: true) },
            if: -> { emailadres.present? }

  validates :sectoraalnummer, length: { in: 1..15 }

  attr_accessor :gebruikersnaam, :wachtwoord, :emailadres, :sectoraalnummer, :sectorcode

  # creates a onderwater against the validations
  # could return any of:
  # (0000) Operatie geslaagd
  # (0030) Incorrecte accountgegevens
  # (0031) Gebruikersnaam reeds in gebruik.
  # (0032) Wachtwoord niet voldoende sterk.
  # (0033) Emailadres niet correct.
  def self.onderwater(soap_request)
    aanvraag = prep_aanvraag soap_request
    account_admin = AccountAdmin.new
    account_admin.gebruikersnaam  = aanvraag[:gebruikersnaam]
    account_admin.wachtwoord      = aanvraag[:wachtwoord]
    account_admin.emailadres      = aanvraag[:emailadres]
    account_admin.sectoraalnummer = aanvraag[:sectoraalnummer]
    account_admin.sectorcode      = aanvraag[:sectorcode]
    account_admin_result = account_admin.valid?
    if account_admin_result
      account_id = create_account(aanvraag)
      account_admin_result = "0000"
      Log.instrument("272", account_id: account_id, webservice_id: soap_request[:webservice_id])
    else
      account_admin_result = find_error(account_admin.errors)
      Log.instrument("273", reason: I18n.t("transactiecode.tc-#{account_admin_result}", locale: :nl), webservice_id: soap_request[:webservice_id])
    end
    account_admin_result
  end

  # sets status of said account to "ingetrokken"
  def self.revoceren(soap_request)
    sectorcode = soap_request[:Sectorgegevens][:Sectorcode]
    sectoraalnummer = soap_request[:Sectorgegevens][:SectoraalNummer]
    revoke_account = Sectorcode.where(sectoraalnummer: sectoraalnummer,
                                      sector_id: sectorcode_to_id(sectorcode))
    revoceren_result = "0030" # not found
    if revoke_account.present?
      account = Account.find(revoke_account.first.account_id)
      if account.present?
        account.state.suspended? ? Log.instrument("277", account_id: account.id, webservice_id: soap_request[:webservice_id]) : Log.instrument("276", account_id: account.id, webservice_id: soap_request[:webservice_id])
        account.update_attribute(:status, ::Account::Status::REVOKED)
        revoceren_result = "0000" # found
      else
        Log.instrument("278", webservice_id: soap_request[:webservice_id])
      end
    end
    revoceren_result
  end

  private #----------------------------------------------------------------------------------------

  private_class_method def self.sectorcode_to_id(sectorcode)
    Sector.find_by(number_name: sectorcode).id
  end

  # creates a test account and returns the id
  private_class_method def self.create_account(aanvraag)
    account = Account.new do |a|
      a.status                     = ::Account::Status::ACTIVE
      a.herkomst                   = aanvraag[:herkomst]
    end

    account.email = Email.new(adres: aanvraag[:emailadres], status: ::Email::Status::VERIFIED) if aanvraag[:emailadres].present?
    account.sectorcodes.build(sectoraalnummer: aanvraag[:sectoraalnummer], sector_id: sectorcode_to_id(aanvraag[:sectorcode]))
    account.password_authenticator = Authenticators::Password.new(status: Authenticators::Password::Status::ACTIVE, username: aanvraag[:gebruikersnaam], password: aanvraag[:wachtwoord], password_confirmation: aanvraag[:wachtwoord], issuer_type: "letter_international")
    account.save(validate: false)
    account.id
  end

  # creates a flat aanvraag onderwater
  private_class_method def self.prep_aanvraag(soap_request)
    aanvraag = {}
    # if any of these is not available we throw an error and catch it with rescue -> syntax error
    aanvraag[:sectorcode]       = soap_request[:Sectorgegevens][:Sectorcode]
    aanvraag[:sectoraalnummer]  = soap_request[:Sectorgegevens][:SectoraalNummer]
    aanvraag[:gebruikersnaam]   = soap_request[:Accountgegevens][:Gebruikersnaam]
    aanvraag[:wachtwoord]       = soap_request[:Accountgegevens][:Wachtwoord]
    aanvraag[:emailadres]       = soap_request[:Accountgegevens][:Emailadres]
    aanvraag[:herkomst]         = soap_request[:webservice_id]
    aanvraag
  end

  # (0000) Operatie geslaagd
  # (0030) Incorrecte accountgegevens
  # (0031) Gebruikersnaam reeds in gebruik.
  # (0032) Wachtwoord niet voldoende sterk.
  # (0033) Emailadres niet correct.
  private_class_method def self.find_error(errors)
    if errors["gebruikersnaam"].present?
      "0030"
    elsif errors["gebruikersnaam_uniek"].present?
      "0031"
    elsif errors["wachtwoord"].present?
      "0032"
    elsif errors["emailadres"].present?
      "0033"
    else
      "0030"
    end
  end

  # different from the one in Account model, since we do not need to produce alternatives
  def gebruikersnaam_uniek?
    authenticators = Authenticators::Password.where("username = ? COLLATE utf8mb4_bin", gebruikersnaam)
    errors.add(:gebruikersnaam_uniek, "0031") if authenticators.count > 0
  end

  # validate if the password is not the same as the username
  def password_not_username?
    errors.add(:wachtwoord, "0032") if wachtwoord.include?(gebruikersnaam)
  end
end
