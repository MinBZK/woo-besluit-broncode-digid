
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

# Restart app if you change this builder (is default)

class FormBuilder < ActionView::Helpers::FormBuilder
  # Helper function to create has_many checkboxes for a has many relation
  # You can provide options if you want to filter or use a different order

  def many_check_boxes(method, options=nil)
    param_ids = "#{method.to_s.singularize}_ids"
    html_name = "#{object_name}[#{param_ids}][]"
    html_id_template = "#{object_name}_#{param_ids}_" + '%d'

    # Get selected ids
    if object.try(:cached_habtm_values) && object.cached_habtm_values[param_ids]
      selected_ids = object.get_cached_collection(method).to_set
    else
      selected_ids = object.send(param_ids).to_set
    end

    output = @template.hidden_field_tag(html_name)
    (options || object.class.reflections[method.to_s].klass.all).each do |option|
      is_selected = selected_ids.include?(option.id)
      html_id = sprintf(html_id_template, option.id)

      checkbox_options = {id: html_id}
      if block_given? && yield(option)
        checkbox_options[:disabled] = true
        # Add hidden field with same value because disabled checkboxes are not sent
        output << @template.hidden_field_tag(html_name, option.id) if is_selected
      end

      output << @template.check_box_tag(html_name, option.id, is_selected, checkbox_options)
      output << ' '
      output << @template.label_tag(html_id, option.to_s, class: 'inline_label')
      output << @template.tag('br')
    end
    output
  end
end
