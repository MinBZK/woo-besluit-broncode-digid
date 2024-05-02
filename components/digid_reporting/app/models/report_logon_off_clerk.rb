
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
# Inloggen per uitgiftepunt per baliemedewerker
#
# Reports the login and logoff times of a frontdesk employee.
# If the employee forget to log off, it tries to determine
# the last action during that session and add the default
# session timeout to that value. It also searches for a
# following login.
# These timestamp will compared and the earliest one will
# be reported. There is no distinguishing between a regular
# logoff or a timeout, as the customer didn't wish to see a
# timeout message.
# As long as the logon event has no front_desk id it's impossible to
# determin a front desk. Reports where a single logon will time out
# will be reported without a front desk.
#
class ReportLogonOffClerk < AdminReport

  def initialize(period = :week)
    @report_class_name = 'ReportLogonOffClerk'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = [@rep_param.period_header, "Uitgiftepuntnaam", "Code uitgiftepunt", "baliemedewerker pseudoniem",
      "Tijdstip succesvolle inlog (tijdzone balie)", "Tijdstip uitlog/verlopen sessie (tijdzone balie)",
      "Tijdstip succesvolle inlog (tijdzone NL)", "Tijdstip uitlog/verlopen sessie (tijdzone NL)"]
    @subject_type ='FrontDesk'
    uc_login = ['uc30.front_desk_employee_assigned_to_front_desk']
    uc_logout = ['uc30.front_desk_employee_log_out']
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    @period = period
    @SESSION_TIME_OUT = APP_CONFIG['session_time_out']
    # We only use 1 code.
    @login_code =  lookup_codes(nil, uc_login).first
    @logout_code =  lookup_codes(nil, uc_logout).first
  end

  def report(start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)

    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    #
    # Count and fetch the records for the given period.
    result = [@header]

    # Proces login records, it will return the result arrays
    # Add report lines to result
    proces_result(get_logins).each do |line|
      result << line
    end

    logger.info "INFO -> #{@rep_param.report_class_name} ===> Result: #{result.inspect}"
    return result
  end

   private

  # Take an Login object and search for the logoff logging.
  # Extract disp id , pseudoniem and logon timestamp from Login
  # object and take the logoff timestamp from query result.
  # If query is empty return the string 'timed out'
  def proces_result(logins)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> processing login object: #{logins.inspect}"
    results = []
    if logins.present?
      logins.each do |login|
        logger.debug "DEBUG #{me} -> proces login: #{login.inspect}"
        line = []
        # Extract timestamp and pseudoniem from current login object
        pseudoniem = login.pseudoniem
        login_ts = login.created_at
        front_desk_id = login.subject_id
        session_id = login.session_id
        transaction_id = login.transaction_id
        if front_desk_id.nil?
          logger.debug "DEBUG #{me} -> QQQ proces login: #{login.inspect}"
          front_desk_id = get_front_desk(session_id)
        end

        front_desk = front_desk_id.present? ? FrontDesk.find(front_desk_id) : nil # QQQ switch to find method? FrontDesk.find(front_desk_id)

        session_end_ts = determine_session_end(login_ts, front_desk_id, pseudoniem, session_id, transaction_id)
        if !session_end_ts.present?
          logger.warn "WARN #{me} -> login: #{login.inspect} timed out."
        end

        line = build_line(front_desk, login_ts, pseudoniem, session_end_ts)

        results << line
      end
    end
    logger.debug "DEBUG #{me} -> return: #{results.inspect}"
    return results
  end

  #
  # Count and fetch the records for the given period.
  # Returns ordered hash with login values
  def get_logins
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> execute main query."
    logins = Log.from( @idx_hint).where(:created_at => (@rep_param.start_ts..@rep_param.end_ts),
        :code => @login_code, :subject_type => @subject_type).order(:pseudoniem,
        :created_at).select("id, pseudoniem, subject_type, subject_id, created_at, transaction_id, session_id")
    logger.debug "DEBUG #{me} -> found logins : #{logins.inspect}"
    return logins if logins.present?
  end

  #
  # Return record with logout action of the given pseudoniem
  def get_logout(login_ts, pseudoniem, session_id)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for login_ts: #{login_ts},
      pseudoniem: #{pseudoniem}, session_id: #{session_id}"
    logout = Log.where("pseudoniem =? and
      created_at > ? and
      code = ? and
      session_id = ?", pseudoniem,login_ts, @logout_code, session_id).order(:pseudoniem,
      :created_at).select("pseudoniem, subject_id, created_at, session_id").first
    logger.debug "DEBUG #{me} -> logout is #{logout.inspect}"
    return logout if logout.present?
  end

  #
  # Return record with the next login for the given pseudoniem
  # regardless of transaction or dispensary
  def get_next_login_pseudoniem(login_ts,  pseudoniem)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for login_ts: #{login_ts},
      pseudoniem: #{pseudoniem}"

    next_login = Log.where("pseudoniem = ? and
      created_at > ? and
      code = ?", pseudoniem, login_ts, @login_code).order(:pseudoniem,
      :created_at).select("pseudoniem, subject_id, created_at, transaction_id").first
    logger.debug "DEBUG #{me} -> next_login is #{next_login.inspect}"
    return next_login if next_login.present?
  end

  #
  # Return record with the next login for the given transaction
  # regardless of pseudoniem or dispensary
  def get_next_login_transaction(login_ts, transaction_id)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for login_ts: #{login_ts},
      transaction_id: #{transaction_id}"

    next_login = Log.where("created_at > ? and
      code = ? and
      transaction_id = ?", login_ts, @login_code, transaction_id).order(:pseudoniem,
      :created_at).select("pseudoniem, subject_id, created_at, transaction_id").first
    logger.debug "DEBUG #{me} -> next_login is #{next_login.inspect}"
    return next_login if next_login.present?
  end

  #
  # Return record of last transaction of the given pseudoniem
  # before next login
  def get_last_action_pseudoniem(login_ts, next_login_ts, front_desk_id, pseudoniem)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for pseudoniem: #{pseudoniem},
      front_desk_id: #{front_desk_id}, login_ts: #{login_ts}, next_login_ts: #{next_login_ts}"

    result = nil
    # Get all transactions between login and next_login
    actions = Log.where(:created_at => (login_ts..next_login_ts),
      :pseudoniem => pseudoniem,
      :subject_id => front_desk_id)
    last_action = actions[actions.count - 2] # -1 is the next login
    logger.debug "DEBUG #{me} -> last_action is #{last_action.inspect}"
    result = last_action if last_action.present?
  end

  #
  # Return record of last transaction of the given pseudoniem
  # before next login
  def get_last_action_transaction(login_ts, next_login_ts, front_desk_id, transaction_id)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for transaction_id: #{transaction_id},
      front_desk_id: #{front_desk_id}, login_ts: #{login_ts}, next_login_ts: #{next_login_ts}"

    result = nil
    # Get all transactions between login and next_login
    actions = Log.where(:created_at => (login_ts..next_login_ts),
      :transaction_id => transaction_id,
      :subject_id => front_desk_id)
    last_action = actions[actions.count - 2]
    logger.debug "DEBUG #{me} -> last_action is #{last_action.inspect}"
    result = last_action if last_action.present?
  end

  #
  # Return last record of session
  def get_last_action_session(login_ts, session_id, pseudoniem)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for session_id: #{session_id},
      login_ts: #{login_ts}, pseudoniem: #{pseudoniem}"
    result = nil
    # Get all transactions between login and next_login
    actions = Log.where("created_at >= '#{login_ts}'
      and session_id = '#{session_id}' and pseudoniem = '#{pseudoniem}'")
    last_action = actions[actions.count - 1]
    logger.debug "DEBUG #{me} -> last_action is #{last_action.inspect}"
    result = last_action if last_action.present?
  end

    #
  # Return last record of session
  def get_front_desk(session_id)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for session_id: #{session_id}"
    result = nil
    # Get all transactions between login and next_login
    actions = Log.where(:session_id => session_id, :subject_type => @subject_type)
    actions.each do |action|
      if action.subject_id.present?
        result = action.subject_id
        break
      end
    end
    logger.debug "DEBUG #{me} -> front desk id is #{result}"
    return result
  end
  #
  # Return the logout / timeout timestamp
  # As the specs don't ask for a cause we
  # move from a precise logout timestamp to
  # a more impliciet timeout.
  def determine_session_end(login_ts, front_desk_id, pseudoniem, session_id, transaction_id)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for login_ts: #{login_ts},
       pseudoniem: #{pseudoniem}, session_id: #{session_id}, transaction_id: #{transaction_id}"

    result = nil
    # Check on logout
    logout = get_logout(login_ts, pseudoniem, session_id)
    if logout.present?
      result = logout.created_at
    else
      last_action = get_last_action_session(login_ts, session_id, pseudoniem)
      result = last_action.created_at + @SESSION_TIME_OUT
    end
    if result.nil?
      # Check on login with same pseudoniem, this should never happen from v.5.1 onwarts
      logger.warn "WARN #{me} -> Can't retrieve logout / timeout from session."
      next_login = get_next_login_pseudoniem(login_ts, pseudoniem)
      if next_login.present?
        next_login_ts = next_login.created_at
        timeout = get_last_action_pseudoniem(login_ts, next_login_ts, front_desk_id, pseudoniem)
        # If timestamp is older than 15min from next login
        if next_login_ts > timeout.created_at + @SESSION_TIME_OUT
          # add 15min to timestamp and return
          result = timeout.created_at + @SESSION_TIME_OUT if timeout.present?
        else
          result = next_login_ts
        end
      else
        if session_id.present?
          # Get last action from session
          last_action = get_last_action_session(login_ts, session_id, pseudoniem)
          result  = last_action.created_at + @SESSION_TIME_OUT if last_action.present?
        else
          result = login_ts + @SESSION_TIME_OUT if login_ts.present?
        end
      end
    end
    # Check on last transaction with same pseudoniem
    return result
  end

  #
  # Build the line.
  def build_line(front_desk, login_ts, pseudoniem, session_end_ts)
    me = "#{@rep_param.report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me} -> for front_desk: #{front_desk.inspect},
      login_ts: #{login_ts}, pseudoniem: #{pseudoniem}, session_end_ts: #{session_end_ts.inspect}"

    line = [@rep_param.period_value]
    fd_tz = ""
    if front_desk.present?
      begin
        line += [front_desk.name, front_desk.code]
        fd_tz = front_desk.time_zone
      rescue Exception => e
        logger.error "ERROR #{me} -> Building report line caused #{e.message} => #{e.backtrace}"
      end
    else
      logger.warn "WARN #{me} -> No FrontDesk found: #{front_desk.inspect}"
    end

    #Add info for missing front desk (or exception)
    if line.size <= 1
      line += ["n.a.", "n.a."]
    end

    line << pseudoniem

    #timestamps in front desk timezone
    line << format_ts_to_tz(login_ts, fd_tz)
    line << (session_end_ts.present? ? format_ts_to_tz(session_end_ts, fd_tz) : 'timeout')

    #timestamps in NL timezone
    line << format_ts_to_tz(login_ts)
    line << (session_end_ts.present? ? format_ts_to_tz(session_end_ts) : 'timeout')

    logger.debug "DEBUG #{me} -> return: #{line.inspect}"
    return line
  end
end

def format_ts_to_tz(ts, tz = nil)
  #default tz for unspecified tz
  tz = 'Europe/Amsterdam' if !tz.present?
  #default for empty tz
  tz = 'UTC' if tz.empty?

  return ts.in_time_zone(tz).strftime('%Y-%m-%d %H:%M:%S (UTC%:z)')
end
