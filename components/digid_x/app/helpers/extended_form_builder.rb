
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

class ExtendedFormBuilder < ActionView::Helpers::FormBuilder
  delegate(:concat, :content_tag, :tag, :link_to, to: :template)
  delegate(:t, to: I18n)

  # START OVERRIDING ORIGINAL FORM BUILDER METHODS

  # # Generates something like <select id="model_method" name="model[field]" class="form__item__field" /><option id="first">First</option></select>
  # alias original_select select
  def custom_select(method, choices, select_options, item_options = {})
    item_options[:class] = class_with_default(item_options[:class], "form__item__field")
    build_item([method], item_options) do |field_options|
      select(method, choices, select_options, field_options)
    end
  end

  # Generates something like <input type="checkbox" id="model_method" name="model[field]" class="form__item__field" />
  alias original_check_box check_box
  def check_box(method, options = {}, checked_value = "1", unchecked_value = "0")
    options[:class] = class_with_default(options[:class], "form__item__field", "box")
    build_item([method], options) do |field_options, specific_options|
      [
        original_check_box(method, field_options, checked_value, unchecked_value),
        specific_options.key?(:box_label) ? label(method, specific_options[:box_label].html_safe, class: "form__item__box-label") : nil
      ].compact.join.html_safe
    end
  end

  def custom_check_box(method, options = {}, id = nil, checked_value = "1", unchecked_value = "0")
    label(method, class: "form__item__box-label checkbox_container", id: id) do
      concat(original_check_box(method, options, checked_value, unchecked_value))
      concat(options.key?(:box_label) ? options.delete(:box_label) : "")
      concat(content_tag(:span, nil, class: "checkbox_checkmark"))
    end
  end

  def custom_radio_button(method, value, label_text, options = {})
    label("#{method}_#{value}", class: "form__item__box-label radio_container") do
      concat(radio_button(method, value, options))
      concat(label_text)
      concat(content_tag(:span, nil, class: "radio_checkmark"))
    end
  end

  # Generates something like <input type="password" id="model_method" name="model[field]" class="form__item__field" />
  alias original_password_field password_field
  def password_field(method, options = {})
    options[:class] = class_with_default(options[:class], "form__item__field")
    options[:placeholder] ||= " "
    if options.delete(:toggle_text)
      options[:fieldset_class] = if options[:fieldset_class].present?
                                   options[:fieldset_class] + " show_password_field"
                                 else
                                   "show_password_field"
                                 end
    end
    data = {
      "minimum-capitals" => 1,
      "minimum-digits" => 1,
      "minimum-length" => 8,
      "minimum-minuscules" => 1,
      "minimum-special-characters" => 1,
      "maximum-length" => 32,
      "password" => true,
      "password-strength" => true
    }
    options[:data] = data if options.delete(:password_strength)
    options[:autocomplete] ||= "off"
    build_item([method], options) do |field_options|
      original_password_field(method, field_options)
    end
  end

  alias original_text_field text_field
  # Generates something like <input type="text" id="model_method" name="model[field]" class="form__item__field" />
  def text_field(method, options = {})
    options[:class] = class_with_default(options[:class], "form__item__field")
    options[:autocomplete] ||= "off"
    options[:spellcheck] ||= false

    build_item([method], options) do |field_options|
      [
        item_legend_hidden(options[:legend]),
        general_text_field(method, field_options)
      ].join.html_safe
    end
  end

  # END OVERRIDING ORIGINAL FORM BUILDER METHODS

  def buttons(*actions, &block)
    buttons        = []
    button_options = {}
    actions.compact.each do |action|
      if action.instance_of?(Hash) && action.include?(:action)
        buttons << action.delete(:action)
        button_options[buttons.last] = action
      else
        buttons << action
      end
    end

    template.actions do
      buttons.each do |action|
        button = Button.new(action: action, arrow: button_options.dig(action, :arrow), inverse: button_options.dig(action, :inverse))
        position = button.pos
        options = { class: button.css_class }
        options[:formnovalidate] = true unless button.validate?
        options["data-disable-with"] = t(action) if button.primary?
        options[:id] = button.primary? ? "submit-button" : "cancel-button" # for javascript identification
        options.merge!(button_options[action]) if button_options.include?(action)
        options[:name] = :commit
        options[:value] = t(action)
        concat(button(t(action), options))
      end

      yield(block) if block_given? # add action buttons that are not part of the form
    end
  end

  def citizen_service_number_field(options = {})
    options[:accessibility_information] ||= t("information_boxes.bsn")  
    options[:label] ||= false
    options[:fieldset_class] ||= "with_legend"

    build_item(%i[burgerservicenummer], options) do |field_options|
      [
        legend(t("citizen_service_number"), options[:required]),
        label(:burgerservicenummer, t("citizen_service_number"), class: "screen-reader"),
        general_text_field(:burgerservicenummer, field_options.merge(class: merge_classes(field_options[:class], "form__item__field"), accessibility_information: t("information_boxes.bsn"), "data-bsn8" => options.key?("data-bsn8").present?, maxlength: 9, "data-bsn-format" => true, type: "tel"))
      ].join.html_safe
    end
  end

  def code_field(method, options = {})
    options["data-code"] = options[:code].upcase if options[:code]
    options["data-minimum-length"] = options[:maxlength] || 9
    options[:class] = class_with_default(options[:class], "form__item__field")
    options[:maxlength] ||= 9
    text_field(method, options)
  end

  def sms_code_field(method, options = {})
    options[:required] = true
    options[:hide_required_indicator] = true
    options["data-code"] = "\\d{6}"
    options[:class] = class_with_default(options[:class], "form__item__field", "medium")
    options[:class] = options[:class] + " validate"
    options[:maxlength] ||= 6
    options["data-use-seperator"] = true
    options["data-code-field-type"] = "digits"
    options[:fieldset_class] = "code_field"
    options[:fieldset_class] = options[:fieldset_class] + " android" if template.android_browser?
    options[:type] = "number"
    options[:fieldOf] = t("enter_code_received_field_of")
    text_field(method, options)
  end

  def verification_code_field(method, options = {})
    options[:required] = true
    options[:pattern] = CharacterClass::APP_VERIFICATION_CODE.source
    options["data-minimum-length"] = options[:maxlength] || 4
    options["data-maximum-length"] = options[:maxlength] || 4
    options["data-capitals"] = true
    options["data-code-field-type"] = "consonants"
    options[:class] = class_with_default(options[:class], "form__item__field", "medium")
    options[:class] = options[:class] + " validate"
    options[:fieldset_class] = "code_field"
    options[:fieldset_class] = options[:fieldset_class] + " android" if template.android_browser?
    options[:maxlength] ||= 4
    options[:fieldOf] = t("enter_code_received_field_of")
    text_field(method, options)
  end

  # Generates two radio buttons for confirmation purposes. "Yes" is the first
  # button, "No" the second. Use this in combination with a Confirm object. The
  # labels default to t('yeah') and t('nope').
  def confirm_radio_buttons(yes_label = nil, no_label = nil, options = {})
    radio_buttons(:value, [[yes_label || t("yeah"), true, checked: true], [no_label || t("nope"), false]], options)
  end

  def content
    content_tag(:div, class: "form__item") do
      yield
    end
  end

  def valid_until_date_field(options = {})
    options[:additional_error_methods] ||= ["valid_until"]
    options[:example] ||= t("examples.date_future", configurable_year: 5.years.from_now.year)
    options[:label] ||= false
    options[:fieldset_class] ||= "with_legend"

    build_item(%i[valid_until_day valid_until_month valid_until_year], options) do |field_options|
      [
        legend(t("valid_until_for_passport_or_id_card"), options[:required]),
        label(:valid_until_day, t("day"), class: "screen-reader"),
        general_text_field(:valid_until_day, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--extra-small"), "data-day" => true, maxlength: 2, placeholder: t("dd"), autocomplete: "off")),
        label(:valid_until_month, t("month"), class: "screen-reader"),
        general_text_field(:valid_until_month, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--extra-small"), "data-month" => true, maxlength: 2, placeholder: t("mm"), autocomplete: "off")),
        label(:valid_until_year, t("year"), class: "screen-reader"),
        general_text_field(:valid_until_year, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--small"), "data-id-number-valid-until" => true, "data-id-not-in-past" => true, maxlength: 4, placeholder: t("yyyy"), autocomplete: "off"))
      ].join.html_safe
    end
  end

  def date_of_birth_field(options = {})
    options[:additional_error_methods] ||= ["geboortedatum"]
    options[:example] ||= t("examples.date")
    options[:label] ||= false
    options[:fieldset_class] ||= "with_legend"

    build_item(%i[geboortedatum_dag geboortedatum_maand geboortedatum_jaar], options) do |field_options|
      [
        legend(t("date_of_birth"), options[:required]),
        label(:geboortedatum_dag, t("day"), class: "screen-reader"),
        general_text_field(:geboortedatum_dag, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--extra-small"), "data-day" => true, maxlength: 2, placeholder: t("dd"), autocomplete: "off", type: "tel")),
        label(:geboortedatum_maand, t("month"), class: "screen-reader"),
        general_text_field(:geboortedatum_maand, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--extra-small"), "data-month" => true, maxlength: 2, placeholder: t("mm"), autocomplete: "off", type: "tel")),
        label(:geboortedatum_jaar, t("year"), class: "screen-reader"),
        general_text_field(:geboortedatum_jaar, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--small"), "data-year-in-past" => true, "data-birthdate-not-in-future" => true, maxlength: 4, placeholder: t("yyyy"), autocomplete: "off", type: "tel"))
      ].join.html_safe
    end
  end

  def header(title = nil, options = {})
    options[:class] = class_with_default(options[:class], "form__header")

    content_tag(:header, class: options[:class]) do
      concat(template.block_with_icon(@object.errors[options[:base]].join.html_safe, :error)) if options.key?(:base) && @object.errors.key?(options[:base])
      concat(content_tag(:div, t("required_fields").html_safe + content_tag(:span, "*", class: "form__item__label__required"), class: "form__header__required-fields")) if options[:show_fields_required]
      concat(content_tag(:h2, title, class: "orange_heading")) if title.present?
    end
  end

  def house_number_field(options = {})
    options[:example] ||= t("examples.house_number_and_addition")
    options[:label] ||= false
    options[:fieldset_class] ||= "with_legend"

    build_item(%i[huisnummer huisnummertoevoeging], options) do |field_options|
      [
        legend(t("house_number_and_addition_label"), options[:required]),
        label(:huisnummer, t("house_number"), class: "screen-reader"),
        general_text_field(:huisnummer, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--small"), "data-house-number" => true, maxlength: 5, autocomplete: "off")),
        label(:huisnummertoevoeging, t("house_number_addition"), class: "screen-reader"),
        # addition is never required
        general_text_field(:huisnummertoevoeging, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--small"), "data-house-number-addition" => true, maxlength: 4, autocomplete: "off").except("data-required"))
      ].join.html_safe
    end
  end

  def mobile_number_and_spoken_sms(options = {})
    options[:accessibility_information] ||= t("information_boxes.mobile_number")
    options[:information]               ||= t("information_boxes.mobile_number")
    options[:box_label]                 ||= t("i_want_to_receive_spoken_sms_messages")
    options[:label]                     ||= t("mobile_phone_number")
    options["data-update-existing-number"] = @object.phone_number.present?
    options["data-initial-spoken-sms-value"] = @object.gesproken_sms
    information_block_content = if options.key?(:spoken_sms_messages_information)
                                  options[:spoken_sms_messages_information]
                                else
                                  t("spoken_sms_messages_information")
                                end
    item = build_item(%i[phone_number gesproken_sms], options) do |field_options, specific_options|
      [
        general_text_field(:phone_number, field_options.merge(class: merge_classes(field_options[:class], "form__item__field"), value: options[:value] || @object.user_friendly_phone_number, "data-phone-number" => "true", autocomplete: "tel")),
        tag(:br),
        custom_check_box(:gesproken_sms, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--box", "gesproken_sms_checkbox"), checked: @object.gesproken_sms, box_label: options[:box_label]).except("data-required"))
      ].compact.join.html_safe
    end
    options = { class: "gesproken_sms_infobox" }
    info_block = template.block_with_icon(information_block_content, :information, options)
    [item, info_block].join.html_safe
  end

  def email_and_no_email(options = {})
    options[:box_label] ||= t("activerecord.attributes.email.no_email")
    options["data-maximum-length"] = '254'
    item = build_item(%i[adres no_email], options) do |field_options, specific_options|
      [
        general_text_field(:adres, field_options.merge(class: merge_classes(field_options[:class], "form__item__field"), "data-email": true, value: @object.adres, autocomplete: "email")),
        tag(:br),
        custom_check_box(:no_email, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--box", "no_email_checkbox"), "data-email-or-checkbox": true, checked: @object.no_email, box_label: options[:box_label]).except("data-required"), "email_checkbox")
      ].compact.join.html_safe
    end
    options = { class: "no_email_infobox" }
    item.html_safe
  end

  def postcode_field(options = {})
    options[:label] ||= false
    options[:fieldset_class] ||= "with_legend"
    options[:example] ||= t("examples.postcode")

    build_item(%i[postcode], options) do |field_options|
      [
        legend(t("postcode_label"), options[:required]),
        label(:postcode, t("postcode"), class: "screen-reader"),
        general_text_field(:postcode, field_options.merge(class: merge_classes(field_options[:class], "form__item__field--small"), maxlength: 7, "data-postal-code" => true))
      ].join.html_safe
    end
  end

  def radio_buttons(method, labels_and_values, options = {})
    options[:class] = merge_classes(options[:class], "form__item__field--box")

    build_item([method], options) do |field_options|
      labels_and_values.map do |label_and_value|
        label_text = label_and_value[0]
        value = label_and_value[1]
        options = label_and_value[2] || {}
        custom_radio_button(method, value, label_text, field_options.merge(options))
      end.join.html_safe
    end
  end

  protected

  attr_reader(:template)

  # you can use the following options (besides the normal helper options):
  #
  # accessibility_information:  text to show when :information text is not accessible
  #                             For example screen readers can't access the hidden :information text,
  #                             because it is only displayed when another element has the focus.
  # additional_error_methods:   enables you to specify extra methods that are checked for errors but
  #                             that are not shown
  # box_label:                  label text that is shown on the right of check boxes
  # code:                       set to validate code fields (starting with the given code)
  # example:                    text that is shown after the field
  # information:                text that is shown if a field gets focus (or on other devices, is
  #                             shown below the field)
  # label:                      label text that is shown at the top of each item
  # pattern:                    regular expression that is used for client side validation
  # required:                   set to validate required fields client side; the label will be
  #                             suffixed with an asterisk automatically
  def build_item(methods, options)
    field_options = options.except(:accessibility_information, :additional_error_methods, :box_label, :button, :code, :example, :information, :label, :text, :legend, :hint, :pattern, :required, :fieldset_class, :fieldset_id)
    # although these options are valid HTML 5, we rename them to prevent native browser validations
    field_options["data-pattern"]   = options[:pattern] if options[:pattern].present?
    field_options["data-required"]  = true if options[:required]
    fieldset_options = { class: options[:fieldset_class], id: options[:fieldset_id] }

    specific_options = options.slice(:box_label)
    error_methods = (options[:additional_error_methods] || []) + methods

    link_options = options.select { |key, _value| key.to_s.match(/^link_+/) }
    link_options.keys.each do |key|
      link_options[key.to_s.slice(5..-1).to_sym] = link_options[key]
      link_options.delete(key)
    end

    render_item(accessibility_information: item_accessibility_information(options[:accessibility_information]),
                example: item_example(options[:example]),
                field: yield(field_options, specific_options),
                fieldset_options: fieldset_options,
                information: item_information(options[:information]),
                button: item_button(options[:button], options[:button_class]),
                link: item_link(link_options.delete(:title), link_options.delete(:href), link_options.delete(:class), link_options),
                label: item_label(methods[0], options[:label], options[:required], options[:hide_required_indicator]),
                text: item_text(options[:text]),
                hint: options[:hint].present? ? content_tag(:div, options[:hint], "class" => "form__item__hint") : nil,
                errors: item_errors(error_methods),
                errors_present_flag: errors_present?(error_methods))
  end

  # Returns the current class string if it already contains an individual class starting with
  # default class; if it doesn't, it adds that default class, optionally a variant of it.
  #
  # Examples:
  # class_with_default(nil, 'form__item__field') => 'form__item__field'
  # class_with_default(nil, 'form__item__field', 'box') => 'form__item__field--box'
  # class_with_default('my-class', 'form__item__field', 'small') => 'my-class form__item__field--small'
  def class_with_default(current_class, default_class, variant = nil)
    return current_class if current_class =~ Regexp.new(default_class)

    new_class = [default_class, variant].compact.join("--")
    merge_classes(current_class, new_class)
  end

  # Renders the part of the item for the accessibility information. The HTML is as follows:
  #
  # <div class="accessibility__information">
  #   <p>{{text}}</p>
  # </div>
  def item_accessibility_information(text)
    text.blank? ? nil : content_tag(:div, text, class: "accessibility__information")
  end

  # flag that indicates whether there is an error on the fieldset using given
  # methods. Does not coincide with the presence of actual item_errors since
  # we allow for fields to be indicated as error without having error messages
  # (user instructions will be derived from elsewhere)
  def errors_present?(methods)
    error_list_for_method_set(methods).present?
  end

  # Renders the part of the item for errors. The HTML is as follows:
  #
  # <ul class="form__item__errors">
  #   <li>{{error #1}}</li>
  #   <li>{{error #2}}</li>
  # </ul>
  def item_errors(methods)
    # for each (unique) error on each given method, a list item is created
    # we ignore empty errors, note however that presence of empty errors still results
    # in the css class form__item__errors, see method errors_present?
    error_li_items = error_list_for_method_set(methods).reject(&:empty?).map { |error| content_tag(:li, error.html_safe) }
    if error_li_items.empty?
      nil
    else
      content_tag(:ul, t("something_went_wrong").html_safe + error_li_items.uniq.join.html_safe, class: "form__item__errors", "aria-live" => "assertive")
    end
  end

  # Renders the part of the item for the example. The HTML is as follows:
  #
  # <span class="form__item__example">{{text}}</span>
  def item_example(text)
    text.blank? ? nil : content_tag(:span, text.html_safe, class: "form__item__example")
  end

  # Renders a button next to the rendered_item
  # klass: 'actions__right--button', 'actions__left--button'
  # <input class="{{klass}}" name="commit" type="submit" value="{{text}}" />
  def item_button(button, klass)
    button.blank? ? nil : submit(t(button), class: klass, formnovalidate: true, tabindex: -1)
  end

  # Renders a link next to the rendered_item
  # <input class="{{klass}}" name="commit" type="submit" value="{{text}}" />
  def item_link(link_title, link_href, link_class, options = {})
    link_title.blank? ? nil : link_to(t(link_title), link_href, { class: link_class }.merge!(options))
  end

  # Renders the part of the item for the information. The HTML is as follows:
  #
  # <div class="form__item__information">
  #   <p>{{text}}</p>
  # </div>
  def item_information(text)
    text.blank? ? nil : content_tag(:div, text, class: "form__item__information")
  end

  # Renders the part of the item for the label. The HTML is as follows:
  #
  # <label for="{{model}}_{{method}}" class="form__item__label">
  #   {{text}}<span class="form__item__label__required">*</span>
  # </label>
  def item_label(method, text, required, hide_required_indicator = false)
    return nil if text == false

    text = object.class.human_attribute_name(method) if text.blank? && object.present?
    text = t(method) if text.blank?

    if hide_required_indicator
      asterisk_content = ""
    else
      asterisk_content = content_tag(:span, "*", class: "form__item__label__required")
      # add an asterisk placeholder if the text doesn't already include it and the field is required
      text += "[*]" if required && text.exclude?("[*]")
    end

    label(method, text.gsub("[*]", asterisk_content).html_safe, class: "form__item__label")
  end

  # Renders the part of the item for the text. The HTML is as follows:
  #
  #<div class="form__item__text"}">
  # {{text}}
  #</div>
  def item_text(text)
    text.blank? ? nil : content_tag(:div, text.html_safe, class: "form__item__text")
  end

  # Renders the part of the item for the hidden legend The HTML is as follows:
  #
  #<legend class="form__item__legend__visually__hidden">
  # {{legend}}
  #</legend>
  def item_legend_hidden(legend)
    legend.blank? ? nil : content_tag(:legend, legend.html_safe, class: "form__item__legend__visually__hidden")
  end

  def merge_classes(*classes)
    classes.flat_map { |c| (c || "").split }.uniq.join(" ").presence
  end

  # <fieldset class="form__item|form__item--error">
  #   {{label}}
  #   {{text}}
  #   <fieldset>
  #     {{legend}}
  #     {{code fields}}
  #   </fieldset>
  #   {{accessibility_information}}
  #   {{example}}
  #   {{field}}
  #   {{information}}
  #   {{errors}}
  # </fieldset>
  def render_item(elements)
    content_tag(:fieldset, class: merge_classes("form__item#{'--error' if elements[:errors_present_flag]}", elements[:fieldset_options][:class]), id: elements[:fieldset_options][:id]) do
      concat(elements[:label]) if elements[:label].present?
      concat(elements[:text]) if elements[:text].present?
      concat(content_tag(:fieldset) do
        concat(elements[:legend])
      end 
      ) if elements[:legend].present?
      concat(elements[:accessibility_information]) if elements[:accessibility_information].present?
      concat(elements[:button]) if elements[:button].present?
      concat(elements[:example]) if elements[:example].present?
      concat(elements[:field])
      concat(elements[:link]) if elements[:link].present?
      concat(elements[:information]) if elements[:information].present?
      concat(elements[:errors]) if elements[:errors].present?
      concat(elements[:hint]) if elements[:hint].present?
    end
  end

  private

  def error_list_for_method_set(methods)
    @error_list_for_method_set ||= {}
    @error_list_for_method_set[methods] ||= methods.reduce([]) do |result, method|
      result + @object.errors[method].flatten
    end
  end

  def general_text_field(method, options = {})
    options[:placeholder] ||= " "

    if options[:type] == "number"
      number_field(method, options)
    else
      original_text_field(method, options)
    end
  end

  def legend(text, required)
    asterisk_content = content_tag(:span, "*", class: "form__legend__required")
    # add an asterisk placeholder if the text doesn't already include it and the field is required
    text += "[*]" if required && text.exclude?("[*]")

    content_tag(:legend, text.gsub("[*]", asterisk_content).html_safe, class: "form__legend")
  end

  def first_empty_field?(field)
    # a field is the first empty field if there has been no other empty field before
    return false if @previous_field_empty
    # and it is not empty itself
    return false if object.send(field).present?

    # remember we have a first empty field now and return true
    @previous_field_empty = true
  end
end
