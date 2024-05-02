
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

# Also see https://github.com/PPPPPP/browser/blob/master/lib/browser/platform/ios.rb
module AppLinkHelper
  def browser
    @browser ||= Browser.new(request.user_agent)
  end

  def android_browser?
    browser.platform.android?
  end

  def ios_browser?
    browser.platform.ios?
  end

  def ios_device_without_nfc?
    ios_browser? && browser.platform.version.to_i < 13
  end

  def ios_device_with_nfc?
    ios_browser? && browser.platform.version.to_i >= 13
  end

  def mobile_browser?
    android_browser? || ios_browser?
  end

  def browser_supported_for_web_to_app?
    return true if android_browser?
    ios_browser? && ["Firefox", "Safari", "Chrome", "Microsoft Edge"].include?(browser_name)
  end

  def third_party_app_from_user_agent
    return @third_party_app if @third_party_app.present?

    user_agent = request.user_agent.match(/DigiDTPA_\(([^\)]+)\)/)
    @third_party_app = ThirdPartyApp.where(user_agent: user_agent[1]).first if user_agent
  end

  def formatted_return_url(return_url, action = nil)
    prefix = if third_party_app_from_user_agent&.return_url.present? && return_url.present?
      Log.instrument("1570", user_agent: third_party_app_from_user_agent.user_agent, human_process: t("process_names.log.#{action || "log_in_with_digid_app"}", wid_type: t("document.id_document", locale: :nl)))
      third_party_app_from_user_agent.return_url
    elsif ockto_browser? && return_url.present?
      APP_CONFIG["ockto_universal_link"]
    end

    prefix ? prefix + "?return_url=" + URI.encode_www_form_component(return_url) : return_url
  end

  def force_username_password?
    /DigiDPWD/.match?(request.user_agent)
  end

  def custom_browser_name_version
     name_version = /(\([^\s]*\)) DigiDPWD/.match(request.user_agent)
     name_version ? name_version[1] : "()"
  end

  def ockto_browser?
    /#{APP_CONFIG["ockto_user_agent"]}/.match?(request.user_agent)
  end

  def use_app_link?
    return false unless Configuration.get_boolean("digid_app_android_app_link") && browser.chrome? && android_browser?
    browser.platform.version.to_i >= 6
  end

  def browser_name
    # catch exceptions here
    if browser.ua =~ /EdgiOS/ # TODO: remove this when it is fixed in https://github.com/PPPPPP/browser/issues/454
      "Microsoft Edge"
    elsif browser.ua =~ /Brave/ # Only detects Brave < v0.9 || TODO: remove this when it is fixed in https://github.com/PPPPPP/browser/issues/455
      "Brave"
    elsif browser.ua =~ /OPiOS/
      "Opera"
    else
      browser.name
    end
  end

  def digid_app_link(data)
    if ios_browser?
      # ios browser is now set in the data property. see digid_app_provisioning_uri(). Browser property here is just for backwards compatibility
      digid_app_url(data: data, browser: browser_name)
    elsif use_app_link?
      digid_app_url(data: data)
    else
      package = Configuration.get_string("digid_app_android_package")
      "intent://#Intent;scheme=digid;package=#{package};S.data=#{data};end"
    end
  end

  def login_url
    "#{request.scheme}://#{APP_CONFIG["hosts"]["digid"]}/inloggen"
  end

  def wid_url?
    params[:data]&.starts_with?("digid-app-wid")
  end

  def digid_app_store_link
    ios_browser? ? ios_store_url : android_store_url
  end

  def desktop_store_info_link
    "https://www.digid.nl/over-digid/app"
  end

  def desktop_id_check_info_link
    "https://www.digid.nl/inlogmethodes/id-check"
  end

  def android_store_url
    Configuration.get_string("digid_app_android_store_url")
  end

  def ios_store_url
    Configuration.get_string("digid_app_ios_store_url")
  end
end
