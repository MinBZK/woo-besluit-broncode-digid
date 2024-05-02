
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
    module Apps
      class DeactivationController < MyDigid::BaseController
        include RdwClient
        include RvigClient
        include ApplicationHelper
        include MyDigidHelper
        include HoogSwitchConcern
        include AppSessionConcern
        include FlowBased
        include NsClient
        before_action :render_not_found_if_account_deceased

        def start
          session[:flow] = DeactivateAppFlow.new
          set_flow_redirects
          Log.instrument("738", **app_auth_log_details(app_authenticator), account_id: current_account.id)

          if account_login_level_two_factor?
            Log.instrument("1212", account_id: current_account.id, hidden: true)
            if current_account.multiple_two_factor_authenticators?
              Log.instrument("1222", account_id: current_account.id, hidden: true)
              Log.instrument("1203", account_id: current_account.id, hidden: true)
            else
              Log.instrument("1221", account_id: current_account.id, hidden: true)
              Log.instrument("1201", account_id: current_account.id, hidden: true)
              # Cannot deactivate app if login prefence is midden and app is last authentication. And user is not in digid_light
              return redirect_to my_digid_pilot_login_preference_url
            end
          end

          render_warning_if_inactive_or_last ||
          render_warning_if_logged_in_with_this_app ||
          redirect_to(my_digid_apps_deactivation_confirm_url)
        end

        def confirm
          @page_name = "D35"

          # User already confirmed to deactivate this app
          if session[:authenticator][:id] == app_authenticator.id
            Log.instrument("1203", account_id: current_account.id, hidden: true)
            redirect_to(my_digid_new_verification_url)
          end

          session[:app_authenticator_id] = app_authenticator.id
        end

        def destroy
          if current_flow.process == :deactivate_app && current_flow.verified?
            ns_client.deregister_app(app_authenticator)
            return unless app_authenticator&.destroy

            flash[:notice] = t("digid_app.deactivated")
            Log.instrument("740", **app_auth_log_details(app_authenticator), account_id: current_account.id)

            current_flow.transition_to!(:completed)

            app_session&.complete!

            if session[:authenticator][:id] == params[:id].to_i
              render_logout_with_content(
                page_header: t("digid_app.deactivate_notice.logout.header"),
                page_content: t("digid_app.deactivate_notice.logout.message"),
                page_name: "D63"
              )
            else
              redirect_to(my_digid_url)
            end

            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_digid_app_deactivated(account_id:  current_account.id, recipient: current_account.adres) # ED032
            elsif current_account.phone_number.present?
              service = SmsChallengeService.new(account: current_account)
              current_account.with_language { service.send_sms(message: t("sms_message.SMS26"), spoken: false) }
            end
          end
        end

        def cancel
          Log.instrument("1202", account_id: current_account.id, hidden: true)

          # flow has not started yet (cancel in 2FA popup warning for example)
          if session[:flow].try(:process) != :deactivate_app
            redirect_to(my_digid_url)
          else 
            redirect_to(my_digid_verification_cancelled_url)
          end
        end

        private
        def app_authenticator
          @app_authenticator ||= current_account.app_authenticators.where(id: params[:id]).first
        end

        def render_warning_if_inactive_or_last
          if !app_authenticator&.active?
            flash[:notice] = t("digid_app.not_activated_deactivation_impossible")
            Log.instrument("746", account_id: current_account.id)

            render_message(button_to: my_digid_path, no_cancel_to: true, button_to_options: { method: :get })
          elsif current_account.last_authenticator?
            flash[:notice] = t("digid_app.last_authenticator_deactivation_not_possible")
            Log.instrument("1327", account_id: current_account.id, hidden: true)

            render_message(button_to: my_digid_path, no_cancel_to: true, button_to_options: { method: :get })
          end
        end

        def render_warning_if_logged_in_with_this_app
          if session[:authenticator][:id] == app_authenticator.id
            flash.now[:notice] = t("digid_app.deactivate_notice.body").html_safe
            Log.instrument("1336", account_id: current_account.id)
            render_simple_message(yes: my_digid_apps_deactivation_confirm_url, cancel: my_digid_apps_deactivation_cancel_url)
          end
        end

        def set_flow_redirects
          current_flow[:verified][:redirect_to] = my_digid_apps_deactivation_destroy_url(app_authenticator)
          current_flow[:failed][:redirect_to] = my_digid_apps_deactivation_cancel_url(app_authenticator)
          current_flow[:cancelled][:redirect_to] = my_digid_apps_deactivation_cancel_url(app_authenticator)
          current_flow[:verify_with_wid][:abort_url] = my_digid_abort_wid_url
        end
      end
    end
  end
end
