
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

# This model is used in the MyDigiD add and change e-mail address screens
module MyDigid
  class Email
    include(ActiveModel::Model)

    attr_accessor(:address)

    validates(:address, format: { allow_blank: true, with: Regexp.only(CharacterClass::EMAIL, ignore_case: true) },
                        length: { allow_blank: true, in: 0..254 },
                        presence: true)
    validates(:address, format: { with: Regexp.only(CharacterClass::EMAIL_LEN_TO_AT_SIGN, ignore_case: true) })

    validate :email_adress_maximum?, if: -> { errors[:address].empty? }

    def email_adress_maximum?
      account_ids = ::Email.joins(:account).where("accounts.status": [::Account::Status::ACTIVE, ::Account::Status::REQUESTED]).where(adres: address).pluck(:account_id)
      return if account_ids.uniq.count < ::Configuration.get_int("E-mailadres_maximum")
      errors.add(:address, I18n.t("you_have_reached_the_email_maximum"))
    end
  end
end
