
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

class LogsController < ApplicationController
  include FilterConcern

  skip_before_action :authenticate!, only: :create
  skip_before_action :verify_authenticity_token, only: :create
  skip_load_resource only: [:transactions]
  load_and_authorize_resource except: [:create, :export]
  authorize_resource only: [:export]

  def index
    @search = Log.from("logs").  # PATCH: In some cases mysql is not using the index, so we force it to improve performance.
                  ransack(params.has_key?(:q) ? params[:q].permit(:s) : {})
    @search.sorts = 'created_at desc'
    @search.created_at_gt = 30.minutes.ago

    # Can't `includes(:subject)`
    # FrontDesk is an ActiveResource class and doesn't support eager loading
    @logs = @search.result.page(params[:page]).per(per_page).includes(:manager).includes(:account).includes(:webservice)
    instrument_logger('uc15.transacties_inzien_gelukt', manager_id: session[:current_user_id])
  end

  def search
    unless params[:q].present?
      redirect_to logs_url
      return
    end
    @search = Log.from("logs") # PATCH: In some cases mysql is not using the index, so we force it to improve performance.
    @search = @search.ransack(merged_search_params)
    @search.sorts = 'id desc'

    unless filter_on_time_range
      redirect_to logs_url
      return
    end

    # Can't `includes(:subject)`
    # FrontDesk is an ActiveResource class and doesn't support eager loading
    @logs = @search.result.page(params[:page]).per(per_page).includes(:manager).includes(:account).includes(:webservice)
    instrument_logger('uc15.transacties_zoeken_gelukt', manager_id: session[:current_user_id])
    render action: :index
  end

  def transactions
    @transactions = Log.transactions(params[:id]).page(params[:transactions_page])

    if request.xhr?
      render json: {  dialog_content: render_to_string(partial: "transactions"), title: "Transacties" }
    else
      render :transactions
    end
  end

  # interface to enable the balie app to post logging
  def create
    instrument_logger(params['msg'],
                      front_desk_id:  params['front_desk_id'],
                      pseudonym:      params['pseudonym'],
                      account_id:     params['account_id'],
                      session_id:     params['session_id'],
                      ip_address:     params['ip_address'])
    head(200)
  end

  def export
    account = Account.find(params[:id])
    @log_search = Log.ransack(params[:log_search])
    @logs = @log_search.result.account_actions(account.bsn).includes(:webservice)

    params[:q] = params[:log_search]

    instrument_logger('1126', query: params[:log_search][:name_or_ip_address_or_pseudoniem_cont], from: parse_time_filter('created_at_gt'), to: parse_time_filter('created_at_lt') || Time.now, manager_id: session[:current_user_id])
    send_data(@logs.to_csv, filename: "Export transactie log (#{account.bsn}).csv", disposition: 'attachment', type: 'text/csv; charset=utf8; header=present')
  end

  private

  def filter_on_time_range
    from = parse_time_filter('created_at_gt')
    to = parse_time_filter('created_at_lt')

    if from && to.nil? # alleen Van ingevuld, OK
      @search.created_at_gt = from
    elsif from && to && from < to # beide geldig, OK
      @search.created_at_gt = from
      @search.created_at_lt = to
    else
      false # invalid time range, use default
    end
  end

  def merged_search_params
    # Delete time filter, we set this manually later on
    search_params = params[:q].except(
      'created_at_gt(1i)',
      'created_at_gt(2i)',
      'created_at_gt(3i)',
      'created_at_gt(4i)',
      'created_at_gt(5i)',
      'created_at_lt(1i)',
      'created_at_lt(2i)',
      'created_at_lt(3i)',
      'created_at_lt(4i)',
      'created_at_lt(5i)'
    )

    # Een leeg filtercriterium of `_` geeft alle resultaten (voor de ingestelde of default van/tot periode)
    query_logs = if params[:query_logs].blank? || params[:query_logs] == '_'
                   nil
                 else
                   params[:query_logs]
                 end
    case params['filter_scope'].to_s
    when 'name'
      search_params.merge(g: [{ name_cont: query_logs.to_s }])
    when 'ip_address'
      if query_logs.to_s.end_with?('%')
        search_params.merge(g: [{ ip_address_start: filter_out_wildcards(query_logs.to_s) }])
      else
        search_params.merge(g: [{ ip_address_eq: filter_out_wildcards(query_logs.to_s) }])
      end
    when 'sector_number'
      search_params.merge(g: [{ sector_number_eq: query_logs.to_i }])
    else
      return nil
    end
  end

  def filter_out_wildcards(input)
    input.tr('%', '')
  end
end
