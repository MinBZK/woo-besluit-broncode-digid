
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

class AppRequest
  HEADERS = [
    "API-Version",
    "OS-Type",
    "App-Version",
    "OS-Version",
    "Release-Type"
  ].freeze

  attr_reader :app_version

  def initialize(headers, request_type, log_statistic, app_auth_log_details)
    # We cannot modify method argument (can be real header from request)
    @headers = HEADERS.each_with_object({}) { |key,items| items[key] = headers[key] }
    @headers["Release-Type"] = "Productie" if @headers["Release-Type"].blank?
    @request_type = request_type
    @log_statistic = log_statistic
    @app_version = AppVersion.find_by_request(self)
    @app_auth_log_details = app_auth_log_details
  end

  def allowed?
    app_version && app_version.allowed?
  end

  def newest?
    app_version && app_version.newest?
  end

  def operating_system
    @headers["OS-Type"]
  end

  def os_version
    @headers["OS-Version"]
  end

  def version
    @headers["App-Version"]
  end

  def release_type
    @headers["Release-Type"]
  end

  def api_version
    @headers["API-Version"]
  end

  def check_headers
    query = []
    missing = []
    HEADERS.each do |h|
      if @headers[h].present?
        query << "#{h}=#{@headers[h]}"
      elsif h != "Release-Type"
        missing << h
      end
    end

    missing.empty?.tap do |completed|
      if completed
        Log.instrument({
          "Kiosk" => "1164",
          "idCheckerAndroid" => "1317"
        }[operating_system] || "734", { hidden: true, request_type: @request_type, statistics: query * "&"}.merge(@app_auth_log_details)) if @log_statistic
      else
        Log.instrument({
          "Kiosk" => "1151",
          "idCheckerAndroid" => "1316"
        }[operating_system] || "742", { request_type: @request_type, headers: missing * ", "})
      end
    end
  end

  def log_app_response
    log_key = app_version ? app_version.status : :unknown_version

    log_codes = {
      digid_kiosk: { force_update: "1145", kill_app: "1146", unknown_version: "1147" },
      id_checker: { force_update: "1313", kill_app: "1314", unknown_version: "1315"},
      digid_app: { force_update: "770", kill_app: "771", unknown_version: "772" }
    }
    do_log log_codes[set_app_type][log_key] if log_codes[set_app_type][log_key]
  end

  def log_update_warning
    do_log case set_app_type
      when :digid_kiosk then "1144"
      when :id_checker then "1312"
      else "769"
    end
    
  end

  def version_response
    app_type = set_app_type("version")
    if app_version
      {
        action: app_version.status.to_s,
        message: I18n.t(app_version.status.to_s, scope: app_type)
      }
    else
      {
        action: :kill_app,
        message: I18n.t("#{app_type}.unknown_version")
      }
    end
  end

  private

  def do_log log_code
    if log_code
      Log.instrument(
        log_code,
        { 
          request_type: @request_type,
          operating_system: operating_system,
          version: version,
          release_type: release_type,
          hidden: true
        }.merge(@app_auth_log_details)
      )
    end
  end

  def set_app_type(variety = nil)
    app_type = case operating_system
               when "Kiosk"
                 variety == "version" ? :kiosk_version : :digid_kiosk
               when "idCheckerAndroid"
                 variety == "version" ? :id_checker_version : :id_checker
               else
                 variety == "version" ? :app_version : :digid_app
               end
  end
end
