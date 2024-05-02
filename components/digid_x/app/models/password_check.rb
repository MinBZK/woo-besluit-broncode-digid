
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

class PasswordCheck
  include ActiveModel::Validations
  POLICY = 2

  validates :password,
            format: { with: Regexp.only(CharacterClass::PASSWORD) },
            length: { in: 8..32 }

  # CharacterClasses the password should include
  validates(:password, format: { with: CharacterClass::CAPITALS, message: :too_few_capitals })
  validates(:password, format: { with: CharacterClass::DIGITS, message: :too_few_digits })
  validates(:password, format: { with: CharacterClass::MINUSCULES, message: :too_few_minuscules })
  validates(:password, format: { with: CharacterClass::SPECIAL_CHARACTERS, message: :too_few_special_characters })

  validate(:password_not_username?, if: -> { password.present? })

  attr_accessor :username, :password

  def initialize(username:, password:)
    @username = username
    @password = password
  end

  # validate if the password is not the same as the username
  def password_not_username?
    errors.add(:password, :contains_username) if username.present? && password.include?(username)
  end
end
