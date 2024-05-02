
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

class AppLinkController < ApplicationController
  include AppLinkHelper
  include ApplicationHelper
  include AuthenticationSession

  helper_method :wid_url?

  before_action :check_host, only: [:apple_app_site_association, :android_asset_links]

  def download
    unless mobile_browser?
      render_not_found
      return
    end

    if (ios_browser? || params["app-app"])&& ::Configuration.get_boolean("URL-schemes_als_backup_voor_universal_links_iOS")
      if app_app_guid = params["app-app"] && params["app-app"].match(/^[a-zA-Z0-9+\/]+={0,2}$/)
        @open_app_url = app_to_app_provisioning_uri(wid_url? ? "wid" : "auth", app_app_guid)
      else 
        @open_app_url = digid_app_provisioning_uri(wid_url? ? "wid" : "auth", session[:app_session_id] || params[:data]&.match(/^.*app_session_id=(.*?)&.+/).try(:[], 1), format: :mobile)
      end
    end

    @page_name = "C8"
  end

  def app_not_installed
    flash.now[:notice] = t("is_digid_app_not_installed_notice")

    render_message(
      button_label: :download_the_app,
      button_to: digid_app_store_link,
      button_to_options: { method: :get },
      cancel_to: login_url
    )
  end

  def apple_app_site_association
    expires_in APP_CONFIG["app_link_cache_ttl"].seconds, public: true
    send_file Rails.root.join("config", "apple_app_site_association_#{app_host}.json").to_s, filename: "apple-app-site-association", type: "application/json"
  end

  def android_asset_links
    expires_in APP_CONFIG["app_link_cache_ttl"].seconds, public: true
    send_file Rails.root.join("config", "android_asset_links.json").to_s, filename: "assetlinks.json", type: "application/json"
  end

  private

  def check_host
    # Mijn is only for test app for MCC for App-to-App
    return if APP_CONFIG["dot_environment"] || app_host != "mijn"
    render json: {}
  end

  def app_host
    @app_host ||= case request.host
                  when /^mijn/
                    "mijn"
                  when /^app/
                    "app"
                  else
                    "kern"
                  end
  end
end
