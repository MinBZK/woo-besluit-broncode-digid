
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

module LogHelper
  def show_manager_actions_for(subject_type, subject_id = nil)
    @manager_log_search = ManagerLog.ransack(params[:manager_log_search])
    @manager_log_search.sorts = 'id desc'

    timeframe = if subject_type == 'Webservice' || subject_type == 'NewsItem'
                  24.hours.ago.beginning_of_day
                else
                  6.months.ago.beginning_of_day
                end
    @manager_log_search.created_at_gt ||= timeframe

    @manager_logs = if subject_id.blank?
                      @manager_log_search.result.manager_actions_for_subject(subject_type).page(params[:manager_logs_page]).includes(:manager)
                    else
                      @manager_log_search.result.manager_actions_with_subject_id(subject_type, subject_id).page(params[:manager_logs_page]).includes(:manager)
                    end

    @subject_type = subject_type.constantize
    render 'shared/manager_logs'
  end

  # used on the gba details screen
  def show_specific_manager_actions_for(identifier, subject_type, subject_id)
    code = if identifier.is_a?(String)
             SYSTEM_MONITOR_MAPPINGS[identifier]
           else
             identifier
           end
    if subject_type == 'Account'
      account = Account.find(subject_id)
      @logs = Log.where(code: code, sector_number: account.bsn).order(created_at: :desc)
    else
      @logs = Log.actions_with_subject_id_and_code(code, subject_type, subject_id).order(created_at: :desc)
    end

    @logs = @logs.page(params[:page]).per(params[:per])
    @subject = t('brp_history')
    render 'shared/manager_specific_logs'
  end

  def show_webservice_actions(id)
    @log_search = Log.ransack(params[:log_search])
    set_webservice_query_time
    @log_search.sorts = 'created_at desc'
    @logs = @log_search.result.webservice_actions(id).page(params[:webservice_page]).includes(:account)
    render 'shared/webservice_logs'
  end

  def show_service_actions(id)
    @log_search = Log.ransack(@search_criteria)
    set_webservice_query_time
    @log_search.sorts = 'created_at desc'
    @logs = @log_search.result.webservice_actions(id).page(params[:webservice_page]).includes(:account)
    render 'shared/service_logs'
  end

  def show_connection_actions(ids)
    @log_search = Log.ransack(@search_criteria)
    set_webservice_query_time
    @log_search.sorts = 'created_at desc'
    @logs = @log_search.result.webservice_actions(ids).page(params[:webservice_page]).includes(:account)
    render 'shared/connection_logs'
  end

  def show_account_actions(account)
    @log_search = Log.ransack(params[:log_search])
    @log_search.created_at_gt ||= 6.months.ago.beginning_of_day
    @log_search.sorts = 'created_at desc'
    @logs = @log_search.result.account_actions(account.bsn).page(params[:account_page]).includes(:webservice)
    render 'shared/account_logs'
  end

  def show_manager_actions(id)
    @log_search = Log.ransack(params[:log_search])
    @log_search.created_at_gt ||= 6.months.ago.beginning_of_day
    @log_search.sorts = 'created_at desc'
    @logs = @log_search.result.manager_actions(id).page(params[:manager_page])
    render 'shared/manager_actions'
  end

  def show_bulk_order_actions
    @log_search = Log.ransack(params[:log_search])
    @log_search.created_at_gt ||= 6.months.ago.beginning_of_day
    @log_search.sorts = 'id desc'
    @logs = @log_search.result.bulk_order_actions.page(params[:bulk_order_page]).without_count
    render 'shared/bulk_order_logs'
  end

  def show_front_desk_action_for(front_desk)
    @front_desk = front_desk
    time_zone_filter = (params[:front_desk_search] && params[:front_desk_search][:time_zone])
    time_zone = (time_zone_filter == 'local') ? @front_desk.time_zone : Time.zone

    Time.use_zone(time_zone) do
      @front_desk_search = Log.ransack(params[:front_desk_search])
      @front_desk_search.created_at_gt ||= 6.months.ago.beginning_of_day
      @front_desk_search.sorts = 'created_at desc'
      @logs = @front_desk_search.result.front_desk_actions(front_desk.id).page(params[:front_desk_page])
    end
    render 'shared/front_desks_logs'
  end

  def compare_time time
    log_time = ::Configuration.get_int("time_period_on_web_service_transaction_logs")
    log_time == 0 && I18n.l(time, format: :short) == I18n.l(log_time.minutes.ago, format: :short)
  end

  private

  def set_webservice_query_time
    log_time = ::Configuration.get_int("time_period_on_web_service_transaction_logs")
    @log_search.created_at_gt ||= log_time.minutes.ago
  end
end
