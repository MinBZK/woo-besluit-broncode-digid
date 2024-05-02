
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

module FourEyesReviewsHelper
  def four_eyes_edit_link(name, link, model)
    field = ''
    field << if in_review?(model) || is_frozen?(model)
               "<span class='disabled'>#{name}</span>"
             else
               edit_link(name, link)
             end
    field.html_safe
  end

  def red_if_different(item, compare_against)
    if !item.nil? && item != '' &&
       !compare_against.nil? && item != compare_against
      haml_tag :span, class: 'strong red' do
        logger.ap(item)
        item
      end
    else
      haml_tag :span do
        logger.ap(item)
        item
      end
    end
  end

  def four_eyes_show_link(name, model, icon: true)
    field = ''
    field << if in_review?(model)
                if icon == true
                  show_link(name, send("review_#{model.class.name.underscore.pluralize.gsub("/", "_")}_path", model.review))
                else
                  link_to(name, send("review_#{model.class.name.underscore.pluralize.gsub("/", "_")}_path", model.review))
                end
             else
                if icon == true
                  show_link(name, model)
                else
                  link_to(name, model)
                end
             end
    field.html_safe
  end

  def four_eyes_cancel_link(name, model)
    field = ''
    if controller.respond_to?(:show) && !model.new_record?
      field << if in_review?(model)
                 show_link(name, send("review_#{model.class.name.underscore.pluralize.gsub("/", "_")}_path", model.review))
               else
                 link_to(name, model)
               end
    end
    field.html_safe
  end

  def four_eyes_model?(model)
    model.class.included_modules.include?(FourEyes)
  end

  def in_review?(model)
    four_eyes_model?(model) && model.under_review?
  end

  def is_frozen?(model)
    model.respond_to?(:frozen_for_review?) && model.frozen_for_review?
  end

  def update_warning(four_eyes_review)
    if four_eyes_review.record_table == "AppVersion" && four_eyes_review.action == FourEyesReview::Action::UPDATE && four_eyes_review.updated.valid? &&
        (four_eyes_review.updated.not_valid_on_or_after || four_eyes_review.updated.kill_app_on_or_after)
      return t('app_version.force_update_no_new_version') if !four_eyes_review.updated.newer_version_available_for_force_update?
      return t('app_version.kill_app_no_other_version') if !four_eyes_review.updated.other_version_available_for_kill_app?
    elsif four_eyes_review.record_table == "AppVersion" && four_eyes_review.action == FourEyesReview::Action::DESTROY
      four_eyes_review.original.other_active_version_exists? ? t('are_you_sure') : t('app_version.destroy_warning')
    else
      nil
    end
  end

  def save_warning model
    return t('app_version.force_update_no_new_version') if !model.newer_version_available_for_force_update?
    return t('app_version.kill_app_no_other_version') if !model.other_version_available_for_kill_app?
  end

  def four_eyes_form_for(model, args = {}, &block)
    if model.under_review?
      args[:url] = send("review_#{model.class.name.underscore.gsub("/", "_").pluralize}_path", model.review)
      args[:method] = :put
    end

    form_for model, args do |f|
      concat render 'shared/errors', model: model
      haml_tag :input, type: "hidden", name: "original_updated_at", value: model.updated_at if model.persisted?
      haml_tag :input, type: "hidden", name: "review_updated_at", value: model.review.updated_at if model.review
      haml_tag :div, class: "four_eyes_review_note" do
        concat field f.label(:note), f.text_field(:note, maxlength: 255, required: true)
      end
      block.call f
    end
  end

  def changes_for(original, new, review)
    yield FourEyesReviewPresenter.new(original, new, review.action, review, self)
  end

end
