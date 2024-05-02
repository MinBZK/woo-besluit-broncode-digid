
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

# model for authentication means (middel) "password tool"
class PasswordTool < ActiveRecord::Base
  module Status
    ACTIVE    = 'active'
    # blocked - strictly for aanvragen via balie, which are useless as long as they are not printed
    BLOCKED   = 'blocked'
    INACTIVE  = 'inactive'
  end

  include Stateful

  has_many :attempts, -> { where attempt_type: 'activation' }, as: :attemptable, dependent: :destroy

  # returns the activationcode part
  def controle_code
    activation_code
  end

  # add an attempt
  def add_one_attempt
    attempts << Attempt.new(attempt_type: 'activation')
    reached_activationcode_limit?
  end

  # retrieves the current attempts and compare with max
  def reached_activationcode_limit?
    pogingen_activationcode = ::Configuration.get('pogingen_activationcode')
    if attempts.size >= pogingen_activationcode
      pogingen_activationcode
    else
      false
    end
  end

  # delete all attemps
  def delete_attempts
    attempts.each(&:destroy)
  end

  def reset_code
    update_attributes(activation_code: nil)
    delete_attempts
  end

  # check the expiry date of an activation code.
  # return the expiry time (days) if expired
  # return false if ok
  # geldigheidstermijn:
  # basis/midden: XX
  def expired?
    if updated_at < (Time.now - geldigheidstermijn.to_i.days)
      return geldigheidstermijn
    end
    false
  end

  # match the controle_code with code
  def controle_code_match?(code)
    if Rails.application.config.integration_mode
      code.upcase == 'PPPPPPPPP'
    else
      controle_code.upcase == code.upcase && !state.blocked?
    end
  end
end
