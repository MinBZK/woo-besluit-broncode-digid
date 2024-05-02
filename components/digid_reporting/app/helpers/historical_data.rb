
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
# Uitility to access historical reports
class HistoricalData
  CLASS_NAME = 'HistoricalData'

  def self.update_runner
    file = File.new("/tmp/historical_data_runner", "a")
    can_lock = file.flock(File::LOCK_EX | File::LOCK_NB)

    if can_lock == false
      exit 1
    else
      #do whatever you want
    end
  end

   def initialize(report_name, ts)
    me = "#{CLASS_NAME}.#{__method__} called with parameters report_name #{report_name} and ts #{ts}."
    @base_name = report_name
    @end_ts = ts
    @logger = Rails.logger   #Logger.new(STDOUT)
    @logger.debug "DEBUG #{me}  ===> Instantiate historical report for #{@base_name}."
  end


  # Update existing record or create a new one
  def update(values)
    me = "#{CLASS_NAME}.#{__method__}"
    @logger.debug "DEBUG #{me}  ===> update with #{values.inspect}"
    ts = Time.now
    values = values.to_s
    report = HistoricalReport.where(:base_name => @base_name).first
    if report.present?
      report.update_attributes(:report_data => values, :counted_till => @end_ts, :updated_at => ts)
    else
      HistoricalReport.create!(:base_name => @base_name, :report_data => values, :counted_till => @end_ts,
          :updated_at => ts, :created_at => ts)
    end
  end

  # Return a hash as the original function
  def get_data
    me = "#{CLASS_NAME}.#{__method__}"
    @logger.debug "DEBUG #{me}  ===> fetching from database ..."
    values = nil
    data = nil
    rep = HistoricalReport.where(:base_name => @base_name).first
    if rep.present?
      data = eval(rep.report_data)
    end
    if data.present?
      #~ values = {}
      #~ data.each do |row|
        #~ values[row[0]] = row[1]
      #~ end
      values = data
    end
    @logger.debug "DEBUG #{me}  ===> fetched #{values}"
    return values
  end

  # True, if historical data is up-to-date
  def check_data?
    me = "#{CLASS_NAME}.#{__method__}"
    result = false
    rep = HistoricalReport.where(:base_name => @base_name).first
    if rep.present?
      rep_date = rep.counted_till
      @logger.debug "DEBUG #{me}  ===> found historical report date: #{rep_date}"
      result = @end_ts <= rep_date
    end
    @logger.debug "DEBUG #{me}  ===> is #{result}"
    return result
  end

  # Return end date or nil.
  def get_end_date
    me = "#{CLASS_NAME}.#{__method__}"
    @logger.debug "DEBUG #{me}  ===> getting"
    result = nil
    rep = HistoricalReport.where(:base_name => @base_name).first
    if rep.present?
      result = rep.counted_till
    end
    @logger.debug "DEBUG #{me}  ===> got #{result}"
    return result
  end

end
