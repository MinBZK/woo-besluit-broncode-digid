
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
  include LoadBalancerCookieHelper

  def account_for_saml_subject(saml_subject)
    /s(?<sectorcode>\d*):(?<sectoraalnummer>\d*)/ =~ saml_subject # sets sectorcode and sectoraalnummer
    sector_id = Sector.get(sectorcode)
    @account_for_saml_subject ||= Account.joins(:sectorcodes).where("sectorcodes.sector_id = ? and sectorcodes.sectoraalnummer = ?", sector_id, sectoraalnummer).first
  end

  def current_provider
    return if session["saml.provider_id"].blank?

    @current_provider ||= SamlProvider.find(session["saml.provider_id"])
  end

  def current_federation
    return unless current_provider.present? && session["saml.session_key"].present?

    @current_federation ||= Saml::Federation.find_by(session_key: session["saml.session_key"], federation_name: current_provider.federation_name)
  end

  def action_button(title, action, options = {})
    button = Button.new(action: title, arrow: options[:arrow], inverted_arrow: options[:inverted_arrow], inverse: options[:inverse])
    options[:class] = merge_classes(options[:class], button.css_class)

    button_to(action, options){ t(title) }
  end

  def action_link(title, action, options = {})
    button = Button.new(action: title, arrow: options[:arrow], inverse: options[:inverse])
    options[:class] = merge_classes(options[:class], button.css_class(options[:css_type] || :link))

    link_to(t(title), action, options)
  end

  def cancel_button(url, options = {})
    title = options[:action] || :cancel
    action_link_with_role_button(title, url, options)
  end

  def previous_button(url, options = {})
    title = options[:action] || :previous
    action_link_with_role_button(title, url, options)
  end

  def not_now_continue_log_in_button(url, options = {})
    title = options[:action] || :not_now_continue_log_in
    action_link_with_role_button(title, url, options)
  end

  def action_link_with_role_button(title, url, options = {})
    options[:css_type] = :button
    options[:role] = :button
    action_link(title, url, options)
  end

  def action_submit(action, options = {})
    button = Button.new(action: action, arrow: options[:arrow], inverse: options[:inverse])

    options[:class] = merge_classes(options[:class], button.css_class)
    options[:formnovalidate] = true if options.delete(:validate) == false
    button_tag(t(action), options)
  end

  def actions
    content_tag(:div, class: "actions") do
      yield
    end
  end

  def heading_with_icon(line1, line2 = nil)
    options = {}
    options[:class] = "h2"
    content_tag(:div, class: "heading_with_icon") do
      if line2.nil?
        # Only text1 is specified: Show it next to DigiD logo, starting at bottom right of logo
        h1_content = <<~HTML
          <br />#{line1}
        HTML
        options[:class] = options[:class] + " single"
        options[:class] = options[:class] + " multiline" if line1.length > 40
      else
        options[:class] = options[:class] + " multiline" if line2.length > 40
        h1_content = <<~HTML
          <strong>#{line1}</strong> <br /> #{line2}
        HTML
      end

      content_tag(:h1, options) do
        concat(image_tag("digid_eo_rgb.svg", alt: t("vignet_digid"), class: "digid-logo"))
        concat(h1_content.html_safe)
      end

    end
  end

  def active_link_to(name = nil, options = nil, html_options = nil, &block)
    active_paths = ([options] << html_options.delete(:active_paths)).flatten
    html_options[:class] = "#{html_options[:class]} #{html_options[:class]}--active" if active_paths.include?(request.env["PATH_INFO"])
    link_to(name, options, html_options, &block)
  end

  def block_with_icon(content = nil, variant = :information, options = {})
    return unless block_given? || content.present?
    options[:class] = merge_classes(options[:class], "block-with-icon--#{variant}")
    options[:"aria-live"] = (variant == :error && content.exclude?("counter")) ? "assertive" : "polite"
    content = t("something_went_wrong").html_safe + content if (variant == :error && content.exclude?("counter"))
    content_tag(:div, options) do
      block_given? ? yield : content_tag(:p, content)
    end
  end

  def block_without_icon(content = nil, options = {})
    options[:class] = merge_classes(options[:class], "block-without-icon")
    content_tag(:div, options) do
      block_given? ? yield : content_tag(:p, content)
    end
  end

  def markdown(text)
    Redcarpet::Markdown.new(Redcarpet::Render::HTML, safe_links_only: true, escape_html: true).render(text).html_safe if text.present?
  end

  def show_navigation?
    session["session"] == "mijn_digid" && %w[D1 D2 D4].include?(@page_name)
  end

  def my_digid_index?
    # Used for pages which have links on the right side (different break points for the logo on the left side)
    %w[D1 D4 D2].include?(@page_name)
  end

  # like form_for() but using ExtendedFormBuilder
  def extended_form_for(record_or_name_or_array, *args, &block)
    options = args.extract_options!
    form_for(record_or_name_or_array, *(args << options.merge(builder: ExtendedFormBuilder)), &block)
  end

  def external_link_to(body, url, html_options = {}, &block)
    link_to(body, url, html_options.merge(target: "_blank"), &block) + " [#{t('opens_in_a_new_window')}]"
  end

  def rijksoverheid_link_to
    external_link_to(t("rijksoverheid"), t("rijksoverheid_link"))
  end

  def mijnoverheid_link_to
    external_link_to(t("mijnoverheid"), t("mijnoverheid_link"))
  end

  def flash_alert
    block_with_icon(flash[:alert], :error) if flash[:alert].present?
  end

  def flash_notice_for_sms
    message = if @auth&.account_or_ghost&.login_level_two_factor? || @account&.login_level_two_factor?
                t("your_settings_require_always_sms_verification_to_login") 
              else
                nil
              end
    block_with_icon(flash[:notice] || message, :information) if flash[:notice].present? || message.present?
  end

  def flash_notice
    block_with_icon(flash[:notice].html_safe, :information) if flash[:notice].present?
  end

  # small wrapper for image_tag that sets the title equal to the alt
  def image(source, options = {})
    options[:alt] ||= ""
    options[:title] ||= options[:alt]
    image_tag(source, options)
  end

  def link_back_to(url)
    uri = URI(url)
    link_to(t("back_to_url", url: uri.host), uri.to_s)
  end

  def inline_qr_tag(data, alt = t("qr_code_alt"), id: nil, klass: "digid-qr-code-logo")
    fallback = Rails.application.config.performance_mode ? "" : image_tag(qr_code_url(data: data), alt: alt)
    content_tag(:div, class: "qr_code_block") do
      content_tag(:div, class: "qr_code", id: id, data: { code: data }, text: data) do
        image("digid_eo_rgb.svg", alt: nil, class: klass) + content_tag(:noscript, fallback)
      end
    end
  end

  def show_extend_session_popup?
    @extend_session_popup && !wrapped_session.timed_out? && wrapped_session.keep_alive_retries_left? && wrapped_session.timeout_warning_delay.present?
  end

  def show_absolute_session_popup?
    wrapped_session.absolute_timeout_warning_delay.present?
  end

  def steps(current, steps, title = nil)
    result = content_tag(:p, t("steps", current_step: current, total_steps: steps).html_safe, class: "simple-step-counter")
    result += content_tag(:h2, title, class: "orange_heading") if title.present?
    result
  end

  # digid-app-wid => for wid scanning
  # digid-app-act => for activation
  # digid-app-auth => for authentication (sign-in)
  def digid_app_provisioning_uri(action, app_session_id, format: :mobile)
    prefix = if format == :mobile
               "digid-app-#{action}://"
             else
               "digid-app-#{action}:"
             end

    uri =  "#{prefix}app_session_id=#{app_session_id}&lb=#{load_balancer_cookie}&at=#{epoch}"

    if ios_browser?
      uri += "&browser=#{URI.encode_www_form_component(browser_name)}"
    end

    if Rails.env.productie?
      uri
    elsif Rails.env.development? || Rails.env.test?
      uri += "&host=#{host}"
    else
      uri += "&host=#{APP_CONFIG["hosts"]["digid"]}"
    end
  end

  def app_to_app_provisioning_uri(action, app_app_guid)
    "digid-app-#{action}://app-app=#{app_app_guid}"
  end

  def epoch
    Time.zone.now.to_i
  end

  def host
    request.host.eql?("SSSSSSSSSSSSSSS") || request.host.eql?("SSSSSSSSSS") ? "digid.#{private_ip}SSSSSSS" : request.host
  end

  def private_ip
    ENV["PRIVATE_IP"] || Socket.ip_address_list.detect(&:ipv4_private?)&.ip_address
  end

  def sort_authenticators(authenticators)
    sorting_order = %w[app_authenticator sms_tool rijbewijs identiteitskaart]
    canonical_two_factors = authenticators.map(&:canonical_type)
    canonical_two_factors.sort_by { |f| sorting_order.index(f) }
  end

  def format_authenticators(authenticators, except: nil, use_uw: false)
    scope = if session[:session].eql?("pilot_login_preferences")
              (use_uw ? "authenticators.hybrid" : "authenticators")
            else
              (use_uw ? "authenticators.human" : "authenticators")
            end

    authenticators = sort_authenticators(authenticators) - [except].flatten
    authenticators.uniq.map { |two_factor| t(two_factor, scope: scope) }
                  .to_sentence(two_words_connector: " " + t("or") + " ", last_word_connector: " " + t("or") + " ")
  end

  def midden_authenticators(except: nil, use_uw: false)
    format_authenticators(current_account.active_two_factor_authenticators, except: except, use_uw: use_uw)
  end

  def all_authenticators(except: nil, use_uw: false, presentable_driving_licence: [], presentable_identity_card: [])
    format_authenticators(current_account.active_two_factor_authenticators, except: except, use_uw: use_uw)
  end

  def icon(file_name, human_name: nil, css_class: nil, style: nil, hidden: false)
    icon_css_class = ("icon " + file_name).parameterize
    default_human_name = file_name.gsub(/\d+(\s|px)/, "").strip + " icoon"
    human_name = human_name.presence || default_human_name
    css_classes = icon_css_class
    css_classes += " #{css_class}" if css_class.present?

    elements = ["class=\"#{css_classes}\""]
    elements << "style=\"#{style}\"" if style.present?
    elements << "aria-hidden='true'" if hidden
    elements << "aria-label=\"#{human_name}\""

    ("<i #{elements.join(' ')}></i>").html_safe
  end

  def device_dependent_image_tag(asset_name, format: :svg)
    file_name = mobile_browser? ? asset_name + "-mobile" : asset_name
    file_name_with_extension = file_name + "." + format.to_s
    image_tag(file_name_with_extension)
  end

  def digid_website_link
    if I18n.locale != I18n.default_locale
      APP_CONFIG["urls"]["external"]["digid_home"] + "/" + I18n.locale.to_s
    else
      APP_CONFIG["urls"]["external"]["digid_home"]
    end
  end

  def webservice_name(service = nil)
    session.dig(:authentication, :webservice_name) || (service || webservice )&.name
  end

  # e.g. BSN, driving license number
  def mask_personal_number(personal_number)
    return unless personal_number.present?

    return personal_number if (personal_number.length <= 4)

    masked_personal_number = "X" * (personal_number.length - 4) + personal_number[-4,4]
  end

  private

  def create_steps(current, titles = [])
    content_tag(:div, class: "steps") do
      content_tag(:ol, class: "steps-container") do
        titles.length.times do |step|
          css_class = if step < (current - 2) then String.new("step step-finished")
                      elsif step == (current - 2) then String.new("step step-previous")
                      elsif step == (current - 1) then String.new("step step-current")
                      else String.new("step step-unfinished")
                      end
          css_class << "-last" if step == titles.length - 1
          concat(content_tag(:li, titles[step].to_s.html_safe, class: css_class))
        end
      end
    end
  end

  def merge_classes(*classes)
    classes.compact.map(&:split).flatten.uniq.join(" ").presence
  end
end
