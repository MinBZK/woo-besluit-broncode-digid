
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

# keeps track of sms activationcode submits
class SmsChallenge < ActiveRecord::Base
  module Status
    CORRECT   = 'correct' # goed beantwoord
    INCORRECT = 'incorrect' # fout beantwoord
    PENDING   = 'pending' # onbeantwoord
    RECOVERY  = 'recovery' # mobiel herstel
  end

  include Stateful

  belongs_to :account

  # tells if the sms challenge is expired
  def expired?
    geldigheid_sms = ::Configuration.get('geldigheid_sms')
    created_at < (Time.now - geldigheid_sms.minutes)
  end

  # check to see if user has exceeded the max sms attempt
  def max_exceeded?
    pogingen_sms = ::Configuration.get('pogingen_sms')
    # 3 strikes, you're out!
    attempt >= pogingen_sms
  end
end
