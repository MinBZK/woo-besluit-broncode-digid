
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

module ApplicationHelper # rubocop:disable Metrics/ModuleLength

  def background_colors
    case APP_CONFIG["admin_theme"] || Rails.env
    when /^acc/
      { 'headers' => '#01689B', 'background' => '#CCE0F1' }
    when /^preprod/
      { 'headers' => '#39870C', 'background' => '#C3DBB6' }
    else
      { 'headers' => APP_CONFIG["admin_header"] || '#E7792C', 'background' => APP_CONFIG["admin_color"] || '#EEA160' }
    end
  end

  def orange(text)
    content_tag :span, text, class: 'orange'
  end

  def new_link(*args)
    icon_link(*args.insert(0, 'new_link'))
  end

  def edit_link(*args)
    icon_link(*args.insert(0, 'edit_link'))
  end

  def delete_link(*args)
    icon_link(*args.insert(0, 'delete_link'))
  end

  def show_link(*args)
    icon_link(*args.insert(0, 'show_link'))
  end

  # used in the shared/_header_actions partial
  def index_path_for_model_instance(model)
    url_for(controller: model.class.name.pluralize.underscore, action: :index)
  end

  # used in the shared/_header_actions partial
  def edit_path_for_model_instance(model)
    url_for(controller: model.class.name.pluralize.underscore, action: :edit, id: model.id)
  end

  def icon_link(*args)
    default_css_class = "icon_link #{args[0]}"
    name              = args[1]
    options           = args[2] || {}
    html_options      = args[3]

    if html_options.present?
      css_class = html_options.delete(:class) || ''
      html_options[:class] = ("#{default_css_class} " + css_class).strip
    else
      html_options = { class: default_css_class }
    end

    html_options = convert_options_to_data_attributes(options, html_options)
    url = url_for(options)

    href = html_options['href']
    tag_options = tag_builder.tag_options(html_options)

    href_attr = "href=\"#{html_escape(url)}\"" unless href
    "<a #{href_attr}#{tag_options}><span class=\"icon\"></span><span class=\"text\">#{html_escape(name || url)}</span></a>".html_safe
  end

  def submit_button(name, id = nil)
    return submit_tag(name, class: 'ui-button ui-widget ui-state-default ui-corner-all', id: id) if id.present?
    submit_tag(name, class: 'ui-button ui-widget ui-state-default ui-corner-all')
  end

  def sector_codes(sector_codes)
    capture_haml do
      @sectors ||= Sector.all
      sector_codes.each do |sector_code|
        sector_name = @sectors.find { |s| s.id == sector_code.sector_id }.name
        haml_tag :div, "#{sector_code.sectoraalnummer} (#{sector_name})"
      end
    end
  end

  # renders the table footer with left a per_page select box and right the pagination
  def table_footer(colspan, &block)
    capture_haml do
      haml_tag :tfoot do
        haml_tag :tr do
          haml_tag :td, colspan: colspan do
            haml_concat block_given? ? capture(&block) : ''
          end
        end
      end
    end
  end

  def human_boolean(abool)
    t(abool == true ? :human_true : :human_false)
  end

  def sso_domain_options(selected_domain = nil)
    options_for_select(SsoDomain.all.map { |d| [d.name, d.id] }, selected_domain)
  end

  def sector_options(webservice = nil)
    # TODO does not handle webservice nil case
    sectors = if webservice.id && webservice.sector_authentications.any?
                Sector.where('id NOT IN (?)', webservice.sector_authentications.map(&:sector_id))
              else
                Sector.all
              end
    options_for_select(sectors.map { |s| [s.name, s.id] })
  end

  def organization_options(selected_organization = nil)
    options_for_select(Organization.all.map { |o| [o.name, o.id] }, selected_organization)
  end

  def dc_organization_options(selected_organization = nil)
    options_for_select(Dc::Organization.list.map(&:values).map(&:reverse), selected_organization)
  end

  def dc_organization_roles_options(selected_organization = 1, selected_organization_role = nil)
    options_for_select(selected_organization && Dc::Organization.find(selected_organization)&.organization_roles&.map { |o| [o.type, o.id] } || [], selected_organization_role)
  end

  def dc_connection_options(selected_connection = nil)
    options_for_select(Dc::Connection.list.map(&:values).map(&:reverse), selected_connection)
  end

  def dc_connection_entity_ids(selected_connection = nil, selected_entity_id = nil)
    connection = Dc::Connection.find(selected_connection)
    options_for_select((connection.entity_ids - [connection.entity_id]).map { |o| [o, o] }, selected_entity_id)
  end

  def can_link_to(klass, block)
    return unless can? :read, klass
    block
  end

  # Provides a bordered column box.
  # set :title to the wanted header string and use a block
  # for the given content to display inside the box.
  # use border: true for a bordered column box
  def column_box(options = {}, &block)
    options[:html]         = {}
    options[:html][:id]    = options[:id] if options[:id].present?
    options[:html][:class] = options[:class] if options[:class].present?
    logger.debug(options.inspect)

    capture_haml do
      haml_tag :div, options[:html] do
        haml_tag('strong.clearfix', options[:title]) unless options[:title].blank?
        haml_tag (options[:border] == true ? '.form_wrapper' : 'div').to_s, capture_haml(&block)
      end
    end
  end

  ## sorting
  # def sortable(column, title = nil)
  #  title ||= column.titleize
  #  css_class = (column == sort_column) ? "current #{sort_direction}" : nil
  #  direction = (column == params[:sort] && params[:direction] == "asc") ? "desc" : "asc"
  #  link_to((title + '<div class="icon"></div>').html_safe, { sort: column, direction: direction }, { class: css_class })
  # end

  def present(object, klass = nil)
    klass ||= "#{object.class}Presenter".constantize
    presenter = klass.new(object, self)
    yield presenter if block_given?
    presenter
  end

  def can_fraud_gba_helper
    can?(:create_gba, FraudReport) && can?(:gba_request, Account)
  end

  def show?
    controller.action_name == "show"
  end
end
