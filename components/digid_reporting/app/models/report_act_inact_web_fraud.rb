
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

#
# Lists the active and inactive webservices
#
class ReportActInactWebFraud < AdminReport
  ACTIVE = "actief"
  INACTIVE = "inactief"

  def self.report_name
    'Aantal actieve inactieve webdiensten'
  end

  def self.report (start_date = nil)
    me = "ReportActInactWeb.#{__method__}"
    ts = Time.now.strftime("%Y-%m-%d")

    row_headers = ["Datum", "Webdienst", "Webdienst id", "actief/ inactief"]

    logger.debug "#{me} DEBUG ===> for #{ts}"

    result = [row_headers]

    Webservice.all.each do |row|
      result << [ts, row.name, row.id, row.active ? ACTIVE : INACTIVE ]
    end

    logger.debug "#{me} DEBUG ===> result is #{result}"
    return result
  end
end
