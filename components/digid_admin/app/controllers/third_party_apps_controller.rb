
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

class ThirdPartyAppsController < ApplicationController
  include FourEyesReviewControllerConcern
  include FourEyesReviewsHelper
  load_and_authorize_resource only: [:index, :show, :edit, :new, :create, :update, :destroy, :update_warning]

  def index
    @third_party_apps_in_review = FourEyesReview.where(record_table: "ThirdPartyApp")
    @search = ThirdPartyApp.ransack params[:q]
    @search.sorts = (params[:q] && params[:q][:s]) || 'return_url desc'
    @third_party_apps = @search.result.page(params[:page]).per(30)
    instrument_logger('third_party_app.inzien', entity: "Third-party app", subject_type: Log::SubjectTypes::THIRD_PARTY_APP)
  end

  def new

  end

  def edit
    @cancel_button = true
    log(:edit)
  end

  # POST /third_party_app
  def create
    if @third_party_app.save_for_review(manager: current_user)
      log_created
      flash[:notice] = t('third_party_app.created_successfully')
      redirect_to(third_party_apps_path)
    else
      render :new
    end
  end

  def update
    if @third_party_app.update_for_review(params[:third_party_app], manager: current_user)
      log(:update)
      flash[:notice] = t('third_party_app.updated_successfully')
      redirect_to(third_party_apps_path)
    else
      render :edit
    end
  end

  def destroy
    @third_party_app.destroy_for_review(manager: current_user)
    log_destroy
    flash[:notice] = t('third_party_app.destroy_successfully')
    redirect_to(third_party_apps_path)
  end

  def update_warning
    @third_party_app.assign_attributes(updatable_third_party_app_params)

    text = save_warning(@third_party_app)

    if text
      render json: { warning: true, text: text }
    else
      render json: { warning: false }
    end
  end

  private

  def log(action)
    case action
      when :edit, :edit_review
        instrument_logger('third_party_app.edit', third_party_app_id: @third_party_app.id, user_agent: @third_party_app.user_agent, return_url: @third_party_app.return_url)
      when :update, :update_review
        log_changes(@third_party_app.review)
      when :review, :accept, :reject, :withdraw
        updated_third_party_app = (@four_eyes_review.updated.new_record? && !@four_eyes_review.original.new_record?) ? @four_eyes_review.original : @four_eyes_review.updated
        return_url = updated_third_party_app.return_url
        user_agent = updated_third_party_app.user_agent
        id = updated_third_party_app.id
        instrument_logger("#{log_mapping[action.to_sym]}", return_url: return_url, user_agent: user_agent, third_party_app_id: id, entity: "Third-party app")
    end
  end

  def new_third_party_app_params
    params.require(:third_party_app).permit(
      :user_agent,
      :return_url
    )
  end

  def updatable_third_party_app_params
    params.require(:third_party_app).permit(
      :return_url, :user_agent
    )
  end

  def log_created
    instrument_logger(
      '1424',
      entity: "Third-party app",
      subject_type: Log::SubjectTypes::THIRD_PARTY_APP
    )
  end

  def log_changes(review)
    instrument_logger(
      '1425',
      entity: "Third-party app",
      subject_type: Log::SubjectTypes::THIRD_PARTY_APP
      )
  end

  def log_destroy
    instrument_logger(
      "1426",
      entity: "Third-party app",
      subject_type: Log::SubjectTypes::THIRD_PARTY_APP
    )
  end

  def localize(*args)
    # Avoid I18n::ArgumentError for nil values
    I18n.localize(*args) unless args.first.nil?
  end

  private

  def log_mapping
    { review: "1432",
      accept: "1433",
      reject: "1434",
      withdraw: "1435" }
  end
end
