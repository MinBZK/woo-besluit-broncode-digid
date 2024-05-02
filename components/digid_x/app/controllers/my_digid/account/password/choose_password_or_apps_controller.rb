
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
    module Password
      class ChoosePasswordOrAppsController < BaseController
        include FlowBased

        before_action :return_if_no_active_password_tool, only: [:show, :create]
        before_action :render_not_found_if_account_deceased

        # GET
        def show
          if (!digid_app_enabled? || !current_account.app_authenticator_active?) && current_account.blocking_manager.blocked?
            Log.instrument("1412", account_id: current_account.id)
            return render_blocked(i18n_key: "password_blocked_changing_password_not_allowed",
                                  page_name: "G3",
                                  show_message: true,
                                  show_expired: false)
          end

          session[:flow] = ChangePasswordFlow.new
          current_flow.transition_to!(:choose_method)
          @page_name = "D14"
          @choice_to_proceed = Confirm.new(value: "change_password")
          Log.instrument("134", account_id: current_account.id)
          set_flow_variables
        end

        def create
          if params[:confirm][:value] == "true"
            redirect_to my_digid_change_via_password_url
          else
            redirect_to my_digid_change_via_app_url
          end
        end

        private

        def set_flow_variables
          current_flow[:verify_app][:header] = t("change_renew_password")
          current_flow[:qr_app][:header] = t("change_renew_password")
          current_flow[:confirm_in_app][:header] = t("change_renew_password")
          current_flow[:enter_pin][:header] = t("change_renew_password")

          current_flow[:failed][:redirect_to] = failed_my_digid_change_via_app_url
          current_flow[:cancelled][:redirect_to] = my_digid_cancel_change_password_url
          current_flow[:password_change_authenticated][:redirect_to] = confirm_my_digid_change_via_app_url
        end
      end
    end
  end
end
