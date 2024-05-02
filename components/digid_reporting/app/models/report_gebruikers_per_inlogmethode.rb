
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

# Report DigiD-app counters
#
# The reports counts the ne users diferentiated to logon method
#
#
class ReportGebruikersPerInlogmethode < AdminReport
  # Construct a report object. The period parameter describe the report
  # period. Default is a week.
  # The period is a constant from the ReportParam class
  def initialize(period = ReportParam::WEEK)
    @rep_param = prepare_report_param(period, Time.now, self.class.name)
    @period = period
  end

  def report (start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, self.class.name)
    result = []
    result << ["Periode", "Alleen wachtwoord totaal", "Wachtwoord en sms totaal",
      "Wachtwoord, sms en enkel DigiD app(s) (midden) totaal", "Wachtwoord, sms en tenminste 1 DigiD app(s) (substantieel) totaal",
      "Wachtwoord en enkel DigiD app(s) (midden) totaal", "Wachtwoord en tenminste 1 DigiD app(s) (substantieel) totaal",
      "Enkel DigiD app(s) (midden) totaal", "Tenminste 1 DigiD app(s) (substantieel) totaal"]
    result << [@rep_param.period_value, [Account.ww_no_sms_no_app(@rep_param), Account.ww_sms_no_app(@rep_param), 
      Account.ww_sms_only_midden_app(@rep_param),  Account.ww_sms_at_least_substantial(@rep_param),
      Account.ww_no_sms_only_midden_app(@rep_param),  Account.ww_no_sms_at_least_substantial(@rep_param),
      Account.no_ww_no_sms_only_midden_app(@rep_param), Account.no_ww_no_sms_at_least_substantial(@rep_param)
    ].map(&:count)].flatten
    result
  end
end
