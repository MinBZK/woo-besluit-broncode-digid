
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

class FrontDesksController < ApplicationController
  load_and_authorize_resource param_method: :front_desk_params

  # General lost_connection error
  class LostConnection < StandardError
  end

  rescue_from LostConnection, with: :lost_connection
  rescue_from ActiveResource::ResourceNotFound, with: :resource_not_found

  def index
    @front_desks = if params[:search]
                     FrontDesk.ransack params[:search]
                   else
                     FrontDesk.all
                   end
    instrument_logger('uc31.front_desk_overview_shown', front_desk_id: '')
  end

  def new
    @front_desk.max_issues               = 100
    @front_desk.alarm_unchecked_accounts = ::Configuration.get_string('balie_default_alarmering_niet_gecheckte_accounts')
    @front_desk.alarm_fraud_suspension   = ::Configuration.get_string('balie_default_alarmering_fraude_vermoedens')
  end

  def create
    @front_desk.save!
    instrument_logger('uc31.front_desk_create_success', front_desk_id: @front_desk.id)
    redirect_to(@front_desk, notice: I18n.t('front_desks.new_succesful'))
  rescue ActiveResource::ResourceInvalid, ActiveResource::ResourceConflict
    instrument_logger('uc31.front_desk_create_fail')
    render :new
  rescue
    instrument_logger('uc31.front_desk_create_fail')
    raise LostConnection
  end

  def show
    @verifications_unaudited       = Verification.find(:all, from: :unaudited,        params: { front_desk_id: @front_desk.id })
    @verifications_fraud_suspicion = Verification.find(:all, from: :fraud_suspicion,  params: { front_desk_id: @front_desk.id })

    instrument_logger('uc31.front_desk_show', front_desk_id: @front_desk.id)
  rescue
    raise LostConnection
  end

  def pseudonym_show
    @pseudonym = params[:pseudonym]

    render json: { dialog_content: @pseudonym ? render_to_string(partial: "pseudonym_show") : nil, title: t('front_desks.pseudonym_show')}
  end

  def edit
    @front_desk.valid? unless flash[:error].blank?
    instrument_logger('uc31.front_desk_show_edit_successful', front_desk_id: @front_desk.id)
  rescue
    raise LostConnection
  end

  def update
    old_front_desk_values = @front_desk.attributes.dup
    if @front_desk.update_attributes(resource_params)
      instrument_logger('uc31.front_desk_update_success', front_desk_id: @front_desk.id)

      changes = @front_desk.attributes.merge(old_front_desk_values) { |_k, old_v, new_v| old_v.to_s != new_v.to_s }.select { |_k, v| v }.keys

      changes.each do |field|
        case field
        when 'alarm_fraud_suspension'
          instrument_logger('uc31.front_desk_warning_fraud_suspicions_changed', front_desk_id: @front_desk.id)
        when 'alarm_unchecked_accounts'
          instrument_logger('uc31.front_desk_warning_unaudited_changed', front_desk_id: @front_desk.id)
        when 'max_issues'
          instrument_logger('uc31.front_desk_maximum_per_day_adjusted', front_desk_id: @front_desk.id)
        end
      end

      redirect_to(@front_desk, notice: I18n.t('front_desks.update_succesful'))
    else
      instrument_logger('uc31.front_desk_update_fail', front_desk_id: @front_desk.id)
      flash[:error] = I18n.t('front_desks.update_failed')
      render :edit
    end
  rescue
    instrument_logger('uc31.front_desk_update_fail', front_desk_id: @front_desk.id)
    raise LostConnection
  end

  def destroy
    if @front_desk.destroy
      instrument_logger('uc31.front_desk_destroy_success', front_desk_id: params[:id])
      redirect_to(front_desks_path, notice: I18n.t('front_desks.delete_succesful'))
    else
      instrument_logger('uc31.front_desk_destroy_fail', front_desk_id: @front_desk.id)
      redirect_to(@front_desk, notice: I18n.t('front_desks.delete_not_found'))
    end
  rescue
    instrument_logger('uc31.front_desk_destroy_fail', front_desk_id: @front_desk.id)
    raise LostConnection
  end

  def block
    unless @front_desk.valid?
      flash[:error] = I18n.t('front_desks.front_desk_blocked_fail_model_not_valid')
      return redirect_to edit_front_desk_path
    end
    if !@front_desk.blocked? && @front_desk.update_attributes(blocked: true)
      instrument_logger('uc31.front_desk_block_success', front_desk_id: @front_desk.id)
      flash[:notice] = I18n.t('front_desks.front_desk_blocked')
    else
      instrument_logger('uc31.front_desk_block_fail', front_desk_id: @front_desk.id)
      flash[:error] = I18n.t('front_desks.front_desk_blocked_fail')
    end
    redirect_to @front_desk
  rescue
    instrument_logger('uc31.front_desk_block_fail', front_desk_id: @front_desk.id)
    raise LostConnection
  end

  def unblock
    unless @front_desk.valid?
      flash[:error] = I18n.t('front_desks.front_desk_unblocked_fail_model_not_valid')
      return redirect_to edit_front_desk_path
    end
    if @front_desk.blocked? && @front_desk.update_attributes(blocked: false)
      instrument_logger('uc31.front_desk_unblock_success', front_desk_id: @front_desk.id)
      flash[:notice] = I18n.t('front_desks.front_desk_unblocked')
    else
      instrument_logger('uc31.front_desk_unblock_fail', front_desk_id: @front_desk.id)
      flash[:error] = I18n.t('front_desks.front_desk_unblocked_fail')
    end
    redirect_to @front_desk
  rescue
    instrument_logger('uc31.front_desk_unblock_fail', front_desk_id: @front_desk.id)
    raise LostConnection
  end

  private

  def resource_params
    params.require(:front_desk).to_unsafe_h
  rescue ActionController::ParameterMissing
    {}
  end

  def lost_connection
    flash[:alert] = I18n.t('front_desks.lost_connection')
    render :index
  end

  def resource_not_found
    @front_desk = nil
    render :show
  end
end
