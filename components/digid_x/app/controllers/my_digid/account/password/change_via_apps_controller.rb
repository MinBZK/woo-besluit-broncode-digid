
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
      class ChangeViaAppsController < ::MyDigid::Account::Password::PasswordsController
        include PasswordConcern
        include AppAuthenticationSession
        include FlowBased

        before_action :check_active_app, except: [:poll, :blocked]

        # GET [mijn.digid.nl] /wachtwoord_wijzigen
        def show
          session[:flow] = ChangePasswordFlow.new unless session[:flow]&.process == :change_password

          current_flow.transition_to!(:with_app)
          @page_name = "D15"
          set_flow_variables
          Log.instrument("890", account_id: current_account.id)
        end

        # D7: "DigiD: Mijn DigiD | Wijzigen wachtwoord"
        def update
          try_to_change_password
        end

        def blocked
          @link_back = true
          reset_session
          flash.now[:notice] = blocked_message.html_safe
          render_simple_message
        end

        def cancel
          Log.instrument("137", account_id: current_account.id)

          notice = t("digid_app.change_password.cancelled")
          flash.now[:notice] = notice
          render_simple_message(ok: my_digid_url)
        end

        def aborted
          notice = t("digid_app.change_password.session_not_found")
          flash.now[:notice] = notice
          render_simple_message(ok: my_digid_url)
        end

        def confirmation
          if app_session.state == "AUTHENTICATED"
            if current_account.password_authenticator.change_password(session[:password], session[:password_confirmation])
              # TODO: Expire session
              # redis.expire(app_session_key, ::Configuration.get_int("app_session_expires_in").minute)
              do_redirect_after_change_password
            end
          end
        end

        def failed
          case current_flow[:failed][:reason]
          when "no_app_session"
            redirect_to aborted_my_digid_change_via_app_url
          when "app_switch_off"
            Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)
            flash.now[:alert] = t("change_password_temporarily_not_possible")
            render_simple_message(ok: my_digid_url)
          when "verification_code_invalid"
            flash.now[:notice] = current_flow[:failed][:message]
            render_simple_message(ok: my_digid_url)
          else
            redirect_to my_digid_cancel_change_password_url
          end
        end

        protected

        def current_password_check
          true
        end

        def password_attributes
          params[:password_changing_account][:password].blank? || params[:password_changing_account][:password_confirmation].blank?
        end

        def change_password
          session[:password] = params[:password_changing_account][:password]
          session[:password_confirmation] = params[:password_changing_account][:password_confirmation]
        end

        def do_redirect_for_password_change
          redirect_to my_digid_controleer_app_start_url
        end

        private

        def check_active_app
          redirect_via_js_or_html(my_digid_url) unless current_account.app_authenticator_active?
        end

        def set_flow_variables
          # only used when starting change password flow with an account that has an active app and a blocked password
          current_flow[:verify_app][:header] = t("change_renew_password") unless current_flow[:verify_app][:header].present?
          current_flow[:qr_app][:header] = t("change_renew_password") unless current_flow[:qr_app][:header].present?
          current_flow[:confirm_in_app][:header] = t("change_renew_password") unless current_flow[:confirm_in_app][:header].present?
          current_flow[:enter_pin][:header] = t("change_renew_password") unless current_flow[:enter_pin][:header].present?

          current_flow[:failed][:redirect_to] = failed_my_digid_change_via_app_url unless current_flow[:failed][:redirect_to].present?
          current_flow[:cancelled][:redirect_to] = my_digid_cancel_change_password_url unless current_flow[:cancelled][:redirect_to].present?
          current_flow[:password_change_authenticated][:redirect_to] = confirm_my_digid_change_via_app_url unless current_flow[:password_change_authenticated][:redirect_to].present?
        end
      end
    end
  end
end
