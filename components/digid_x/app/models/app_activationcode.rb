
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

class AppActivationcode
  ALLOWED_LETTERS = (("A".."Z").to_a - %w(A E O I U Y)).freeze
  CODE_SIZE = 6

  include ActiveModel::Model
  attr_accessor :activationcode, :expected_activationcode, :app_authenticator

  validates :activationcode, code: { allow_blank: true, starts_with: "K" },
                             presence: true, length: { minimum: 6, maximum: 6 }

  def self.generate
    Array.new(4) { ALLOWED_LETTERS.sample }.join
  end

  def start_with_k?
    activationcode.upcase.start_with?("K")
  end

  private

  def activationcode_correct?
    return false if activationcode.blank?
    activationcode.casecmp(expected_activationcode) == 0
  end
end
