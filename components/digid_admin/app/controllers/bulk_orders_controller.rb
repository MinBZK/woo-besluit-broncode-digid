
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

class BulkOrdersController < ApplicationController
  load_and_authorize_resource
  skip_load_resource only: [:transactions]
  # rescue_from ActiveRecord::RecordNotFound, with: :resource_not_found

  def index
    @bulk_orders = BulkOrder.order(created_at: :desc).ransack.result.page(params[:page])
    log_action_for(:index)
  end

  def show
    log_action_for(:show)
  end

  def new
  end

  def create
    @bulk_order.manager = current_user

    if @bulk_order.save
      log_action_for(:create) if @bulk_order.created_status?
      log_action_for(:create_invalid) if @bulk_order.invalid_status?
      redirect_to @bulk_order, notice: 'Opdracht succesvol aangemaakt!'
    else
      if @bulk_order.csv_contains_too_much_bsns?
        instrument_logger('uc46.bulk_order_csv_amount', subject_type: 'BulkOrder')
      else
        instrument_logger('uc46.bulk_order_add_command_failed', subject_type: 'BulkOrder')
      end
      render :new
    end
  end

  def destroy
    log_action_for(:destroy) if @bulk_order.destroy
    redirect_to bulk_orders_url, notice: 'Opdracht succesvol verwijderd!'
  end

  # PATCH
  def approve
    @bulk_order.approval_manager = current_user
    log_action_for(:approve) if @bulk_order.approved_status!

    BulkOrderJob.perform_async(@bulk_order.id)

    redirect_to bulk_orders_url, notice: 'Opdracht succesvol geaccordeerd!'
  end

  # PATCH
  def reject
    @bulk_order.rejection_manager = current_user
    log_action_for(:reject) if @bulk_order.rejected_status!
    redirect_to bulk_orders_url, notice: 'Opdracht succesvol afgekeurd!'
  end

  # GET /bulk_orders/transactions/:id
  def transactions
    @transactions = Log.transactions(params[:id]).page(params[:transactions_page])

    if request.xhr?
      render json: { dialog_content: render_to_string(partial: "transactions"), title: "Transacties" }
    else
      render :transactions
    end
  end

  # GET /bulk_orders/download_overview
  def download_overview
    overview_csv_creator = Bulk::OverviewCsvCreator
    if overview_csv_creator.available?
      log_action_for(:download_overview)
      send_data(overview_csv_creator.csv, filename: overview_csv_creator.filename, type: 'text/csv')
    else
      flash[:alert] = I18n.t('bulk_order.overview_unavailable')
      redirect_to bulk_orders_url
    end
  end

  # GET /bulk_orders/:id/download/invalid_bsn
  def download_invalid_bsn
    log_action_for(:download_invalid_bsn)
    invalid_bsn_csv_creator = Bulk::InvalidBsnCsvCreator.new(@bulk_order)
    send_data(invalid_bsn_csv_creator.csv, filename: invalid_bsn_csv_creator.filename, type: 'text/csv')
  end

  # GET /bulk_orders/:id/download/account_status
  def download_account_status
    if @bulk_order.invalid_status?
      flash[:alert] = I18n.t('bulk_order.status_csv_invalid')
      redirect_to @bulk_order
    else
      status_csv_creator = Bulk::StatusCsvCreator.new(@bulk_order)
      if status_csv_creator.available?
        log_action_for(:download_account_status)
        send_data(status_csv_creator.csv, filename: status_csv_creator.filename, type: 'text/csv')
      else
        flash[:alert] = I18n.t('bulk_order.status_csv_unavailable')
        redirect_to @bulk_order
      end
    end
  end

  # GET /bulk_orders/:id/download/address_list
  def download_address_list
    address_list_csv_creator = Bulk::AddressListCsvCreator.new(@bulk_order)
    send_data(address_list_csv_creator.csv, filename: address_list_csv_creator.filename, type: 'text/csv')
    log_action_for(:download_address_list)
  end

  private

  def create_params
    params.require(:bulk_order).permit(:name, :account_status_scope, :bulk_type, :csv_upload)
  end

  def log_action_for(action)
    i18n_key =
      case action
      when :index
        @bulk_order = BulkOrder.new
        'uc46.bulk_order_overview_shown'
      when :download_overview
        @bulk_order = BulkOrder.new
        'uc46.bulk_order_download_overview'
      when :show                    then 'uc46.bulk_order_show'
      when :create                  then 'uc46.bulk_order_created'
      when :create_invalid          then 'uc46.bulk_order_created_invalid'
      when :approve                 then 'uc46.bulk_order_approved'
      when :reject                  then 'uc46.bulk_order_rejected'
      when :destroy                 then 'uc46.bulk_order_destroyed'
      when :download_invalid_bsn    then 'uc46.bulk_order_download_invalid_bsn'
      when :download_account_status then 'uc46.bulk_order_download_account_status'
      when :download_address_list   then 'uc46.bulk_order_download_address_list'
      else return
      end

    instrument_logger(
      i18n_key,
      id: @bulk_order.id,
      name: @bulk_order.name,
      type: @bulk_order.bulk_type,
      bulk_order_id: @bulk_order.id
    )
  end

  def resource_not_found
    @bulk_order = nil
    render :show
  end
end
