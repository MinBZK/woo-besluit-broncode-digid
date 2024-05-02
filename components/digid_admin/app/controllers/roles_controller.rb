
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

class RolesController < ApplicationController
  include FourEyesReviewControllerConcern
  load_and_authorize_resource only: [:index, :show, :edit, :new, :create, :update, :destroy]
  respond_to :html

  before_action :check_four_eyes!, only: [:edit, :update, :destroy]

  # GET /roles
  def index
    @roles_in_review = FourEyesReview.where(record_table: "Role")
    respond_with @roles
    instrument_logger('uc27.beheerrollen_overzicht_inzien_gelukt', manager_id: session[:current_user_id])
  end

  # GET /roles/1
  def show
    @logs = Log.where(role_id: @role.id).page(params[:page]).per(params[:per])
    respond_with @role
    instrument_logger('uc27.beheerrol_inzien_gelukt', role_id: @role.id, manager_id: session[:current_user_id])
  end

  # GET /roles/new
  def new
    respond_with @role
  end

  # GET /roles/1/edit
  def edit
    log(:edit)
  end

  # POST /webservices
  def create
    if @role.save_for_review(manager: current_user)
      instrument_logger('uc27.beheerrol_aanmaken_gelukt', role_id: @role.id, name: @role.name)
      redirect_to review_roles_path(@role.review)
    else
      render :new
    end
  end

  def update
    if @role.update_for_review(params[:role], manager: current_user)
      log(:update)
      redirect_to review_roles_path(@role.review)
    else
      render :edit
    end
  end

 # DELETE /webservices/1
  def destroy
    if params[:role] && params[:role][:note].present?
      @role.note = params[:role][:note]
    else
      @role.errors.add(:note, :blank)
      render_four_eyes_dialog
      return
    end
    @role.note = params[:role][:note] if params[:role]
    if @role.destroy_for_review(manager: current_user)
      instrument_logger('uc27.beheerrol_verwijderen_gelukt', role_id: @role.id)
      redirect_via_js(review_roles_path(@role.review))
    else
      instrument_logger('uc27.beheerrol_verwijderen_mislukt', role_id: @role.id, errors: @role.errors)
      redirect_via_js(url_for(@role))
    end
  end

  private

  def report_log
    @role.added_managers.each {|manager| create_report_log(type: :add_role, modified_manager: manager, role: @role) } if @role.added_managers
    @role.removed_managers.each {|manager| create_report_log(type: :remove_role, modified_manager: manager, role: @role) } if @role.removed_managers
  end

  def log_managers_and_permissions_updates
    managers_updates = FourEyes::AssociationUpdates.new(original: @role.review.original, updated: @role.review.updated, association_klass: Manager)
    permissions_updates = FourEyes::AssociationUpdates.new(original: @role.review.original, updated: @role.review.updated, association_klass: Permission)

    instrument_logger('uc27.beheerder_beheerrol_toevoegen_gelukt', accounts: managers_updates.added.collect(&:account_name), role_id: @role.id) if managers_updates.added.any?
    instrument_logger('uc27.beheerder_beheerrol_verwijderen_gelukt', accounts: managers_updates.removed.collect(&:account_name), role_id: @role.id) if managers_updates.removed.any?

    instrument_logger('uc27.beheerprivilege_toevoegen_beheerrol_gelukt', privileges: permissions_updates.added.collect(&:name), role_id: @role.id) if permissions_updates.added.any?
    instrument_logger('uc27.beheerprivilege_verwijderen_beheerrol_gelukt', privileges: permissions_updates.removed.collect(&:name), role_id: @role.id) if permissions_updates.removed.any?
  end

   def log(action)
    case action
      when :update, :update_review
        instrument_logger('uc27.beheerrol_gewijzigd_ter_accorderen', role_id: @role.id)
        log_managers_and_permissions_updates
      when :edit, :edit_review
        instrument_logger('uc27.beheerrol_inzien_wijzigen_gelukt', role_id: @role.id, manager_id: session[:current_user_id])
      when :review
        instrument_logger( 'uc27.wijziging_beheerrol_inzien_gelukt', name: @four_eyes_review.updated.name, role: @four_eyes_review.updated)
      when :accept
        instrument_logger('uc27.wijziging_beheerrol_geaccordeerd', name: @four_eyes_review.updated.name, role: @four_eyes_review.updated)
      when :reject
        instrument_logger('uc27.wijziging_beheerrol_afgekeurd', name: @four_eyes_review.updated.name, role: @four_eyes_review.updated)
      when :withdraw
        instrument_logger('uc27.wijziging_beheerrol_ingetrokken', name: @four_eyes_review.updated.name, role: @four_eyes_review.updated)
    end
  end

  def check_four_eyes!
    @role ||= Role.find(params[:id])
    if @role.under_review?
      redirect_to({action: :index}, alert: I18n.t("four_eyes.role_under_review"))
      return
    end
    redirect_to({action: :index}, alert: I18n.t("four_eyes.role_frozen")) if @role.frozen_for_review? || @role.managers_updated_since?(params[:updating_started_at])
  end

end
