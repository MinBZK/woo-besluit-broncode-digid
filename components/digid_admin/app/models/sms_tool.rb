
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

class SmsTool < AccountBase
  belongs_to :account

  module Status
    ACTIVE    = 'active'.freeze
    INACTIVE  = 'inactive'.freeze # DEPRICATED remove 5.8
    PENDING   = 'pending'.freeze # uitbreiding
  end

  include Stateful

  delegate :active?, :pending?, to: :state

  def expired?
    return geldigheidstermijn if updated_at < geldigheidstermijn.to_i.days.ago
    false
  end

  def user_friendly_phone_number
    phone_number.to_s.gsub(/^\+316/, '06').gsub(/^00/, '+')
  end

  def human_status
    I18n.t(status, scope: 'accounts.sms_tool.status')
  end
end
