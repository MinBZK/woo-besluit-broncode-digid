
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
    class SpokenTextMessagesController < BaseController
      # GET [mijn.digid.nl] /gesproken_sms
      before_action :load_sms_tool
      before_action :render_not_found_if_account_deceased

      def show
        Log.instrument("551", account_id: current_account.id)
        @page_name = "D19"
      end

      # PUT [mijn.digid.nl] /gesproken_sms
      def update
        @page_name = "D19"

        @sms_tool.update!(account_params)
        if @sms_tool.previous_changes.include?(:gesproken_sms)
          if @sms_tool.gesproken_sms
            flash[:notice] = t("spoken_sms_is_activated")
            Log.instrument("149", account_id: current_account.id)
          else
            flash[:notice] = t("spoken_sms_is_deactivated")
            Log.instrument("392", account_id: current_account.id)
          end
        end
        redirect_to(my_digid_url)
      end

      def cancel
        Log.instrument("552", account_id: current_account.id)
        redirect_to my_digid_url
      end

      private

      def load_sms_tool
        @sms_tool = current_account.active_sms_tool || current_account.pending_sms_tool
      end

      def account_params
        params.require(:authenticators_sms_tool).permit(:gesproken_sms)
      end
    end
  end
end
