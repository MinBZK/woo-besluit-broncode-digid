
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
    class SmsAuthenticatorsController < BaseController
      include FlowBased
      include AppSessionConcern
      include RdwClient
      include RvigClient
      include NsClient

      before_action :check_prerequisites, only: [:new, :choose_app_or_letter, :create, :confirm_sms, :confirm]
      before_action :render_not_found_if_account_deceased

      def new
        Log.instrument("898", account_id: current_account.id)

        redirect_to existing_sms_request_url and return if current_account.pending_sms_tool.present? # Used to protect against direct url access

        @page_name = "D17"
        @page_title = t("titles.mijn_digid.activate_sms_authenticator.D17")

        session.delete(:sms_options)
        session[:flow] = ActivateSmsAuthenticatorFlow.new
        session[:current_flow] = session[:flow].process
        session[:flow][:failed][:redirect_to] = my_digid_sms_authenticators_afbreken_url
        session[:flow][:verify_app_completed][:redirect_to] = my_digid_sms_authenticators_bevestiging_url
        session[:flow][:cancelled][:redirect_to] = my_digid_url

        @sms_tool = Authenticators::SmsTool.new
      end

      def choose_app_or_letter
        if current_account.bsn.blank? && current_account.oeb.blank?
          flash[:notice] = t("activation_sms_not_possible")
          Log.instrument("1073", account_id: current_account.id)
          render_simple_message(ok: my_digid_url)
        else
          @page_name = "D30"
          @page_title = t("titles.mijn_digid.activate_sms_authenticator.D30")
          @activation_method_options = []
          @activation_method_options << [I18n.t("activate_sms_with_app"), :app, checked: true]
          @activation_method_options << [I18n.t("activate_sms_with_letter"), :letter]
        end
      end

      def create
        sms_tool_params = params.require(:authenticators_sms_tool).permit(:phone_number, :gesproken_sms)
        sms_tool_params[:issuer_type] = current_account.password_authenticator&.issuer_type

        @sms_tool = current_account.sms_tools.pending.build(sms_tool_params)

        if @sms_tool.valid?
          session[:sms_options] = {
            cancel_to:      my_digid_url,
            gesproken_sms:  sms_tool_params[:gesproken_sms],
            instant_cancel: true,
            new_number:     sms_tool_params[:phone_number],
            return_to:      my_digid_sms_authenticators_bevestiging_sms_url,
            step:           :activate_sms_authenticator
          }
          current_flow.transition_to!(:phone_number_verified)
          current_flow.transition_to!(:verify_sms)
          redirect_to authenticators_check_mobiel_url
        else
          @page_name = "D17"
          @sms_tool.gesproken_sms = false # reset to false
          render :new
        end
      end

      def confirm_app_or_letter
        if params[:confirm]
          if params[:confirm][:value] == "letter"
            Log.instrument("1071", account_id: current_account.id)
            if current_account.bsn.blank? && current_account.oeb.present?
              flash[:notice] = t("activation_sms_by_letter_with_oeb_not_possible")
              Log.instrument("1075", account_id: current_account.id)
              render_simple_message(ok: my_digid_url)
            else
              redirect_to request_sms_url
            end
          else
            Log.instrument("1072", account_id: current_account.id)
            redirect_to my_digid_sms_authenticators_start_url
          end
        end
      end

      def confirm_sms
        current_flow.transition_to!(:verify_sms_completed)

        current_flow[:verify_app][:header] = t("headers.mijn_digid.activate_sms_authenticator.D57B")
        current_flow[:qr_app][:header] = t("headers.mijn_digid.activate_sms_authenticator.D37A")
        current_flow[:confirm_in_app][:header] = t("headers.mijn_digid.activate_sms_authenticator.D37B")
        current_flow[:enter_pin][:header] = t("headers.mijn_digid.activate_sms_authenticator.D37C")

        redirect_to my_digid_controleer_app_start_url
      end

      def confirm
        current_flow.transition_to!(:confirmation)

        current_account.sms_tools.destroy_all

        @sms_tool = ::Authenticators::SmsTool.create(
          account: current_account,
          issuer_type: app_authenticator_from_session.substantieel? ? "digid_app_sub" : "digid_app",
          phone_number: DigidUtils::PhoneNumber.normalize(session[:sms_options][:new_number]),
          gesproken_sms: session[:sms_options][:gesproken_sms],
          activated_at: Time.zone.now,
          status: ::Authenticators::SmsTool::Status::ACTIVE
        )

        current_flow.transition_to!(:confirmation_completed)

        Log.instrument("900", account_id: current_account.id)
        NotificatieMailer.delay(queue: "email").notify_sms_controle_activated(account_id: current_account.id, recipient: current_account.adres) if current_account.email_activated?
        ns_client.send_notification(current_account.id, "PSH03", "", current_account.locale.upcase)

        reset_flow
        flash[:notice] = I18n.t("activate_sms_authenticator.app.success")
        redirect_to my_digid_url
      end

      def abort
        current_flow.transition_to!(:aborted) if session[:flow]
        notice_key_prefix = "activate_sms_authenticator.app.abort."
        simple_message_options = {ok: my_digid_url}
        reason = session[:abort_reason].present? ? session[:abort_reason] : current_flow[:failed][:reason]
        case reason
        when "app_not_active"
          flash.now[:notice] = t(notice_key_prefix + "app_not_active")
          Log.instrument("892", account_id: current_account.id)
        when "sms_tool_already_active"
          flash.now[:notice] = t(notice_key_prefix + "sms_tool_already_active")
          Log.instrument("894", account_id: current_account.id)
        when "app_switch_off"
          flash.now[:alert] = t(notice_key_prefix + "app_switch_off")
          Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)
        when "verify_via_app_cancelled_with_app"
          flash.now[:notice] = t(notice_key_prefix + "app_cancelled")
          Log.instrument("962", account_id: current_account.id)
        when "verification_code_invalid"
          flash.now[:notice] = current_flow[:failed][:message]
        end
        reset_flow
        render_simple_message(simple_message_options)
      end

      def cancel
        app_session.cancel! if session[:app_session_id]
        session.delete(:app_session_id)
        Log.instrument("165", account_id: current_account.id) unless session[:flow]&.process == :remove_sms_verification

        if session[:flow]&.process == :remove_sms_verification
          Log.instrument("170")
        elsif session[:flow] && current_flow.process != :activate_sms_authenticator
          Log.instrument("uc5.cancel_#{current_flow.process}")
        end

        redirect_to my_digid_url
      end

      def fail
        flash.now[:notice] = t("you_need_to_activate_sms_code_verification_first_new")
        Log.instrument("639", account_id: current_account.id)
        render_simple_message(ok: my_digid_url)
        reset_flow
      end

      def destroy
        Log.instrument("169", account_id: current_account.id)

        unless current_account.sms_tools.active?
          redirect_via_js_or_html(my_digid_sms_authenticators_mislukt_url)
        end

        session[:flow] = ::RemoveSmsAuthenticatorFlow.new
        current_flow[:completed][:redirect_to] = my_digid_url
        current_flow[:rescue_url] = my_digid_url
        @page_name = "D20A"
      end

      # Destroy sms warning, voorkeur inlogniveau midden
      def dont_destroy_warning
        Log.instrument("1212", account_id: current_account.id, hidden: true)
        if current_account.multiple_two_factor_authenticators?
          Log.instrument("1214", account_id: current_account.id, hidden: true)
          flash.now[:notice] = t("cant_delete_sms_midden_has_app").html_safe
        else
          Log.instrument("1213", account_id: current_account.id, hidden: true)
          flash.now[:notice] = t("cant_delete_sms_midden_has_no_app").html_safe
        end
        render_simple_message(yes: my_digid_sms_authenticators_continue_warning_url, cancel: my_digid_sms_authenticators_cancel_warning_url)
      end

      def continue_warning
        if current_account.multiple_two_factor_authenticators?
          Log.instrument("1203", account_id: current_account.id, hidden: true)
          redirect_to my_digid_sms_authenticators_verwijderen_url
        else
          Log.instrument("1201", account_id: current_account.id, hidden: true)
          redirect_to my_digid_pilot_login_preference_url
        end
      end

      def cancel_warning
        Log.instrument("1202", account_id: current_account.id, hidden: true)
        redirect_to my_digid_url
      end
      # END Destroy sms warning

      private

      def check_prerequisites
        session[:abort_reason] = nil
        if Switch.digid_app_enabled? == false
          session[:abort_reason] = "app_switch_off"
        elsif current_account.active_sms_tool.present?
          session[:abort_reason] = "sms_tool_already_active"
        elsif current_account.app_authenticator_active? == false
          session[:abort_reason] = "app_not_active"
        end

        if session[:abort_reason].present?
          redirect_to my_digid_sms_authenticators_afbreken_url
        end
      end
    end
  end
end
