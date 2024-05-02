
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
  module Verifications
    class AppVerificationsController < BaseController
      include FlowBased
      include AppAuthenticationSession
      include AppLinkHelper
      include ApplicationHelper
      include VerificationConcern
      before_action :check_app, except: [:poll]

      def new
        current_flow.transition_to!(:verify_app)

        app_session = App::Session.create(app_session_options(current_flow.process))
        @app_session_id = session[:app_session_id] = app_session.id

        if mobile_browser? && !%i[authenticate_substantial add_email change_email remove_email cancel_account deactivate_app].include?(current_flow.process)
          choose_device
        else
          verification_code
        end
      end

      def verification_code
        set_page_title_and_page_header("D57B", current_flow)
        @code = ::AppVerificationCode.new

        render :verification_code
      end

      def choose_device
        set_page_title_and_page_header("D59", current_flow)
        # WORKAROUND revoke driving licence is setting a custom page_title that only applies to D57B in :verify_app state
        @page_title = t("titles.mijn_digid.#{current_flow.process}.D59") if [:revoke_identity_card, :revoke_driving_license].include?(current_flow.process)
        render :choose_device
      end

      def qr_code
        @code = AppVerificationCode.new(params.require(:app_verification_code).permit(:verification_code)) if params[:app_verification_code].present?
        @app_session_id = session[:app_session_id]

        if @code&.invalid?
          render :verification_code
        else
          current_flow.transition_to!(:qr_app)
          set_page_title_and_page_header("D37A", current_flow)
        end
      end

      def failed
        render_simple_message(flash[:actions] || {})
      end

      def confirm_in_app
        current_flow.transition_to!(:confirm_in_app)
        set_page_title_and_page_header("D37B", current_flow)
      end

      def enter_pin
        current_flow.transition_to!(:enter_pin)
        set_page_title_and_page_header("D37C", current_flow)
      end

      def web_to_app
        human_process = t("process_names.log.#{current_flow.process}", locale: :nl)
        unless browser_supported_for_web_to_app?
          Log.instrument("1548", human_process: human_process, account_id: current_account&.id, hidden: true)
          flash.now[:notice] = t("ios_browser_not_supported_for_my_digid_with_app").html_safe
          return render_simple_message(previous: my_digid_url)
        end

        app_session = App::Session.create(app_session_options(current_flow.process, app_id: session[:authenticator][:id]))
        @app_session_id = session[:app_session_id] = app_session.id
        @url = digid_app_link(digid_app_provisioning_uri("auth", session[:app_session_id]))

        redirect_to @url
      end

      # Used for ios devices
      def return_mobile_only
        if app_session.state == "AUTHENTICATED"
          if current_flow[:verified].present?
            current_flow.transition_to!(:verified)
          else
            current_flow.complete_step!(:verify_app, controller: self)
          end
          return redirect_to current_flow.redirect_to
        elsif app_session.state == "ABORTED" && %w[authenticate_substantial add_email change_email remove_email cancel_account deactivate_app].include?(app_session.action)
          current_flow.transition_to!(:failed)
          return redirect_to current_flow.redirect_to
        end
      end

      def invalid_verification_code
        flash.now[:notice] = t("digid_app.verification_code_invalid", human_process: t("process_names.notice.#{current_flow.process}"))
        render_simple_message({ ok: current_flow.redirect_to })
      end

      private

      def check_poll
        state = app_session.state

        if !state
          current_flow.transition_to!(:failed)
          current_flow[:failed][:reason] = "no_app_session"
          return { url: current_flow.redirect_to }
        elsif !digid_app_enabled?
          current_flow.transition_to!(:failed)
          current_flow[:failed][:reason] = "app_switch_off"
          return { url: current_flow.redirect_to }
        elsif state == "CANCELLED"
          current_flow.transition_to!(:failed)
          current_flow[:failed][:reason] = "verify_via_app_cancelled_with_app"
          return { url: current_flow.redirect_to }
        elsif state == "FAILED"
          current_flow.transition_to!(:failed)
          if ["invalid_instance_id", "invalid_user_app_id"].include?(app_session.error)
            flash[:notice] = blocked_message.html_safe
            flash[:actions] = { ok: my_digid_url }
            return { url: my_digid_controleer_app_failed_url }
          else
            current_flow[:failed][:reason] = "verify_via_app_failed"
            return { url: app_session.error == "invalid_pin" ? request_logout_url : current_flow.redirect_to }
          end
        elsif state == "AUTHENTICATED"
          if current_flow[:verified].present?
            current_flow.transition_to!(:verified)
          else
            current_flow.complete_step!(:verify_app, controller: self)
          end
          return { url: current_flow.redirect_to }
        elsif state == "ABORTED"
          current_flow.transition_to!(:failed)

          abort_code = app_session.abort_code
          if abort_code == "verification_code_invalid"
            document_type = t("document.driving_licence", locale: :nl) if current_flow.process == :revoke_driving_license
            document_type ||= t("document.id_card", locale: :nl) if current_flow.process == :revoke_identity_card

            Log.instrument("1420", hidden: true, human_process: log_process, document_type: document_type)
            current_flow[:failed][:reason] = abort_code

            return { url:  my_digid_invalid_verification_code_url }
          elsif abort_code == "not_equal_to_current_user_app_id"
            current_flow[:failed][:reason] = abort_code.to_sym
          else
            current_flow[:failed][:reason] = "verify_via_app_aborted"
          end
          return { url: current_flow.redirect_to }
        elsif %w(RETRIEVED AWAITING_CONFIRMATION).include?(state) && params[:current_step] != "confirm"
          return { url: my_digid_controleer_app_confirm_in_app_url }
        elsif state == "CONFIRMED" && params[:current_step] != "enter_pin"
          return { url: my_digid_controleer_app_enter_pin_url }
        else
          return nil
        end
      end

      def check_app
        if !current_account.app_authenticator_active?
          current_flow.transition_to!(:failed)
          current_flow[:failed][:reason] = "no_active_app"
          return redirect_to current_flow.redirect_to
        end

        if !digid_app_enabled?
          current_flow.transition_to!(:failed)
          current_flow[:failed][:reason] = "app_switch_off"
          return redirect_to current_flow.redirect_to
        end
      end

      def app_session_options(action = "verify", options = {})
        {
          flow: "confirm_session",
          state: "AWAITING_QR_SCAN",
          account_id: current_account.id,
          action: action,
          app_to_destroy: session[:app_authenticator_id],
          return_url: formatted_return_url(my_digid_return_mobile_only_url, action)
        }.merge(options)
      end
    end
  end
end
