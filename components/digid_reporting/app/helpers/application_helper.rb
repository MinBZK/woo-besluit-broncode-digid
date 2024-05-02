
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

module ApplicationHelper
    PERIOD_HEADER = "Periode"

    PERIOD_FORMATS = {
      ReportParam::DAY => "%Y-%m-%d",
      ReportParam::WEEK => "%G#%V",
      ReportParam::MONTH => "%Y-%m",
      ReportParam::YEAR => "%Y",
      ReportParam::DATETIME => "%Y-%m-%d %T",
      ReportParam::TIME => "%T" }

   def get_day_start_end(date)
      #
      # Use last day as period if no day
      # is supplied.
      if date.nil?
         start_ts = Time.now.yesterday.beginning_of_day
      else
         start_ts = Time.parse("#{date}").beginning_of_day
      end

      end_ts = start_ts.end_of_day

      return start_ts, end_ts
   end

   def get_week_start_end(date)
      #
      # Use last week as period if no date
      # is supplied.
      if date.nil?
         start_ts = Time.now.prev_week.beginning_of_week
      else
         start_ts = Time.parse("#{date}").beginning_of_week
      end

      end_ts = start_ts.next_week()

      return start_ts, end_ts
   end

   def get_month_start_end(date)
      #
      # Use last month as period if no date
      # is supplied.
      if date.nil?
         start_ts = Time.now.prev_month.beginning_of_month
      else
         start_ts = Time.parse("#{date}").beginning_of_month
      end

      end_ts = start_ts.next_month()

      return start_ts, end_ts
   end

    def get_year_start_end(date)
      #
      # Use last year as period if no date
      # is supplied.
      if date.nil?
         start_ts = Time.now.prev_year.beginning_of_year
      else
         start_ts = Time.parse("#{date}").beginning_of_year
      end

      end_ts = start_ts.next_month()

      return start_ts, end_ts
   end

  #
  # Returns the numeric month value of the date given
  # or with no argument, the prevoius month.
  def get_month(date)
      if date.nil?
         #a_month = Time.now.prev_month.month
         a_month = Time.now.prev_month.strftime("%Y-%m")
      else
         #a_month = Time.parse("#{date}").month
         a_month = Time.parse("#{date}").strftime("%Y-%m")
      end
      return a_month
  end

  def get_history_start
    me = "Application_helper.#{__method__}"
    logger.debug "DEBUG #{me}. ===> called."
    max_logging_age = 18 # default retention time of logs table in month
    history_start = nil
    max_logging_age = APP_CONFIG['max_logging_age']
    historical_reporting_start = APP_CONFIG['historical_reporting_start']

    # Fetch retention period from configuration
    if max_logging_age.present?
      logger.debug "DEBUG #{me}. ===> Max log age from configuration is #{max_logging_age} month."
    end
    if historical_reporting_start.present?
      ts = Time.parse(historical_reporting_start)
      logger.debug "DEBUG #{me}. ===> Historical reporting starts from #{ts}"
      #~ if ts > Time.now.months_ago(max_logging_age)
        history_start = ts
      #~ else
        #~ # Send alert to admin app
        #~ logger.error "ERROR #{me}. ===> Historical reporting starts before available data."
      #~ end
    end
    return history_start
  end

  # Set timestamps for history reporting, jira581
  def get_historical_interval(end_ts, hist_ts)
    me = "Application_helper.#{__method__}"
    logger.debug "DEBUG #{me}. ===> called parameters end date #{end_ts} and hist_ts #{hist_ts}."
    #hist_ts = hist_ts  #get_historical_end_date
    if hist_ts.nil?
      hist_ts = get_history_start
    end
    if hist_ts.present?
      history_start_ts = hist_ts.beginning_of_day
      logger.debug "DEBUG #{me}. ===> found start date #{history_start_ts}."

      history_end_ts = end_ts
      if history_start_ts > history_end_ts
        # QQQ raise exception?
        logger.error "ERROR #{me} -> History start date greater than end date."
        history_start_ts = history_end_ts = nil
      end
    else
      history_start_ts = history_end_ts = nil
      logger.debug "DEBUG #{me} -> No history start date found."
    end
    logger.debug "DEBUG #{me} -> Time range historical reporting is from #{history_start_ts} to #{history_end_ts}"
    return history_start_ts, history_end_ts
  end

  def lookup_codes(citizen=nil, admin=nil)
    codes = []
    citizens = []
    admins = []

    if citizen.present?
      citizens << citizen
      citizens&.each do |c|
        if (c.is_a?(String) || c.is_a?(Array)) && BURGER_LOG_MAPPINGS.values_at(*c).compact.present?
          codes += BURGER_LOG_MAPPINGS.values_at(*c)
        else
          codes << c
        end
      end
    end

    if admin.present?
      admins << admin
      admins&.each do |a|
        if a.is_a?(String) || a.is_a?(Array)
          codes += ADMIN_LOG_MAPPINGS.values_at(*a)
        else
          codes << a
        end
      end
    end

    codes
  end

  def first_snapshot_after(start_date)
    start_ts, end_ts = get_day_start_end(start_date)
    Snapshot.where("created_at > ?", end_ts).order("created_at ASC").first
  end

  def snapshot_before(snapshot)
    # QQQ
    # Snapshot.where("id < ?", snapshot.id).order("id DESC").first
    Snapshot.where("created_at < ?", snapshot.created_at).order("created_at DESC").first
  end

  def enclosing_snapshots(start_date)
    counter_snapshot = first_snapshot_after start_date
    previous_snapshot = snapshot_before counter_snapshot

    # Return in "temporal" order
    return previous_snapshot, counter_snapshot
  end

  # Send an alert to munin/alarm on the current environment
  # The initiator calls alert and supplies the parameters.
  # Subject and Body cannot be empty.
  # Level is at the moment not used in mailer method.
  # QQQ Should this be moved to application_helper.rb ?

  def send_alert(params)
    # Original code from digid_x doesn't work in this context (error: "end of file reached")
    # QQQ Probably because of https (http in digid_x) and/or certificate mismatch
    # uri = APP_CONFIG['alarm_url']
    # Net::HTTP.post_form URI.parse(uri), params
    Rails.logger.debug "#{__method__}: called with #{params.inspect}"
    parameters = nil
    if params.present?
      parameters = param_to_hash(params)
    end
    uri = URI.parse(APP_CONFIG['alarm_url'])

    http = Net::HTTP.new(uri.host, uri.port)
    http.use_ssl = (uri.scheme.casecmp("https") == 0 ? true : false)
    # QQQ The certificate won't match the ip address used. Ignore that.
    http.verify_mode = OpenSSL::SSL::VERIFY_NONE

    request = Net::HTTP::Post.new(uri.request_uri)
    request.set_form_data(parameters)

    response = http.request(request)

    result = Net::HTTPSuccess === response
    if result
      Rails.logger.info "#{__method__}: alert sent successfully:\n #{params.inspect}"
    else
      Rails.logger.error "#{__method__}: unexpected HTTP response: #{response.to_s} for alert:\n #{params.inspect}"
    end

    result
  end

  def param_to_hash(symbol_hash)
    Rails.logger.debug "#{__method__}: called with #{symbol_hash.inspect}"
    result = {}
    if symbol_hash.present?
      symbol_hash.each_key do |key|
        Rails.logger.debug "#{__method__}: key #{key.inspect}"
        if key.is_a? Symbol
          k = key.to_s
        else
          k = key
        end
        result[k] = symbol_hash[key]
      end
    end
    return result
  end

  def reporting_server?
    APP_CONFIG['reporting_server'] ? true : false
  end

  def get_balie_from_subject(result)
    me = "Application_helper.#{__method__}"
    logger.debug "DEBUG #{me}. ===> called with result:#{result} ."
    balie = nil

    if result.subject_id.present?
      balie = Balie.where(:id => result.subject_id).first
    else
      logger.warning "#{me}  ===> No Balie found! Skipped this record."
    end

    return balie
  end

  def get_balie_from_account(result)
    me = "Application_helper.#{__method__}"
    logger.debug "DEBUG #{me}. ===> called with result:#{result} ."
    balie = nil

    if result.present? or result.account_id.present?
      if Account.where(:id => result.account_id).first.via_balie?
        logger.debug "#{me}  ===> Found account from balie."
        de = DistributionEntity.where(:account_id => result.account_id).first
        balie = Balie.where(:id => de.balie_id).first
      else
        logger.warning "#{me}  ===> No account found! Skipped this record."
      end
    else
        logger.warning "#{me}  ===> No account_id! Skipped this record."
    end

    return balie
  end

  #
  # Prepare for report parameter
  def prepare_report_param(period, start_date = nil, report_class_name = "")
    me = "Application_helper.#{__method__}"
    Rails.logger.debug "DEBUG #{me} -> for a : #{period} with start_date #{start_date}"
    rep_param = ReportParam.new

    case period
      when ReportParam::DAY then
        rep_param.report_class_name = report_class_name + "Daily"
        # Use yesterday as period if no date
        # is supplied.
        rep_param.start_ts, rep_param.end_ts = get_day_start_end(start_date)
      when ReportParam::WEEK then
        rep_param.report_class_name = report_class_name + "Weekly"
        # Use last week as period if no date
        # is supplied.
        rep_param.start_ts, rep_param.end_ts = get_week_start_end(start_date)
      when ReportParam::MONTH then
        rep_param.report_class_name = report_class_name + "Monthly"
        # Use last month as period if no date
        # is supplied.
        rep_param.start_ts, rep_param.end_ts = get_month_start_end(start_date)
      when ReportParam::YEAR then
        rep_param.report_class_name = @report_class_name + "Yearly"
        # Use last year as period if no date
        # is supplied.
        rep_param.start_ts, rep_param.end_ts = get_year_start_end(start_date)
      else
        Rails.logger.warn "#{me} ===> Unexpected else in case type block."
    end
    rep_param.period_value = format_report_period(period, rep_param.start_ts)
    rep_param.period_header = PERIOD_HEADER

    return rep_param
  end

  #
  # Format a date into standard period notation
  def format_report_period(period, start_date)
    return start_date.strftime(PERIOD_FORMATS[period])
  end

  #
  # Format an arbitrary date into a defined string format
  def format_timestamp(ts)
    return ts.strftime(PERIOD_FORMATS[ReportParam::DATETIME])
  end

  #
  # Get ReportParam periode from string
  def get_report_param_periode(periode)
    me = "Application_helper.#{__method__}"
    Rails.logger.debug "DEBUG #{me} -> for: #{periode}"
    result = nil
    if periode.upcase.match(/MONTH/).present?
      result = ReportParam::MONTH
    elsif periode.upcase.match(/WEEK/).present?
      result = ReportParam::WEEK
    elsif periode.upcase.match(/FRAUD|INTEGRITY/).present?
      result = ReportParam::DAY
    else
      Rails.logger.warn "WARN #{me} -> no match found!"
    end
    return result
  end
end
