
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

class FourEyesReviewPresenter
  def initialize(model, new_model, action, review, template)
    @current = model
    @new = new_model
    @review = review
    @action = action
    @template = template
  end

  def updated
    @new
  end

  def original
    @current
  end

  def method_missing(*args, &block)
    @template.send(*args, &block)
  end

  def spacer
    render_spacer_row
  end

  def combined_boolean(attributes, options = {})
    # TODO clean up te combined boolean
    label = options[:label]
    empty_label = options[:empty_label]

    first_old = @current.send(attributes[0])
    second_old = @current.send(attributes[1])

    first_new = @new.send(attributes[0])
    second_new = @new.send(attributes[1])

    if first_old == true && second_old == true
      old_value = "#{options[:first]}, #{options[:second]}"
    elsif first_old == false && second_old == true
      old_value = "#{options[:second]}"
    elsif first_old && !second_old
      old_value = "#{options[:first]}"
    else
      old_value = "#{options[:empty]}"
    end

    if first_new == true && second_new == true
      new_value = "#{options[:first]}, #{options[:second]}"
    elsif first_new == false && second_new == true
      new_value = "#{options[:second]}"
    elsif first_new && !second_new
      new_value = "#{options[:first]}"
    else
      new_value = "#{options[:empty]}"
    end

    render_text(old_value, new_value, label)
  end

  def image(attribute)
    current_image = @current.send(attribute)
    new_image = @new.send(attribute)


    current_value = image_tag(current_image) if current_image
    new_value = image_tag(new_image) if new_image

    render_text(current_value, new_value, "")
  end


  def has_one(attribute, options = {value_attribute: :name})
    current_association = @current.send(attribute)
    new_assocation = @new.send(attribute)

    current_value = current_association.send(:try, options[:value_attribute])
    new_value = new_assocation.send(:try, options[:value_attribute])

    if options[:date_format]
      current_value = I18n.l(current_value.to_datetime, format: options[:date_format]) if !current_value.nil?
      new_value = I18n.l(new_value.to_datetime, format: options[:date_format]) if !new_value.nil?
    end

    if options[:true]
      new_value = new_value ? options[:true] : options[:false]
      current_value = current_value ? options[:true] : options[:false]
    end

    label = get_label_for(attribute, options[:label])

    css_class = if options[:compare_values]
      current_value != new_value ? "changed" : "unchanged"
    else
      current_association.try(:id) != new_assocation.try(:id) ? "changed" : "unchanged"
    end

    render_row(label, new_value, label, current_value, css_class)
  end

  def has_changes_on_relation?(relation)
    relation.any? do |record|
      record.marked_for_destruction? || record.new_record? || record.changed_for_autosave?
    end
  end

  def nested_header(label, empty_label, relation)
    css_class = "collection header"

    if has_changes_on_relation?(relation)
      css_class+= " changed"
    else
      css_class+= " unchanged"
    end

    if relation.empty? # No items in collection
      render_row(label, empty_label, label, empty_label, css_class)
    elsif relation.size == relation.select(&:new_record?).size # everything is new
      render_row(label, nil, label, empty_label, css_class)
    elsif relation.size == relation.select(&:marked_for_destruction?).size # everything is destroyed
      render_row(label, empty_label, label, nil, css_class)
    else
      render_row(label, nil, label, nil, css_class)
    end
  end

  def nested_changes_for(attribute, options = {})
    relation = [@new.send(attribute)].flatten.compact # Trick to handle both has one, has many and habtm

    type = attribute.to_s.last == "s" ? :has_many : :has_one

    label = get_label_for(attribute, options[:label])

    empty_label = get_empty_label_for(attribute, options[:empty])

    nested_header(label, empty_label, relation) if options[:heading] != false

    class_string = attribute.to_s.singularize.classify

    if options[:namespace]
      class_string = options[:namespace].to_s.classify + "::" + class_string
    end

    association_class = Object.const_get(class_string) # SectorAuthentication / Webservice::AppConfiguration

    if options[:order]
      relation = relation.sort_by(&options[:order])
    end

    for record in relation
      if record.marked_for_destruction?
        current = record
        new = record
        yield self.class.new(current, new, "destroy", @review, @template)
      elsif record.new_record?
        current = association_class.new
        new = record
        yield self.class.new(current, new, "create", @review, @template)
      else #record.changed_for_autosave? || unchanged
        if type == :has_one
          current = @current.send(attribute) || association_class.new
        else
          current = association_class.find(record.id)
        end
        new = record
        # @action = 'create' or 'update'
        yield self.class.new(current, new, @action, @review, @template)
      end
    end

    # handle has_one associations differently when current = nil or when new = nil
    if type == :has_one && relation.empty?
      current = @current.send(attribute)
      if current # new = nil, current = <Object>
        new = association_class.new
        yield self.class.new(current, new, @action, @review, @template)
      else # new = nil, current = nil
        current = association_class.new
        new = association_class.new
        yield self.class.new(current, new, @action, @review, @template)
      end
    end
  end

  def collection(attribute, options = {})
    order = options[:order] ? options[:order] : :name

    current_ids = @current.send(attribute).order(order).pluck(:id)
    new_ids = @new.send(attribute).collect(&:id)

    klazz = Object.const_get(attribute.to_s.singularize.classify) # Manager / Role / AppVersion / Webservice

    label = get_label_for(attribute, options[:label])

    collection_header(label, options[:empty], current_ids, new_ids)

    for record in klazz.order(order).all
      present_in_old = current_ids.include? record.id
      present_in_new = new_ids.include? record.id

      if present_in_new == true && present_in_old == true
        change = :show
      elsif present_in_old == true && present_in_new == false
        change = :removed
      elsif present_in_old == false && present_in_new == true
        change = :added
      else
        change = :skip
      end

      render_collection_row(record.name, change)
    end
  end

  def text(attribute, options = {})
    if @current.respond_to?("#{attribute}")
      current_value = @current.send(attribute)
      new_value = @new.send(attribute)
    end

    if options[:date_format]
      current_value = I18n.l(current_value.to_datetime, format: options[:date_format]) if !current_value.nil?
      new_value = I18n.l(new_value.to_datetime, format: options[:date_format]) if !new_value.nil?
    end

    if options[:format]
      if options[:format].try(:call)
        current_value = options[:format].call(current_value) if current_value
        new_value = options[:format].call(new_value) if new_value
      else
        current_value = I18n.t(options[:format], :"#{attribute}" => current_value)
        new_value = I18n.t(options[:format], :"#{attribute}" => new_value)
      end
    end

    label = get_label_for(attribute, options[:label])

    if options[:link_when].try(:call)
      current_value = link_to(current_value, @current)
      new_value = link_to(new_value, @new)
    end

    render_text(current_value, new_value, label, options)
  end

  def hashmap(attribute, options = {})
    if @current.respond_to?("#{attribute}")
      current_value = hashmap_to_human_readable(@current.send(attribute))
      new_value = hashmap_to_human_readable(@new.send(attribute))
    end

    label = get_label_for(attribute, options[:label])
    render_text(current_value, new_value, label, options)
  end

  def hashmap_to_human_readable(hashmap)
    # Delete all fully empty items from the hashmap
    hashmap.delete_if { |hash|
      (hash.values.all? { |el| (el.last == false || el.last.blank?) })
    }

    csv_string = hashmap.map{ |hash|
      hash.map{ |property|
        "#{property[0]}: #{property[1]}"
      }.join(", ")
    }.join("\n")

    # Convert newlines into html
    simple_format(csv_string)
  end

  def has_many(attribute, options)
    new_value = ""
    current_value = ""
    if @current.respond_to?("#{attribute}")
      current_value = "<table><thead>#{options[:headers].map {|i| "<th>#{i}</th>"}.join}</thead><tbody>"
      @current.send(attribute).each do |list|
        current_value << "<tr>"
        current_value << options[:fields].map{|i| "<td>"+(list.send(i) || "-")+ "</td>"}.join("")
        current_value << "</tr>"
      end
      current_value <<"</tbody></table>"
    else
      current_value = "Geen " + t("services.menu.#{attribute.to_s}.").downcase
    end

    if @new.respond_to?("#{attribute}")
      new_value = "<table><thead>#{options[:headers].map {|i| "<th>#{i}</th>"}.join}</thead><tbody>"
      @new.send(attribute).each do |list|
        new_value << "<tr>"
        new_value << options[:fields].map{|i| "<td>"+(list.send(i) || "-")+ "</td>"}.join("")
        new_value << "</tr>"
      end
      new_value <<"</tbody></table>"
    else
      new_value = "Geen " + t("services.menu.#{attribute.to_s}").downcase
    end

    options[:html_safe] = true
    render_text(current_value, new_value, options[:label], options)
  end

  private

  def determine_boolean_format(value_to_format, options)
      if value_to_format == true || value_to_format == false
        boolean_format = value_to_format ? options[:true] : options[:false]
      else
        boolean_format = value_to_format == "0" ? options[:false] : options[:true]
      end
      return boolean_format
  end

  def get_label_for(attribute, custom = nil)
    custom ? custom : I18n.t("model_attributes.#{@current.class.name.underscore}.#{attribute.to_s}")
  end

  def get_empty_label_for(attribute, custom = nil)
    custom ? custom : I18n.t("model_attributes.#{@current.class.name.underscore}.no_#{attribute.to_s}")
  end

  def render_collection_row(name, change, from_empty = nil)
    case change
    when :skip
      return
    when :show
      unchanged_row(name)
    when :removed
      removed_row(name)
    when :added
      added_row(name)
    end
  end

  def render_text(current_value, new_value, label, options = {})
    css_class = new_value.to_s != current_value.to_s ? "changed" : "unchanged"

    # Boolean formatting option
    if options[:true]
        current_value = determine_boolean_format(current_value, options)
        new_value = determine_boolean_format(new_value, options)
        css_class = new_value.to_s != current_value.to_s ? "changed" : "unchanged"
    end

    if options[:download_link]
      new_data_uri = @new.send(options[:download_link]) # Required to use data-uri attribute, href attribute seems to be limited in length in IE
      current_data_uri = @current.send(options[:download_link])
      new_file_name = @new.filename
      current_file_name = @new.filename
      current_value = link_to(current_value, current_data_uri, download: current_file_name, :"data-filename" => current_file_name, :"data-uri" => current_data_uri) if current_value
      new_value = link_to(new_value, new_data_uri, download: new_file_name, :"data-filename" => new_file_name, :"data-uri" => new_data_uri) if new_value
    end

    if options[:empty]
      new_value = new_value.blank? ? options[:empty] : new_value
      current_value = current_value.blank? ? options[:empty] : current_value
    end

    if options[:html_safe]
      render_row(label, new_value.html_safe, label, current_value.html_safe, css_class)
    else
      render_row(label, new_value, label, current_value, css_class)
    end
  end

  def collection_header(label, empty_label, current_ids, new_ids)
    css_class = "collection header"

    if current_ids != new_ids
      css_class+= " changed"
    else
      css_class+= " unchanged"
    end

    if current_ids.empty? && new_ids.empty?
      render_row(label, empty_label, label, empty_label, css_class)
    elsif current_ids.empty?
      render_row(label, nil, label, empty_label, css_class)
    elsif new_ids.empty?
      render_row(label, empty_label, label, nil, css_class)
    else
      render_row(label, nil, label, nil, css_class)
    end
  end

  def render_spacer_row
    haml_tag :tr, :class => "spacer" do
      haml_tag :td, "&nbsp;".html_safe, colspan: 4
    end
  end

  def render_row(*args, css_class)
    haml_tag :tr, :class => "#{css_class}" do
      if show_left_columns # on update or create, show the left column
        haml_tag :td, args[0], width: "15%", class: 'label'
        haml_tag :td, args[1], width: "35%", class: 'value'
      else
        haml_tag :td, nil, width: "15%", class: 'label'
        haml_tag :td, nil, width: "35%", class: 'value'
      end
      if show_right_columns # update or destroy, show the right column
        haml_tag :td, args[2], width: "15%", class: 'label'
        haml_tag :td, args[3], width: "35%", class: 'value'
      else
        haml_tag :td, nil, width: "15%", class: 'label'
        haml_tag :td, nil, width: "35%", class: 'value'
      end
    end
  end

  def unchanged_row(name)
    render_row(nil, name, nil, name, 'collection unchanged')
  end

  def removed_row(name)
    render_row(nil, nil, nil, name, "collection removed")
  end

  def added_row(name)
    render_row(nil, name, nil, nil, "collection added")
  end

  def show_left_columns
    ['update', 'create'].include?(@action)
  end

  def show_right_columns
    ['update', 'destroy'].include?(@action)
  end
end
