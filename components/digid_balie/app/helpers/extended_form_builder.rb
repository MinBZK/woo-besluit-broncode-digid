
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

class ExtendedFormBuilder < ActionView::Helpers::FormBuilder # rubocop:disable Metrics/ClassLength
  delegate(:concat, :content_tag, :tag, to: :template)
  delegate(:t, to: I18n)

  # START OVERRIDING ORIGINAL FORM BUILDER METHODS

  alias original_check_box check_box
  def check_box(method, options = {}, checked_value = '1', unchecked_value = '0')
    options[:class] = class_with_default(options[:class], 'form__item__field', 'box')
    build_item([method], options) do |field_options, specific_options|
      [
        original_check_box(method, field_options, checked_value, unchecked_value),
        specific_options.key?(:box_label) ? label(method, specific_options[:box_label].html_safe + (content_tag(:span, '*', class: 'form__item__label__required') if field_options['data-required']), class: 'form__item__box-label', required: true) : nil
      ].compact.join.html_safe
    end
  end

  # Generates something like
  # <input type="password" id="model_method" name="model[field]" class="form__item__field" />
  alias original_password_field password_field
  def password_field(method, options = {})
    options[:class] = class_with_default(options[:class], 'form__item__field')
    data = {
      'minimum-capitals' => 1,
      'minimum-digits' => 1,
      'minimum-length' => 8,
      'minimum-minuscules' => 1,
      'minimum-special-characters' => 1,
      'maximum-length' => 32,
      'password' => true,
      'password-strength' => true
    }
    options[:data] = data if options.delete(:password_strength)
    build_item([method], options) do |field_options|
      original_password_field(method, field_options)
    end
  end

  alias original_select select
  def select(method, collection, options = {})
    options[:class] = class_with_default(options[:class], 'form__item__field')
    build_item([method], options) do
      original_select(method, collection)
    end
  end

  alias original_text_field text_field
  # Generates something like
  # <input type="text" id="model_method" name="model[field]" class="form__item__field" />
  def text_field(method, options = {})
    options[:class] = class_with_default(options[:class], 'form__item__field')
    options[:autocomplete] ||= 'off'
    options[:spellcheck] ||= false
    build_item([method], options) do |field_options|
      general_text_field(method, field_options)
    end
  end

  alias original_text_area text_area
  # Generates something like
  # <input type="text" id="model_method" name="model[field]" class="form__item__field" />
  def text_area(method, options = {})
    options[:class] = class_with_default(options[:class], 'form__item__field')
    options[:autocomplete] ||= 'off'
    options[:spellcheck] ||= false
    build_item([method], options) do |field_options|
      original_text_area(method, field_options)
    end
  end

  # END OVERRIDING ORIGINAL FORM BUILDER METHODS

  def buttons(*actions)
    available_buttons = {
      left: %i[edit activate change log_in next save_password],
      right: %i[cancel skip skip_verification switch_to_buitenland_aanvraag_proces]
    }
    # for now all buttons on the left cause client side validation (this might change)
    validation_buttons = available_buttons[:left]

    template.actions do
      %i[left right].each do |direction|
        available_buttons[direction].each do |button|
          # render button if it is included in the argument list
          next unless actions.include?(button)

          options = { class: "actions__#{direction}--button" }
          options[:formnovalidate] = true if validation_buttons.exclude?(button)
          concat(submit(t(button), options))
        end
      end
    end
  end

  def citizen_service_number_field(options = {})
    options['data-citizen-service-number'] = true unless options.key?('data-citizen-service-number')
    options[:label] ||= t('citizen_service_number').html_safe
    options[:maxlength] ||= 9
    options['data-bsn-format'] = true
    text_field(:citizen_service_number, options)
  end

  def code_field(method, options = {})
    options['data-code'] = options[:code].upcase if options[:code]
    options['data-minimum-length'] = options[:maxlength] || 9
    options['data-maximum-length'] = options[:maxlength] || 9
    options[:maxlength] ||= 9
    options[:class] = class_with_default(options[:class], 'form__item__field', 'medium')
    text_field(method, options)
  end

  # Generates two radio buttons for confirmation purposes. "Yes" is the first
  # button, "No" the second. Use this in combination with a Confirm object. The
  # labels default to t('yeah') and t('nope').
  def confirm_radio_buttons(yes_label = nil, no_label = nil, options = {})
    radio_buttons(:value, [[yes_label || t('yeah'), true], [no_label || t('nope'), false]], options)
  end

  def content
    content_tag(:div, class: 'form__item') do
      yield
    end
  end

  def id_expires_at_field(options = {})
    options[:additional_error_methods] ||= ['id_expires_at']
    options[:label] ||= t('valid_until')

    build_item(%i[id_expires_at_day id_expires_at_month id_expires_at_year], options) do |field_options|
      [
        label(:id_expires_at_day, t('day'), class: 'screen-reader'),
        general_text_field(:id_expires_at_day, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--extra-small'), 'data-day' => true, maxlength: 2, placeholder: t('dd'), autocomplete: 'off')),
        label(:id_expires_at_month, t('month'), class: 'screen-reader'),
        general_text_field(:id_expires_at_month, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--extra-small'), 'data-month' => true, maxlength: 2, placeholder: t('mm'), autocomplete: 'off')),
        label(:id_expires_at_year, t('year'), class: 'screen-reader'),
        general_text_field(:id_expires_at_year, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--small'), 'data-year-not-in-past' => true, maxlength: 4, placeholder: t('yyyy'), autocomplete: 'off'))
      ].join.html_safe
    end
  end

  def date_of_birth_field(options = {})
    options[:additional_error_methods] ||= ['geboortedatum']
    options[:example] ||= t('examples.date')
    options[:label] ||= t('date_of_birth')

    build_item(%i[geboortedatum_dag geboortedatum_maand geboortedatum_jaar], options) do |field_options|
      [
        label(:geboortedatum_dag, t('day'), class: 'screen-reader'),
        general_text_field(:geboortedatum_dag, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--extra-small'), 'data-day' => true, maxlength: 2, placeholder: t('dd'), autocomplete: 'off')),
        label(:geboortedatum_maand, t('month'), class: 'screen-reader'),
        general_text_field(:geboortedatum_maand, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--extra-small'), 'data-month' => true, maxlength: 2, placeholder: t('mm'), autocomplete: 'off')),
        label(:geboortedatum_jaar, t('year'), class: 'screen-reader'),
        general_text_field(:geboortedatum_jaar, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--small'), 'data-year-in-past' => true, maxlength: 4, placeholder: t('yyyy'), autocomplete: 'off'))
      ].join.html_safe
    end
  end

  def header(title = nil, options = {})
    options.reverse_merge!(show_fields_required: true)
    options[:class] = class_with_default(options[:class], 'form__header')

    content_tag(:header, class: options[:class]) do
      concat(template.block_with_icon(@object.errors[options[:base]].join.html_safe, :error)) if options.key?(:base) && @object.errors.key?(options[:base])
      concat(content_tag(:div, t('required_fields').html_safe + content_tag(:span, '*', class: 'form__item__label__required'), class: 'form__header__required-fields')) if options[:show_fields_required]
      concat(content_tag(:h2, title)) if title.present?
    end
  end

  def header_rni(title = nil, options = {})
    options.reverse_merge!(show_fields_required: true)
    options[:class] = class_with_default(options[:class], 'form__header')

    content_tag(:header, class: options[:class]) do
      concat(template.block_with_icon(@object.errors[options[:base]].join.html_safe, :error)) if options.key?(:base) && @object.errors.key?(options[:base])
      concat(content_tag(:h2, title)) if title.present?
    end
  end

  def house_number_field(options = {})
    options[:example] ||= t('examples.house_number_and_addition')
    options[:label] ||= t('house_number_and_addition_label')

    build_item(%i[huisnummer huisnummertoevoeging], options) do |field_options|
      [
        label(:huisnummer, t('house_number'), class: 'screen-reader'),
        general_text_field(:huisnummer, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--small'), 'data-house-number' => true, maxlength: 5, autocomplete: 'off')),
        label(:huisnummertoevoeging, t('house_number_addition'), class: 'screen-reader'),
        # addition is never required
        general_text_field(:huisnummertoevoeging, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--small'), 'data-house-number-addition' => true, maxlength: 4, autocomplete: 'off').except('data-required'))
      ].join.html_safe
    end
  end

  def mobile_number_and_spoken_sms(options = {})
    options[:accessibility_information] ||= t('information_boxes.mobile_number')
    options[:information] ||= t('information_boxes.mobile_number')
    options[:box_label] ||= t('i_want_to_receive_spoken_sms_messages')
    options[:label] ||= t('mobile_number')

    build_item(%i[mobiel_nummer gesproken_sms], options) do |field_options, specific_options|
      [
        general_text_field(:mobiel_nummer, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field'), 'data-maximum-length' => 30, 'data-mobile-number' => 'true', autocomplete: 'off')),
        tag(:br),
        original_check_box(:gesproken_sms, field_options.merge(class: merge_classes(field_options[:class], 'form__item__field--box')).except('data-required')),
        label(:gesproken_sms, specific_options[:box_label].html_safe, class: 'form__item__box-label')
      ].compact.join.html_safe
    end
  end

  def postcode_field(options = {})
    options[:class] = merge_classes(options[:class], 'form__item__field--small')
    options[:example] ||= t('examples.postcode')
    options[:label] ||= t('postcode_label')
    options[:maxlength] ||= 7
    options['data-postal-code'] = true
    text_field(:postcode, options)
  end

  def radio_buttons(method, labels_and_values, options = {})
    options[:class] = merge_classes(options[:class], 'form__item__field--box')

    build_item([method], options) do |field_options|
      labels_and_values.map do |label_and_value|
        label_text = label_and_value[0]
        value = label_and_value[1]
        options = label_and_value[2] || {}
        radio_button(method, value, field_options.merge(options)) + label("#{method}_#{value}", label_text, class: 'form__item__box-label')
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
  #                             by default looks up the translated attribute name
  #                             optionally you can supply false to skip rendering the label
  # pattern:                    regular expression that is used for client side validation
  # required:                   set to validate required fields client side; the label will be
  #                             suffixed with an asterisk automatically
  def build_item(methods, options)
    field_options = options.except(:accessibility_information, :additional_error_methods, :box_label, :button, :code, :example, :information, :label, :pattern, :required)
    # although these options are valid HTML 5, we rename them to prevent native browser validations
    field_options['data-pattern']   = options[:pattern] if options[:pattern].present?
    field_options['data-required']  = true if options[:required]

    specific_options = options.slice(:box_label)
    error_methods = (options[:additional_error_methods] || []) + methods

    render_item(accessibility_information: item_accessibility_information(options[:accessibility_information]),
                example: item_example(options[:example]),
                field: yield(field_options, specific_options),
                information: item_information(options[:information]),
                button: item_button(options[:button], options[:button_class]),
                label:  item_label(methods[0], options[:label], options[:required]),
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

    new_class = [default_class, variant].compact.join('--')
    merge_classes(current_class, new_class)
  end

  # Renders the part of the item for the accessibility information. The HTML is as follows:
  #
  # <div class="accessibility__information">
  #   <p>{{text}}</p>
  # </div>
  def item_accessibility_information(text)
    text.blank? ? nil : content_tag(:div, text, class: 'accessibility__information')
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
    error_li_items.empty? ? nil : content_tag(:ul, error_li_items.uniq.join.html_safe, class: 'form__item__errors')
  end

  # Renders the part of the item for the example. The HTML is as follows:
  #
  # <span class="form__item__example">{{text}}</span>
  def item_example(text)
    text.blank? ? nil : content_tag(:span, text.html_safe, class: 'form__item__example')
  end

  # Renders a button next to the rendered_item
  # klass: 'actions__right--button', 'actions__left--button'
  # <input class="{{klass}}" name="commit" type="submit" value="{{text}}" />
  def item_button(button, klass)
    button.blank? ? nil : submit(t(button), class: klass, formnovalidate: true, tabindex: -1)
  end

  # Renders the part of the item for the information. The HTML is as follows:
  #
  # <div class="form__item__information">
  #   <p>{{text}}</p>
  # </div>
  def item_information(text)
    text.blank? ? nil : content_tag(:div, text, class: 'form__item__information')
  end

  # Renders the part of the item for the label. The HTML is as follows:
  #
  # <label for="{{model}}_{{method}}" class="form__item__label">
  #   {{text}}<span class="form__item__label__required">*</span>
  # </label>
  def item_label(method, text, required)
    return nil if text == false
    text = object.class.human_attribute_name(method) if text.blank? && object.present?
    text = t(method) if text.blank?

    asterisk_content = content_tag(:span, '*', class: 'form__item__label__required')
    # add an asterisk placeholder if the text doesn't already include it and the field is required
    text += '[*]' if required && text.exclude?('[*]')
    label(method, text.gsub('[*]', asterisk_content).html_safe, class: 'form__item__label')
  end

  def merge_classes(class1, class2)
    ((class1 || '').split + (class2 || '').split).uniq.join(' ').presence
  end

  # <fieldset class="form__item|form__item--error">
  #   {{label}}
  #   {{accessibility_information}}
  #   {{field}}
  #   {{example}}
  #   {{information}}
  #   {{errors}}
  # </fieldset>
  def render_item(elements)
    content_tag(:fieldset, class: "form__item#{'--error' if elements[:errors_present_flag]}") do
      concat(elements[:label]) if elements[:label].present?
      concat(elements[:accessibility_information]) if elements[:accessibility_information].present?
      concat(elements[:button]) if elements[:button].present?
      concat(elements[:field])
      concat(elements[:example]) if elements[:example].present?
      concat(elements[:information]) if elements[:information].present?
      concat(elements[:errors]) if elements[:errors].present?
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
    if autofocus_on_first_empty_field?
      options[:autofocus] = first_empty_field?(method)
    end
    original_text_field(method, options)
  end

  def autofocus_on_first_empty_field?
    options[:autofocus_on_first_empty_field]
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
