
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

# for all things public under root
class HomeController < ApplicationController
  # this one must go first so it will catch all errors including those in other filters
  prepend_around_action :disable_exception_notifier
  before_action :check_cookie_blocked?

  # homepage
  def index
    @page_title = t("titles.home")
  end

  def api_docs
    api_docs = YAML.load_file("doc/api.yaml")

    api_docs["servers"] = [{url: "#{APP_CONFIG['protocol']}://#{APP_CONFIG["hosts"]["digid"]}", description: Rails.env}]
    api_docs["info"]["description"] = Rails.root.join("doc/api.md").read
    api_docs["components"]["parameters"]["appVersion"]["example"] = latest_valid_version
    api_docs["components"]["parameters"]["kioskVersion"]["example"] = latest_valid_version("kiosk")
    api_docs["components"]["parameters"]["widCheckerVersion"]["example"] = latest_valid_version("idCheckerAndroid")

    render json: api_docs
  end

  def testbeeld
    @page_name = params[:page_name] || "D1"
    @news_items = news_items("Mijn DigiD")
    @disable_autofocus = params[:autofocus].present?
  end

  def robots
  end

  private

  def check_cookie_blocked?
    super if params[:check_cookie].present?
  end

  def latest_valid_version(app_type = nil)
    if app_type.present?
      AppVersion.where("not_valid_before <= ?", Time.zone.today)
                .where("kill_app_on_or_after IS NULL OR kill_app_on_or_after >= ?", Time.zone.today)
                .where("not_valid_on_or_after IS NULL OR not_valid_on_or_after >= ?", Time.zone.today)
                .where("operating_system = ?", app_type)
                .maximum("version")
    else
      AppVersion.where("not_valid_before <= ?", Time.zone.today)
                .where("kill_app_on_or_after IS NULL OR kill_app_on_or_after >= ?", Time.zone.today)
                .where("not_valid_on_or_after IS NULL OR not_valid_on_or_after >= ?", Time.zone.today)
                .maximum("version")
    end
  end

  def disable_exception_notifier
    yield
  rescue => e
    logger.error e.message
  end
end
