
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

module MyDigid
  class CancelTwoFactorAuthenticationsController < BaseController
    # D20: "Mijn DigiD | Opheffen sms-controle" (http GET)
    def show
      if current_account.sms_tools.active?
        Log.instrument("169", account_id: current_account.id)
        @page_name = "D20"
      else
        redirect_to my_digid_url
      end
    end

    # D20: "Mijn DigiD | Opheffen sms-controle" (http POST)
    def destroy
      if password_verification.valid?
        flash[:notice] = t("two_factor_authentication_has_been_cancelled")
        Log.instrument("171", account_id: current_account.id)
        current_account.remove_mobiel
        current_account.reset_zekerheidsniveau!
        redirect_to my_digid_url
      else
        unless current_session_is_being_expired_because_account_blocked?
          @page_name = "D20"
          render :show
        end
      end
    end
  end
end
