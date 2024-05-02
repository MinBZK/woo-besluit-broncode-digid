
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

class ReportSuccessAuthWebMonthly < AdminReport
    UC_LABELS = ['uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt', 'uc2.authenticeren_digid_app_gelukt',
                 'uc2.authenticeren_substantieel_gelukt', 'uc2.authenticeren_digid_app_to_app_gelukt', 'uc2.authenticeren_digid_app_to_app_substantieel_gelukt',
                 'uc2.authenticeren_hoog_gelukt', 'uc2.authenticeren_digid_app_to_app_hoog_gelukt']

  def self.report_name
    'Aantal gelukte authenticaties per webdienst per sector per maand'
  end

  def self.report(start_date = nil)
    rep = ReportAuthWeb.new(ReportParam::MONTH, UC_LABELS)
    return rep.report(start_date)
  end
end
