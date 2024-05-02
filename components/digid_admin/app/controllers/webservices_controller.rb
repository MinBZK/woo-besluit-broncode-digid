
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

class WebservicesController < ApplicationController
  include FourEyesReviewControllerConcern
  load_and_authorize_resource only: [:index, :show, :new, :edit, :create, :update, :destroy], param_method: :webservice_params
  before_action :webservice_params, only: [:update, :update_review]

  respond_to :html

  # GET /webservices
  def index
    @webservices_in_review = FourEyesReview.where(record_table: "Webservice")
    @search = Webservice.ransack(g: [
                                  { m: 'or', id_eq: params[:q], name_or_description_or_organization_name_or_sectors_name_cont: params[:q] }
                                ])
    @search.active_eq = params[:active] if params[:active]
    @webservices = @search.result(distinct: true).page(params[:page])
    if params[:commit]&.include?("Filter") && (params[:q].present? || params[:active].present?)
      log(:search)
    else
      log(:index)
    end
  end

  # GET /webservices/1
  def show
    log(:show)
    current_user.webservice_viewed(@webservice)
  end

  # GET /webservices/new
  def new
    @webservice.build_saml_provider
    @webservice.build_aselect_webservice
  end

  # GET /webservices/1/edit
  def edit
    log(:edit)
    current_user.webservice_viewed(@webservice)
  end

  # POST /webservices
  def create
    if @webservice.save_for_review(manager: current_user)
      instrument_logger('uc14.webdienst_aanmaken_gelukt', name: @webservice.name, webservice_id: @webservice.id)
      redirect_to review_webservices_path(@webservice.review)
    else
      render :new
    end
  end

  def update
    if @webservice.update_for_review(params[:webservice], manager: current_user)
      log(:update)
      redirect_to review_webservices_path(@webservice.review)
    else
      render :edit
    end
  end

  # DELETE /webservices/1
  def destroy
    if @webservice.destroy_for_review(manager: current_user)
      log(:destroy)
      redirect_to(review_webservices_path(@webservice.review))
    else
      redirect_to @webservice
    end
  end

  def authorize_sector
    @webservice = Webservice.where(id: params[:webservice_id]).first_or_initialize
    if @webservice.sector_authentications.map(&:sector_id).include?(params[:sector_id].to_i)
      @sector_authentication = @webservice.sector_authentications.find_by_sector_id(params[:sector_id])
    else
      @sector_authentication = @webservice.sector_authentications.build(sector_id: params[:sector_id])
    end

    render json: {
      append: render_to_string(partial: "sector_authentication_fields", locals: { sector_authentication: @sector_authentication }),
      id: @sector_authentication.sector.id
    }
  end

  def log(action)
    case action
      when :index
        instrument_logger('373')
      when :search
        instrument_logger('374')
      when :show
        instrument_logger('uc13.webdienst_inzien_gelukt', webservice_id: @webservice.id)
      when :review
        instrument_logger('uc14.wijziging_webdienst_inzien', name: @four_eyes_review.updated.name, webservice_id: @four_eyes_review.updated.id)
      when :destroy
        instrument_logger('uc14.webdienst_verwijderen_gelukt', webservice_id: @webservice.id)
      when :accept
        instrument_logger('uc14.wijziging_webdienst_geaccordeerd', name: @four_eyes_review.updated.name, webservice_id: @four_eyes_review.updated.id)
      when :reject
        instrument_logger('uc14.wijziging_webdienst_afgekeurd', name: @four_eyes_review.updated.name, webservice_id: @four_eyes_review.updated.id)
      when :withdraw
        instrument_logger('uc14.wijziging_webdienst_ingetrokken', name: @four_eyes_review.updated.name, webservice_id: @four_eyes_review.updated.id)
      when :edit, :edit_review
        instrument_logger('761')
      when :update, :update_review
        instrument_logger('uc14.webdienst_wijzigen_gelukt', name: @webservice.name, webservice_id: @webservice.id)
        if @webservice.review.original.active == true && @webservice.review.updated.active == false
          instrument_logger('uc14.webdienst_inactief_zetten_gelukt', webservice_id: @webservice.id)
        elsif @webservice.review.original.active == false && @webservice.review.updated.active == true
          instrument_logger('uc14.webdienst_actief_zetten_gelukt', webservice_id: @webservice.id)
        end
      end
  end

  private
  def webservice_params
    params[:webservice].tap {|ws| ws[:apps] = ws[:apps].to_h.map(&:second) }
  end

  def flash_succes_for(four_eyes_review)
    name = four_eyes_review.updated.name
    action_text = I18n.t("four_eyes_reviews.#{four_eyes_review.action}")
    result_text = I18n.t("four_eyes_reviews.#{action_name}")
    model_name = I18n.t("four_eyes_reviews.flash.success.#{four_eyes_review.record_table.underscore.downcase}")

    flash[:notice] = "#{action_text} #{model_name} #{name} #{result_text}."
  end

end
