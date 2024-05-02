
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
  module Account
    class PreferredLanguagesController < BaseController
      before_action :render_not_found_if_account_deceased

      def show
        @page_name = "D66"
        Log.instrument("1360", account_id: current_account.id)
      end

      def update
        Log.instrument("1362", account_id: current_account.id)
        if current_account.update locale: params[:account][:locale]
          Log.instrument("1363", account_id: current_account.id)
          flash[:notice] = t("preferred_language_success")
        else
          Log.instrument("1364", account_id: current_account.id)
          flash[:notice] = t("preferred_language_fail")
        end
        redirect_to my_digid_url
      end

      def cancel
        Log.instrument("1361", account_id: current_account.id)
        flash[:notice] = t("preferred_language_cancel")
        redirect_to my_digid_url
      end
    end
  end
end
