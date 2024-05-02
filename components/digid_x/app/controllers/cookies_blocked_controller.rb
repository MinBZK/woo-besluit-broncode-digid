
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

class CookiesBlockedController < ApplicationController

  ALLOWED_HOST_REGEX = /^#{APP_CONFIG[:protocol]}:\/\/(#{APP_CONFIG['hosts']['digid']}|.+\.#{APP_CONFIG['hosts']['digid']})$/ # => /^https:\/\/(digid.nl|.+\.digid.nl)$/

  def index
    if cookies.first.present?

      return render_not_found unless params[:url]&.match(ALLOWED_HOST_REGEX)

      redirect_to(params[:url])
    else
      logcode = case params[:process]
        when "registrations" then 1437
        when "activations" then 1438
        when "recover_accounts/request_recover_passwords", "recover_accounts/recover_passwords" then 1439
        when "authentications", "saml/identity_provider" then 1440
        when "my_digid" then 1441
      end
      Log.instrument(logcode) if logcode
      flash.now[:notice] = t("cookies_blocked").html_safe
      render_simple_message(continue: APP_CONFIG["urls"]["external"]["digid_home"])
    end
  rescue URI::InvalidURIError
    render_not_found
  end
end
