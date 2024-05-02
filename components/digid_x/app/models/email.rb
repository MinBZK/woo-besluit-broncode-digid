
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

class Email < ActiveRecord::Base
  module Status
    NOT_VERIFIED = "not_verified"
    VERIFIED = "verified"
    BLOCKED = "blocked"
  end

  include Stateful
  include HasAttempts

  attr_accessor :no_email

  belongs_to :account
  after_destroy :create_account_history

  default_value_for :status, Status::NOT_VERIFIED

  has_many :attempts, -> { where(attempt_type: "email") }, as: :attemptable, dependent: :destroy

  validates :adres,
            format: { with: Regexp.only(CharacterClass::EMAIL, ignore_case: true) },
            length: { maximum: 254 }
  validates :adres,
            format: { with: Regexp.only(CharacterClass::EMAIL_LEN_TO_AT_SIGN, ignore_case: true) }

  validate :email_adress_maximum?, if: -> { adres_changed? && errors["adres"].empty? }

  validate :no_email_present?

  # check the expiry date of an email.
  # return the expiry time (days) if expired
  # return false if ok
  def expired?
    geldigheid_controlecodes = ::Configuration.get_int("geldigheid_controlecodes")
    return geldigheid_controlecodes if updated_at < geldigheid_controlecodes.days.ago
    false
  end

  def not_verified?
    status == Status::NOT_VERIFIED
  end

  def create_account_history
    if !destroyed_by_association
      AccountHistory.create(account_id: account.id, gebruikersnaam: account.password_authenticator&.username, email_adres: adres)
    end
  end

  def adres=(adres)
    self[:adres] = adres.try(:downcase)
  end

  def max_number_of_failed_attempts
    ::Configuration.get_int("pogingen_controlecodes")
  end

  def email_adress_maximum?
    account_ids = self.class.joins(:account).where("accounts.status": [Account::Status::ACTIVE, Account::Status::REQUESTED]).where(adres: adres).pluck(:account_id)
    return if account_ids.uniq.count < ::Configuration.get_int("E-mailadres_maximum")

    errors.add(:adres, I18n.t("you_have_reached_the_email_maximum"))
  end

  def no_email_present?
    errors.add(:no_email) if no_email == "1"
  end

  def confirmation_expired?
    if status == Email::Status::VERIFIED
      if confirmed_at.nil?
        # only do this because there are null values in emails table for confirmed_at
        update_attribute(:confirmed_at, updated_at)
      end

      maximum_period = confirmed_at + ::Configuration.get_int("maximum_period_unconfirmed_email").months
      if Time.zone.now > maximum_period
        return true
      end
    end

    false
  end
end
