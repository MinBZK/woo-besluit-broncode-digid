
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
# Updates historical database from cron.
class HistoricalRunner

  def self.run
    logger = Rails.logger
    # Set end timestamp (end_of_day_yesterday)
    end_ts = Time.now.yesterday.end_of_day
    file_name = "/tmp/historical_data_runner"
    file = File.new(file_name, "a")
    # QQQ Get PID and put it into file?
    file.write("#{$$}")
    can_lock = file.flock(File::LOCK_EX | File::LOCK_NB)
    logger.debug "#{__method__} ===> Wrote lock file for process #{$$}"

    if can_lock == false
      logger.warn "#{__method__} ===> #{report_class.name} can't set lock, assuming another process is still running."
      exit 1
    else
      # Get a list of history reports
      AdminReport.subclasses.find_all { |klass| klass.name =~ /^Report.*Hist.*$/ }.map do |report_class|
        logger.info "#{__method__} ===> report: #{report_class}. name: #{report_class.name}"
        # Call the retrieve_historical_data method sequentially on all reports
        begin
          report = report_class.new
          report.update_history(end_ts)
          logger.debug "#{__method__} ===> #{report_class.name} updated for end date #{end_ts}"
        rescue Exception => e
          logger.error "#{__method__} ===> Error updating {report_class.name} #{end_ts} (#{report_class.name}): #{e.message}"
          logger.debug e.backtrace.join("\n")
        ensure
          file.flock(File::LOCK_UN)
          file.close
          File.delete(file_name)
          logger.debug "#{__method__} ===> removed lock file."
        end
      end
    end
  end
end
