
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

class AppVersionsController < ApplicationController
  include FourEyesReviewControllerConcern
  include FourEyesReviewsHelper
  load_and_authorize_resource only: [:index, :show, :edit, :new, :create, :update, :destroy, :update_warning]

  def index
    @app_versions_in_review = FourEyesReview.where(record_table: "AppVersion")
    @search = AppVersion.ransack params[:q]
    @search.sorts = (params[:q] && params[:q][:s]) || 'version desc'
    @app_versions = @search.result.page(params[:page]).per(30)
    instrument_logger('app_version.inzien', subject_type: Log::SubjectTypes::APP_VERSION)
  end

  def new

  end

  def edit
    @cancel_button = true
    log(:edit)
  end

  # POST /app_version
  def create
    if @app_version.save_for_review(manager: current_user)
      log_created
      redirect_to review_app_versions_path(@app_version.review)
    else
      render :new
    end
  end

  def update
    if @app_version.update_for_review(params[:app_version], manager: current_user)
      log(:update)
      redirect_to review_app_versions_path(@app_version.review)
    else
      render :edit
    end
  end

  def destroy
    if params[:app_version] && params[:app_version][:note].present?
      @app_version.note = params[:app_version][:note]
    else
      @app_version.errors.add(:note, :blank)
      render_four_eyes_dialog
      return
    end
    if @app_version.destroy_for_review(manager: current_user)
      log_destroy
      redirect_via_js(review_app_versions_path(@app_version.review))
    else
      redirect_via_js(edit_app_version_path @app_version)
    end
  end

  def update_warning
    @app_version.assign_attributes(updatable_app_version_params)

    text = save_warning(@app_version)

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
        instrument_logger('app_version.edit', app_version_id: @app_version.id, operating_system: @app_version.operating_system, version: @app_version.version, release_type: @app_version.release_type)
      when :update, :update_review
        log_changes(@app_version.review)
      when :review, :accept, :reject, :withdraw
        updated_app_version = (@four_eyes_review.updated.new_record? && !@four_eyes_review.original.new_record?) ? @four_eyes_review.original : @four_eyes_review.updated
        version = updated_app_version.version
        operating_system = updated_app_version.operating_system
        release_type = updated_app_version.release_type
        id = updated_app_version.id
        instrument_logger("#{log_mapping[action.to_sym]}", version: version, operating_system: operating_system, release_type: release_type, app_version_id: id)
    end
  end

  def new_app_version_params
    params.require(:app_version).permit(
      :operating_system,
      :version,
      :not_valid_before,
      :not_valid_on_or_after
    )
  end

  def updatable_app_version_params
    params.require(:app_version).permit(
      :not_valid_before,
      :not_valid_on_or_after,
      :not_valid_before,
      :not_valid_on_or_after,
      :kill_app_on_or_after,
      :note
    )
  end

  def log_created
    instrument_logger(
      'app_version.created',
      app_version_id: @app_version.id,
      operating_system: @app_version.operating_system,
      version: @app_version.version,
      release_type: @app_version.release_type,
      not_valid_before: localize(@app_version.not_valid_before),
      not_valid_on_or_after: localize(@app_version.not_valid_on_or_after),
      kill_app_on_or_after: localize(@app_version.kill_app_on_or_after)
    )
  end

  def log_changes(review)
    instrument_logger('app_version.updated',
      app_version_id: review.updated.id,
      operating_system: review.updated.operating_system,
      version: review.updated.version,
      release_type: @app_version.release_type,
      not_valid_before: localize(review.updated.not_valid_before),
      not_valid_on_or_after: localize(review.updated.not_valid_on_or_after),
      kill_app_on_or_after: localize(review.updated.kill_app_on_or_after),
      not_valid_before_was: localize(review.original.not_valid_before),
      not_valid_on_or_after_was: localize(review.original.not_valid_on_or_after),
      kill_app_on_or_after_was: localize(review.original.kill_app_on_or_after)
      )
  end

  def log_destroy
    instrument_logger(
      "776",
      app_version_id: @app_version.id,
      operating_system: @app_version.operating_system,
      version: @app_version.version,
      release_type: @app_version.release_type,
      not_valid_before: localize(@app_version.not_valid_before),
      not_valid_on_or_after: localize(@app_version.not_valid_on_or_after),
      kill_app_on_or_after: localize(@app_version.kill_app_on_or_after)
    )
  end

  def localize(*args)
    # Avoid I18n::ArgumentError for nil values
    I18n.localize(*args) unless args.first.nil?
  end

  private

  def log_mapping
    { review: "823",
      accept: "825",
      reject: "826",
      withdraw: "827" }
  end
end
