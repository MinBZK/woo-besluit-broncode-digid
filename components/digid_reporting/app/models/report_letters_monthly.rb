
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

      # CLASSNAME : repBrfMnt
      # BRON  : week_maandRaportage.csv rij 24 (see also mantis:6842)
      # RETURN: maand, aantal brieven / status
      # JOINS :
      # WHERE : activation_letters.status;  group by activation_letters.status  (mogelijke status: init,finished,sent)

      #
      # Use last month as period if no date
      # is supplied.
      class ReportLettersMonthly < AdminReport
        def self.report_name
          'Aantal brieven in briefbestand aangemaakt niet aangemaakt per maand'
        end

        def self.report(start_date = nil)
          report_header = ["Maand", "Status", "Aantal"]
          start_ts, end_ts = get_month_start_end(start_date)

          # Count and fetch all records in the given period.
          letters = ActivationLetter.where(:created_at => (start_ts..end_ts)).group(:status).count

          month = start_ts.month
          # QQQ remove literals
          result = [report_header]
          letters.each do |status, count|
            result << [month, status, count]
          end

          return result
        end
      end
