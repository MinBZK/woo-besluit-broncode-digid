
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

# frozen_string_literal: true

module ApplicationHelper
  def action_button(title, action, position = :left, options = {})
    options[:class] = merge_classes(options[:class], "actions__#{position}--button")
    button_to(title, action, options)
  end

  def action_link(title, action, position = :left, options = {})
    options[:class] = merge_classes(options[:class], "actions__#{position}--link")
    link_to(title, action, options)
  end

  def action_submit(title, position = :left, options = {})
    options[:class] = merge_classes(options[:class], "actions__#{position}--button")
    options[:formnovalidate] = true if options.delete(:validate) == false
    submit_tag(title, options)
  end

  def actions
    content_tag(:div, class: 'actions') do
      yield
    end
  end

  def active_link_to(name = nil, options = nil, html_options = nil, &block)
    controllers = ([] << html_options.delete(:controller)).flatten
    html_options[:class] = "#{html_options[:class]} #{html_options[:class]}--active" if controllers.include?(params[:controller].to_sym)
    link_to(name, options, html_options, &block)
  end

  def block_with_icon(content = nil, variant = :information, options = {})
    options[:class] = merge_classes(options[:class], "block-with-icon--#{variant}")
    content_tag(:div, options) do
      block_given? ? yield : content_tag(:p, content)
    end
  end

  # like form_for() but using ExtendedFormBuilder
  def extended_form_for(record_or_name_or_array, *args, &block)
    options = args.extract_options!
    form_for(
      record_or_name_or_array,
      *(args << options.merge(builder: ExtendedFormBuilder)),
      &block
    )
  end

  def flash_alert
    block_with_icon(flash[:alert], :error) if flash[:alert].present?
  end

  def flash_notice
    block_with_icon(flash[:notice], :information) if flash[:notice].present?
  end

  # small wrapper for image_tag that sets the title equal to the alt
  def image(source, options = {})
    options[:alt] ||= ''
    options[:title] ||= options[:alt]
    image_tag(source, options)
  end

  def merge_classes(*classes)
    classes.compact.map(&:split).flatten.uniq.join(' ').presence
  end

  def where_who_helper
    return '' unless current_user
    "#{t('digid_front_desk')} #{current_front_desk.try(:name)} (#{truncate(current_user.pseudonym, length: 12, omission: '')})"
  end

  def markdown(text)
    options = {
      escape_html:  true,
      safe_links:   true
    }
    BlueCloth.new(text, options).to_html.html_safe
  end
end
