
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

class SharedSecret < AccountBase
  belongs_to :aselect_webservice

  attr_reader :original_shared_secret

  def self.table_name
    'aselect_shared_secrets'
  end

  def reset_shared_secret
    self.shared_secret      = nil
    @original_shared_secret = nil
  end

  def generate_shared_secret
    return original_shared_secret if original_shared_secret
    part_time               = (Time.now.to_f * 1000).to_i.to_s(36)
    part_random             = SecureRandom.hex(((24 - part_time.length) / 2) + 1)
    combined                = part_random[0...(24 - part_time.length)] + part_time
    @original_shared_secret = combined.scan(/\w{4}/).join('-').upcase
    self.shared_secret      = Digest::SHA1.hexdigest(@original_shared_secret).upcase
    @original_shared_secret
  end
end
