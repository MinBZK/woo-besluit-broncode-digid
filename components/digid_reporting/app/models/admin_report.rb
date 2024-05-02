
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

include ApplicationHelper

class AdminReport < ReportingBase
  def self.table_name
    "admin_reports" # QQQ which is the default table name for this class
  end

  class Type
    FRAUD      = "fraud"
    INTEGRITY  = "integrity"
    MONTHLY    = "monthly"
    WEEKLY     = "weekly"
    SEC        = "sec"
    STD        = "std"
    ADHOC_LOG  = "adhoc_log"
    ADHOC_GBA  = "adhoc_gba"
    ADHOC_TX   = "adhoc_tx"
  end

    @@CategoryForType = { ::AdminReport::Type::ADHOC_GBA => :ad_hoc_frauderapportages_accounts,
      ::AdminReport::Type::ADHOC_LOG => :ad_hoc_frauderapportages_log,
      ::AdminReport::Type::ADHOC_TX  => :ad_hoc_frauderapportages_transacties,
      ::AdminReport::Type::MONTHLY   => :maandrapportages,
      ::AdminReport::Type::WEEKLY    => :weekrapportages,
      ::AdminReport::Type::STD       => :standaardrapportages,
      ::AdminReport::Type::FRAUD     => :frauderapportages,
      ::AdminReport::Type::INTEGRITY => :integriteitsrapportages,
      ::AdminReport::Type::SEC       => :security_rapportages
    }

  # Exclusion to prevent mass assignment problems
  # Suggestion from MdH.
  # QQQ check if this is safe
  #QQQ error in rails 4.1x attr_accessible :name, :report_type, :csv_content, :interval_start, :interval_end, :created_at, :updated_at, :manager_id

  #QQQ new behaviour in rails 4.1.x
  # config.cache_classes is not enough for the .subclasses to work
  #Rails.application.eager_load!

  def self.lookup_type_pattern(report_type)
    pattern = case report_type
    when ::AdminReport::Type::MONTHLY then
      /^Report.*Monthly$/
    when ::AdminReport::Type::WEEKLY then
      /^Report.*Weekly$/
    when ::AdminReport::Type::FRAUD then
      /^Report.*Fraud$/
    when ::AdminReport::Type::INTEGRITY
      /^Report.*Integrity$/
    else
      logger.debug "#{__method__} =>  unforeseen report_type = #{report_type}"
      nil
    end
  end

  def self.interval_start_end(report_type, start_date)
    start_ts, end_ts = case report_type
    when ::AdminReport::Type::MONTHLY then
      get_month_start_end start_date
    when ::AdminReport::Type::WEEKLY then
      get_week_start_end start_date
    when ::AdminReport::Type::FRAUD then
      get_day_start_end(start_date)
    when ::AdminReport::Type::INTEGRITY then
      #although snapshots are created every day, integrity reporting also
      #works in case a snapshot is "missed"
      snapshot, previous_snapshot = get_snapshots(start_date)
      [(previous_snapshot.present? ? previous_snapshot.created_at : nil),
      snapshot.present? ? snapshot.created_at : nil]
    else
      logger.debug "#{__method__} =>  unforeseen report_type = #{report_type}"
      [nil, nil]
    end
  end

  def self.send_report_alerts(result, report_type, name, content)
    subject = nil
    body = nil

    #default body and subject
    subject = "Rapport "
    subject += result ? "OK" : "NOK"
    subject += ": #{name}"
    body = "Het rapport '#{name}' is#{!result ? ' vanwege een fout NIET' : '' } aangemaakt."

    #override body and subject for integrity and fraud reports
    case report_type
    when AdminReport::Type::FRAUD
      # Alert if the fraud report contains more than just a header row
      if content.count > 1
        subject = "Fraude rapport niet leeg",
        body = "Het fraude rapport is niet leeg: #{name}"
      end
    when AdminReport::Type::INTEGRITY
      # Generate alert if first column on first data row is not "OK"
      if content.present? && content.count > 1 && content[1][0] != "OK"
        subject = "NOK integriteitsrapport",
        body = "Het integriteitsrapport #{name} constateert mogelijke integriteitsproblemen (NOK)"
      end
    else
      logger.debug "#{__method__} =>  unforeseen report_type = #{report_type}"
    end

    begin
      send_alert :category => @@CategoryForType[report_type], :subject => subject, :body => body
    rescue Exception => e
      logger.error "Error generating alert for report #{name}: #{e.message}"
      logger.debug e.backtrace.join("\n")
    end
  end

  def self.single_setup(report_type, name, start_ts, end_ts, batch_ts, manager_id=nil)
    rpt = nil
    begin
      rpt = self.create!(
        :report_type => report_type,
        :name => name,
        :interval_start => start_ts,
        :interval_end => end_ts,
        :created_at => Time.now,
        :updated_at => Time.now,
        :batch_started_at => batch_ts,
        :manager_id => manager_id
      )
    rescue Exception => e
      logger.error "Error setting up report #{name}, #{start_ts} - #{end_ts}: #{e.message}"
      logger.debug e.backtrace.join("\n")
    end

    return rpt
  end

  def self.batch_setup (report_type, start_date)
    pattern = lookup_type_pattern(report_type)

    start_ts, end_ts = interval_start_end(report_type, start_date)
    batch_ts = Time.now

    reports = {}

    AdminReport.subclasses.find_all { |klass| klass.name =~ pattern }.map do |report_class|
      name = report_class.report_name #+ " maand #{start_ts.strftime('%Y-%m')}"

      if ((report_type == ::AdminReport::Type::WEEKLY && !APP_CONFIG['weekly_report_whitelist'].split(';').include?(name)) ||
          (report_type == ::AdminReport::Type::MONTHLY && !APP_CONFIG['monthly_report_whitelist'].split(';').include?(name)) ||
          (report_type == ::AdminReport::Type::FRAUD && !APP_CONFIG['fraud_report_whitelist'].split(';').include?(name)))
        logger.info "#{__method__} ===> report: #{report_class}. name: #{report_class.name}. Skipped."
        next
      end

      logger.info "#{__method__} ===> report: #{report_class}. name: #{report_class.name}."
      reports[report_class] = single_setup(report_type, name, start_ts, end_ts, batch_ts)
    end

    return reports
  end

  def self.single_run(cls, rpt, start_date)
    rpt.update!(
      :report_started_at => Time.now,
      :updated_at => Time.now
    )

    result = true
    content = nil
    name = ''

    begin
      name = cls.report_name
      content = cls.report start_date
    rescue Exception => e
      logger.error "Error generating report #{cls} / #{name}: #{e.message}"
      logger.debug e.backtrace.join("\n")
      result = false
    end

    if result && !content.nil? then
      #Adhoc reports already return CSV strings
      if content.kind_of?(String) then
        csv_string = content
      else
        csv_string = CSV.generate do |csv|
          content.each do |r|
              csv << r
          end
        end
      end
    else
      logger.warn "#{__method__} ===> Error or result [set] is empty for report #{cls} / #{name}."
    end

    rpt.update!(
      :report_ended_at => Time.now,
      :csv_content => (result ? csv_string : nil),
      :lines => (result ? csv_string.lines.count : nil),
      :result => result,
      :updated_at => Time.now
    )

    send_report_alerts result, rpt.report_type, name, content

    return result
  end

  def self.batch_run(reports_to_run, start_date)
    reports_run = []

    reports_to_run.each do |cls, rpt|
      result = single_run(cls, rpt, start_date)

      reports_run << name unless !result
    end

    return reports_run
  end

  def self.generate_report_batch(report_type, start_date)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => report_type = #{report_type}, start_date = #{start_date}"

    reports = batch_setup(report_type, start_date)
    return batch_run(reports, start_date)
  end

  #
  # Iterate over all monthly reports
  # create csv  strings from the results
  # and store csv report in AdminReport table.
  # Take a date as optional parameter.
  def self.generate_monthly_reports(start_date = nil)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    return generate_report_batch(::AdminReport::Type::MONTHLY, start_date)
  end

  #
  # Iterate over all weekly reports
  # create csv  strings from the results
  # and store csv report in AdminReport table.
  # Take a date as optional parameter.
  def self.generate_weekly_reports(start_date = nil)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    return generate_report_batch(::AdminReport::Type::WEEKLY, start_date)
  end

  #
  # Iterate over all fraud reports
  # create csv  strings from the results
  # and store csv report in AdminReport table.
  # Take a date as optional parameter.
  def self.generate_fraud_reports(start_date = nil)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    return generate_report_batch(::AdminReport::Type::FRAUD, start_date)
  end

  def self.get_snapshots(start_date)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    # Which snapshots to use is based on the end of the (day) interval based on <start_date>.
    # The counters from the snapshot immediately after the end of the (day) interval are to be used.
    # These counters related to the snapshot immediately before that.

    # Clgr:next statement will produce an exception, let's catch it Q&D'
    begin
      previous_snapshot, snapshot = enclosing_snapshots start_date
    rescue Exception => e
      logger.error "ERROR #{method_name} => No previous snapshot for #{start_date}. enclosing_snapshots threw exception #{e.message} ."
    end

    if snapshot.nil?
      logger.debug "DEBUG #{method_name} => No snapshot found for #{start_date}."
    end
    if previous_snapshot.nil?
      logger.debug "DEBUG #{method_name} => No previous snapshot for #{start_date}"
    end

    return [snapshot, previous_snapshot]
  end

  def self.get_snapshot_present(start_date)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    get_snapshots(start_date).all? { |s| s.present? }
  end

  #
  # Iterate over all integrity reports
  # create csv  strings from the results
  # and store csv report in AdminReport table.
  # Take a date as optional parameter.
  def self.generate_integrity_reports(start_date = nil)
    method_name = __method__
    logger.debug "DEBUG #{method_name} => start_date = #{start_date}"

    snapshot_present = get_snapshot_present(start_date)

    # If both snapshots are present we can compare
    if snapshot_present
      return generate_report_batch(::AdminReport::Type::INTEGRITY, start_date)
    else
      logger.warn "WARN #{method_name} => No snapshots found, skip integrity reports."
    end

    return []
  end

  def self.generate_adhoc_report(cls_instance)
    method_name = __method__
    logger.debug "DEBUG #{method_name}"

    alert_subject = ''
    name = cls_instance.report_name
    rpt = single_setup(cls_instance.report_type, name, cls_instance.periode_start, cls_instance.periode_end, nil, cls_instance.manager_id)
    result = single_run cls_instance, rpt, nil

    return (result ? [name] : nil)
  end

  #
  # Run a single report.
  # Parameter:
  #   report class name - mandatory
  #   start date - default is today
  #
  def self.run_report(report, start_date = nil)
    method_name = __method__
    logger.debug "DEBUG #{method_name} =>  #{report.to_s} with start_date = #{start_date}"

    report_class = AdminReport.subclasses.find_all { |klass| klass.name =~ /#{report}/ }.first

    logger.debug "DEBUG #{method_name} =>  report_class: #{report_class}"

    # Extract type string from class name
    report_type = get_report_type(report_class.name.scan( /[AFIMW][a-z]*$/ ).first.downcase) if report_class.present?
    logger.debug "DEBUG #{method_name} =>  type: #{report_type}"
    name = report_class.report_name
    rep_param = prepare_report_param(get_report_param_periode(report_type), start_date, report_class.name) if report_type.present?

    rpt = single_setup report_type, name, rep_param.start_ts, rep_param.end_ts, nil
    single_run report_class, rpt, start_date

    return name
  end

  def self.cleanup_reports(start_date = nil)
    method_name = __method__
    logger.debug "#{method_name} =>  start_date = #{start_date}"
    #All reports expire after <factor> times their reporting interval length
    #The maximum (and default) interval length is 1.month
    expiry_factor = APP_CONFIG['expiry_factor']
    #Default to 7 as a precautionary measure against premature cleanup in case no factor is configured
    if expiry_factor.nil? then expiry_factor = 7 end

    if start_date.nil?
      ts = Time.now
    end

    expiries = { 1.day   => [::AdminReport::Type::INTEGRITY, ::AdminReport::Type::FRAUD],
                 1.week  => [::AdminReport::Type::WEEKLY, ::AdminReport::Type::ADHOC_LOG, ::AdminReport::Type::ADHOC_GBA, ::AdminReport::Type::ADHOC_TX],
                 1.month => nil }

    expiries.each do |exp, report_types|
      since = ts - (exp * expiry_factor)
      since = since.end_of_day    # fix for 1 of error
      # Log execution duration
      rec_count = 0
      exec_time = time do
        expired_reports = AdminReport.where("created_at <= ?", since)
        if report_types.present?
          expired_reports = expired_reports.where(:report_type => report_types)
        end
        rec_count = expired_reports.delete_all
      end
      logger.info "INFO #{method_name} => TIMING: #{exec_time} -> #{name}; deleted #{rec_count} reports of type #{report_types}"
    end
  end

  # -------------------------------------------------------------------------------------------------
  private
  # -------------------------------------------------------------------------------------------------

  # Measure excution time of block
  def self.time
    start = Time.now
    yield
    Time.now - start
  end

  #Return constant for string
  def self.get_report_type(type)
    method_name = __method__
    logger.debug "DEBUG #{method_name} =>  type = #{type}"
    t = type.downcase
    result = nil
    case t
      when "integrity"
        result = AdminReport::Type::INTEGRITY
      when "fraud"
        result = AdminReport::Type::FRAUD
      when "weekly"
        result = AdminReport::Type::WEEKLY
      when "monthly"
        result = AdminReport::Type::MONTHLY
      when "adhoc_gba"
        result = AdminReport::Type::ADHOC_GBA
      when "adhoc_tx"
        result = AdminReport::Type::ADHOC_TX
      when "adhoc_log"
        result = AdminReport::Type::ADHOC_LOG
      when "sec"
        result = AdminReport::Type::SEC
      when "std"
        result = AdminReport::Type::STD
      else
        logger.warn "WARN #{method_name} => invalid parameter"
    end
    return result
  end
end
