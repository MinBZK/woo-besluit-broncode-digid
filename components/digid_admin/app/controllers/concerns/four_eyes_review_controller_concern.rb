
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

module FourEyesReviewControllerConcern
  extend ActiveSupport::Concern

  included do
    before_action :load_and_authorize_review, only: [:withdraw_review, :review, :accept_review, :reject_review, :edit_review, :update_review]
    before_action :if_review_up_to_date?, only: [:accept_review, :reject_review, :update_review]
    before_action :if_original_up_to_date?, only: [:update, :destroy]
    before_action :review_exists?, only: [:update, :destroy]
    before_action :original_frozen_for_review?, only: [:edit_review, :update_review]
    rescue_from ActiveRecord::RecordNotFound, :with => :record_not_found
  end

  def review
    log(:review)
  end

  def edit_review
    instance_variable_set("@#{controller_name.singularize}", @four_eyes_review.updated)
    log(:edit_review)
    render :edit
  end

  def update_review
    instance_variable_set("@#{controller_name.singularize}", @four_eyes_review.update_review(params: model_params))

    if serialized_model.valid?
      @four_eyes_review.serialized_object = serialized_model.sanitized_params
      @four_eyes_review.note = model_params[:note]
      @four_eyes_review.save!
      log(:update)
      redirect_to send("review_#{controller_path.gsub("/", "_")}_path", @four_eyes_review)
    else
      render :edit
    end
  end

  def withdraw_review
    @four_eyes_review.destroy
    log(:withdraw)
    flash[:notice] = flash_succes_for @four_eyes_review
    redirect_to "/#{controller_path}"
  end

  def accept_review
    model_name = controller_name.singularize
    instance_variable_set("@#{model_name}", @four_eyes_review.updated)
    instance_variable_get("@#{model_name}").valid? if @four_eyes_review.action != "destroy" && @app_version
    if instance_variable_get("@#{model_name}").errors.empty? && @four_eyes_review.accept(manager: current_user)
      log(:accept)
      report_log
      flash[:notice] = flash_succes_for @four_eyes_review
      FourEyesReviewMailer.delay(queue: "email-admin").notify_creator(@four_eyes_review.serialized_hash, current_user.id, result: 'accepted') unless @four_eyes_review.manager.email.blank?
      redirect_to "/#{controller_path}"
    else
      flash[:action] = "Accorderen"
      render :review
    end
  end

  def report_log
    # noop
  end

  def create_report_log(type:, modified_manager:, role: nil)
    FourEyesReport.create!(
      description: I18n.t("four_eyes_reports.#{type}", role: role),
      manager: modified_manager,
      creator_manager: @four_eyes_review.manager,
      acceptor_manager: @current_user,
      changed_at: @four_eyes_review.updated_at
    )
  end

  def reject_review
    @four_eyes_review.destroy
    log(:reject)
    flash[:notice] = flash_succes_for @four_eyes_review
    FourEyesReviewMailer.delay(queue: "email-admin").notify_creator(@four_eyes_review.serialized_hash, current_user.id, result: 'rejected') unless @four_eyes_review.manager.email.blank?
    redirect_to "/#{controller_path}"
  end

  private

  def render_four_eyes_dialog
    render json: { dialog_content: render_to_string(partial: "four_eyes_reviews/note_dialog"), title: t("four_eyes_reviews.dialog_title.#{controller_name.singularize}") }
  end

  def flash_succes_for(four_eyes_review)
    name = four_eyes_review.updated.name
    action_text = I18n.t("four_eyes_reviews.#{four_eyes_review.action}")
    result_text = I18n.t("four_eyes_reviews.#{action_name}")
    model_name = I18n.t("four_eyes_reviews.flash.success.#{four_eyes_review.record_table.underscore.downcase}")

    flash[:notice] = "#{action_text} #{name} #{model_name} #{result_text}."
  end

  def review_exists?
    if FourEyesReview.find_by(record_id: params[:id], record_table: controller_name.classify).present?
      flash[:alert] = I18n.t("four_eyes_reviews.already_exists")
      redirect_to "/#{controller_path}"
    end
  end

  def no_review_found
    if [:accept_review, :reject_review].include?(action_name.to_sym)
      flash[:alert] = I18n.t("four_eyes_reviews.rejected_anyone_else_already_accepted")
    else #[:withdraw_review, :edit_review].include?(action_name.to_sym)
      flash[:alert] = I18n.t("four_eyes_reviews.already_accepted_or_rejected")
    end
    redirect_to "/#{controller_path}"
  end

  def unauthorized
    flash[:alert] = I18n.t("four_eyes_reviews.unauthorized")
    redirect_to "/#{controller_path}"
  end

  def load_and_authorize_review
    @four_eyes_review = FourEyesReview.find_by(id: params[:id])
    return no_review_found if @four_eyes_review.nil?
    instance_variable_set("@#{controller_name.singularize}", @four_eyes_review.updated)

    case action_name.to_sym
      when :withdraw_review, :edit_review, :update_review
        unauthorized if (@four_eyes_review.manager_id != current_user.id) || (cannot? :manage, @four_eyes_review.original)
      when :review
        unauthorized if (cannot? :read, @four_eyes_review) || (cannot? :read, @four_eyes_review.original)
      when :accept_review, :reject_review
        unauthorized unless (@four_eyes_review.manager_id != current_user.id && can?(:accorderen, @four_eyes_review.original)) || can?(:accept_own_change, @four_eyes_review)
    end
  end

  def record_not_found
    flash[:alert] = I18n.t("four_eyes_reviews.already_exists")
    redirect_to "/#{controller_path}"
  end

  def if_review_up_to_date?
    return unless params[:review_updated_at] || params[:updated_at]
    if @four_eyes_review.updated_at > (params[:review_updated_at] || params[:updated_at])
      if params[:review_updated_at]
        flash[:alert] = I18n.t("four_eyes_reviews.already_exists")
      else
        flash[:alert] = I18n.t("four_eyes_reviews.rejected_anyone_else_already_accepted")
      end
      redirect_to "/#{controller_path}"
    end
  end

  def if_original_up_to_date?
    return unless params[:original_updated_at]
    model = model_class.find_by(id: params[:id])
    four_eyes_updated_at = case
      when model.is_a?(Manager)
        model.four_eyes_updated_at
      else
        model.try(:four_eyes_updated_at) || model.updated_at
    end
    if four_eyes_updated_at && four_eyes_updated_at > params[:original_updated_at]
      flash[:alert] = I18n.t("four_eyes_reviews.already_exists")
      redirect_to "/#{controller_path}"
    end
  end

  def original_frozen_for_review?
    if @four_eyes_review.original.try(:frozen_for_review?)
      flash[:alert] = I18n.t("four_eyes_reviews.frozen_concurrency")
      redirect_to "/#{controller_path}"
    end
  end

  def model_class
    controller_path.singularize.classify.gsub("/", "::").constantize
  end

  def model_params
    params[serialized_model.class.to_s.snakecase.downcase.gsub("/", "_").to_sym]
  end

  def serialized_model
    instance_variable_get("@#{controller_name.singularize}")
  end
end
