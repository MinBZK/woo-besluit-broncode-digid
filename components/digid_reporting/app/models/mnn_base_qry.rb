
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

class MnnBaseQry
  def initialize(name:, type:, filter_by_webservices: false)
    @name = name
    @type = type
    @codes = get_codes()
    @webservice_ids ||= []
    @filter_by_webservices = filter_by_webservices

    @time_now = Time.now
    @beginning_of_day = @time_now.beginning_of_day

    # Metrics are the Active records we store the query result in
    @metric = get_or_create_metric(@name)
    # Metric cumulative is its own record in the database.
    @metric_cumulative = get_or_create_metric("#{@name}_cumul")

    # Some time variables which will come in handy later
    @end_timeframe = @time_now
    @begin_timeframe = @metric.updated_at

    # Munin counters should contain data of only 1 day. If a day has passed handle logic
    if @begin_timeframe < @beginning_of_day
      reset_metrics
      @begin_timeframe = @beginning_of_day
    end
  end

  def update_metrics_timestamp
    # Abuse the field updated_at to save the end of the timeframe
    # This is used to determine the start of the next timeframe
    @metric.updated_at = @end_timeframe.utc.strftime('%Y-%m-%d %H:%M:%S')
    @metric_cumulative.updated_at = @end_timeframe.utc.strftime('%Y-%m-%d %H:%M:%S')
    @metric.save()
    @metric_cumulative.save()
  end

  def run
    query_result = self.execute_query

    Rails.logger.debug("#{@name} has result: #{query_result}")

    if Time.now.beginning_of_day > @beginning_of_day
      Rails.logger.error("#{@name} query execution finished in next day. Not counting it's results to keep todays metrics accurate")
      return
    end

    # Tested that even if the updated_at field is changed in the database vs updated_at in memory now,
    # This query will only touch the total field. Which is desired, leaving this comment here for confirmation
    @metric.total = query_result
    @metric_cumulative.total = @metric_cumulative.total + query_result
    @metric.save
    @metric_cumulative.save
    return query_result
  end

  def reset_metrics
    @metric.updated_at = @beginning_of_day.utc.strftime('%Y-%m-%d %H:%M:%S')
    @metric_cumulative.updated_at = @beginning_of_day.utc.strftime('%Y-%m-%d %H:%M:%S')
    @metric.total = 0
    @metric_cumulative.total = 0
    @metric.save()
    @metric_cumulative.save()
  end

  protected
  def get_codes
    # Default implementation is to get the codes by type. Child classes can choose to overwrite this
    if @type
      get_codes_by_descriptions(get_code_descriptions_by_type(@type))
    else
      raise NotImplementedError.new("get_codes not implemented for '#{@name}', without type")
    end
  end

  def get_or_create_metric(metric_name)
    metric = Metric.where(:name => metric_name).first()
    unless metric
      # The average field is deprecated
      metric = Metric.new(
        :name => metric_name,
        :average => 0,
        :total => 0,
        :created_at => @time_now.utc.strftime('%Y-%m-%d %H:%M:%S'),
        :updated_at => @beginning_of_day.utc.strftime('%Y-%m-%d %H:%M:%S')
      )
      metric.save()
    end

    return metric
  end

  def execute_query
    from = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"

    # Initialize a where hash so we have an optional filter for webservice_id in the base class
    # Active record .where() filtering on a hash only supports comparing using '=' so the created filter is specified seperately
    where = {:code => @codes}
    if @filter_by_webservices
      where[:webservice_id] = @webservice_ids
    end

    begin
      return Log.from(from)
      .where("created_at > ?", @begin_timeframe.strftime('%Y-%m-%d %H:%M:%S'))
      .where("created_at <= ?", @end_timeframe.strftime('%Y-%m-%d %H:%M:%S'))
      .where(where)
      .count
    rescue StandardError => e
      Rails.logger.error "Error occured during query execution of '#{@name}', Error message '#{e.message}'"
      return 0
    end
  end

  def get_codes_by_descriptions(descriptions)
    codes = []
    descriptions.each do |description|
      codes << BURGER_LOG_MAPPINGS[description]
    end
    return codes
  end

  def get_code_descriptions_by_type(type)
    case type
    when :success
      return get_success_codes
    when :failure
      return get_failure_codes
    when :attempt
      return get_attempt_codes
    else
      raise ArgumentError.new("Unknown type '#{type}' in '#{@name}'")
    end
  end
end
