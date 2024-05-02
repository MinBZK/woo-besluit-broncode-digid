
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

class ManagersController < ApplicationController
  include FourEyesReviewControllerConcern
  load_and_authorize_resource only: [:index, :show, :new, :edit, :update, :create, :destroy]
  respond_to :html

  before_action :check_four_eyes!, only: [:edit, :edit_own, :update, :update_own, :destroy]

  # GET /managers
  def index
    @managers_in_review = FourEyesReview.where(record_table: "Manager")
    @search   = Manager.distinct.ransack(params[:q])
    @managers = @search.result.page(params[:page])
    instrument_logger('uc9.beheer_overzicht_bekijken_gelukt', manager_id: session[:current_user_id])
  end

  # GET /managers/1
  def show
    @logs    = Log.where(account_id: @manager.id).order(created_at: :desc).page(params[:page]).per(params[:per_page])
    manager_log('uc9.beheer_inzien_gelukt')
    respond_with @manager
  end

  def show_own
    @manager = current_user
    manager_log('uc26.eigen_beheeraccount_inzien_gelukt')
  end

  def edit_own
    manager_log('uc26.eigen_beheeraccount_inzien_wijzigen_gelukt')
  end

  def update_own
    if @manager.update(params[:manager])
      manager_log('uc26.eigen_beheeraccount_wijzigen_gelukt')
      redirect_to show_own_manager_path(@manager), notice: "Eigen beheeraccount succesvol gewijzigd."
    else
      render :edit_own
    end
  end

  # GET /managers/new
  def new

  end

  # GET /managers/1/edit
  def edit
    log(:edit)
  end

  def create
    to_superuser! @manager
    if @manager.save_for_review(manager: current_user)
      manager_log('uc25.beheeraccount_aanmaken_gelukt', name: @manager.account_name)
      redirect_to review_managers_path(@manager.review)
    else
      render :new
    end
  end


  def update
    to_superuser! @manager
      if @manager.update_for_review(params[:manager], manager: current_user)
        log(:update)
        redirect_to review_managers_path(@manager.review)
      else
        render :edit
      end
  end

  private

  def report_log
    create_report_log(type: :disable_account, modified_manager: @four_eyes_review.original) if @manager.active_changed? && !@manager.active?
    create_report_log(type: :create_account, modified_manager: @four_eyes_review.original) if @four_eyes_review.action == "create"

    @manager.added_roles.each {|role| create_report_log(type: :add_role, modified_manager:@four_eyes_review.original, role: role) } if @manager.added_roles
    @manager.removed_roles.each {|role| create_report_log(type: :remove_role, modified_manager: @four_eyes_review.original, role: role) } if @manager.removed_roles
  end

  def manager_log(key, options = {})
    payload = {manager_id: session[:current_user_id], subject_type: 'Manager', subject_id: @manager.id}.merge(options)
    instrument_logger(key, payload)
  end

  def log(action)
    case action
      when :review
        manager_log('uc25.wijziging_beheeraccount_inzien_gelukt', name: @manager.account_name)
      when :accept
        manager_log('uc25.wijziging_beheeraccount_geaccordeerd', name: @manager.account_name)
      when :reject
        manager_log('uc25.wijziging_beheeraccount_afgekeurd', name: @manager.account_name)
      when :withdraw
        manager_log('uc25.wijziging_beheeraccount_ingetrokken', name: @manager.account_name)
      when :edit, :edit_review
        manager_log('uc9.beheer_inzien_wijzigen_gelukt')
      when :update, :update_review
        if @manager.id.to_s == session[:current_user_id]
          manager_log('uc26.eigen_beheeraccount_wijzigen_gelukt')
        else
          manager_log('uc25.beheeraccount_wijzigen_gelukt')
        end

        if params[:manager][:certificate].present?
          manager_log('uc25.beheeraccount_authenticatiemiddel_toevoegen_gelukt')
        end

        if params[:manager][:remove_certificate] == "1"
          manager_log('uc25.beheeraccount_authenticatiemiddel_verwijderen_gelukt')
        end

        if @manager.review.original.active === true && @manager.review.updated.active === false
          manager_log('uc25.beheeraccount_opschorten_gelukt')
        elsif @manager.review.original.active === false && @manager.review.updated.active === true
          manager_log('uc25.beheeraccount_opschorten_ongedaan_maken_gelukt')
        end
      end
  end

  def check_four_eyes!
    if /_own$/ === request.path_parameters[:action]
      @manager = current_user
      dest = {action: :show_own}
    else
      @manager = Manager.find(params[:id])
      dest = {action: :index}
      redirect_to(dest, alert: I18n.t("four_eyes.manager_frozen")) if @manager.frozen_for_review? || @manager.roles_updated_since?(params[:updating_started_at])
    end
    redirect_to(dest, alert: I18n.t("four_eyes.manager_under_review")) if @manager.under_review?
  end

  # only superusers can provide superuser rights to other users
  # @param [Manager] manager the format type, `:text` or `:html`
  def to_superuser!(manager)
    return unless params.present? && params[:manager].present?

    want_to_be_superuser = (params[:manager].fetch(:superuser, false) == '1')
    params[:manager].delete(:superuser) # delete superuser from params in any case

    return unless current_user.superuser?

    manager.superuser = if want_to_be_superuser
                          true
                        else
                          false
                        end
    manager.save # no validation expected
  end
end
