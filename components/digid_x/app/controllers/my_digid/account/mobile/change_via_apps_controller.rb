
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
    module Mobile
      class ChangeViaAppsController < ChangeMobilesController
        include AppAuthenticationSession
        include MobileConcern
        include FlowBased

        before_action :check_show_app_switch, except: [:show]
        before_action :check_app_active, unless: :digid_app_switch_is_not_enabled, except: [:poll, :blocked]

        def show
          session[:flow] = ChangePhoneNumberFlow.new
          set_flow_variables
          @page_name = "D12"
          @page_title = I18n.t("titles.D12")
          Log.instrument("897", account_id: current_account.id)
          @sms_tool = Authenticators::SmsTool.new
        end

        def create
          current_flow.transition_to!(:verify_sms)
          @sms_tool = build_pending_sms_tool(account: current_account, **sms_tool_params.merge(issuer_type: current_account.active_sms_tool.issuer_type))

          if @sms_tool.valid?
            # params[:authenticators_sms_tool][:phone_number] = sms_tool.phone_number # valid also cleans-up mobile number
            session[:change_mobile_flow] << "|nr" if session[:change_mobile_flow]
            # delete sms_challenges when it's a new number
            if current_account.phone_number != @sms_tool.phone_number
              current_account.sms_challenges.each { |sms| sms.destroy if sms.state.incorrect? || sms.state.pending? }
            end
            sms_options(:confirm_new_mobile_number_via_app, my_digid_controleer_app_start_url, gesproken_sms: @sms_tool.gesproken_sms, new_number: @sms_tool.phone_number )
            redirect_to authenticators_check_mobiel_url
          else
            Log.instrument("150", account_id: current_account.id)
            @page_name = "D12"
            @page_title = I18n.t("titles.D12")
            render :show
          end
        end

        def blocked
          @link_back = true
          reset_session
          reset_flow
          flash.now[:notice] = blocked_message.html_safe
          render_simple_message
        end

        def cancel
          Log.instrument("148", account_id: current_account.id)

          reset_flow
          cleanup_pending_sms_tool
          flash.now[:notice] = t("digid_app.change_phone_number.cancelled")
          render_simple_message(ok: my_digid_url)
        end

        def aborted
          reset_flow
          cleanup_pending_sms_tool
          flash.now[:notice] = t("digid_app.change_phone_number.session_not_found")
          render_simple_message(ok: my_digid_url)
        end

        def failed
          case current_flow[:failed][:reason]
          when "no_app_session"
            reset_flow
            redirect_to aborted_my_digid_change_via_app_url
          when "app_switch_off"
            Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)
            flash.now[:alert] = t("changing_mobile_via_digid_app_temporarily_not_possible")

            reset_flow
            cleanup_pending_sms_tool
            render_simple_message(ok: my_digid_url)
          when "verification_code_invalid"
            flash.now[:notice] = current_flow[:failed][:message]
            cleanup_pending_sms_tool
            render_simple_message(ok: my_digid_url)
          else
            redirect_to cancel_my_digid_change_mobile_via_app_url
          end
        end

        def confirmation
          if app_session.state == "AUTHENTICATED"
            current_flow.transition_to!(:completed)
            current_account.with_language { SmsChallengeService.new(account: current_account).send_sms(message: t("sms_message.SMS24")) } if current_account.sms_tool_active?
            current_account.activate_sms_tool!
            # TODO: Expire session
            # redis.expire(app_session_key, ::Configuration.get_int("app_session_expires_in").minute)

            current_account.destroy_old_email_recovery_codes do |account|
              Log.instrument("889", account_id: account.id, attribute: "telefoonnummer", hidden: true)
            end

            Log.instrument("153", account_id: current_account.id)

            # send notify telefoonnummer wijziging email
            NotificatieMailer.delay(queue: "email").notify_telefoonnummer_wijziging(account_id: current_account.id, recipient: current_account.adres) if current_account.email_activated?

            redirect_after_phone_number_changed
          end
        end

        protected
        def cleanup_pending_sms_tool
          current_account&.pending_sms_tool&.destroy
        end

        def digid_app_switch_is_not_enabled
          !digid_app_enabled?
        end

        def check_show_app_switch
          return if digid_app_enabled?
          Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)

          reset_flow
          flash.now[:alert] = t("changing_mobile_via_digid_app_temporarily_not_possible")
          render_simple_message(ok: my_digid_url)
        end

        def check_app_active
          return if current_account.app_authenticator_active?

          reset_flow
          flash.now[:alert] = t("cant_use_digid_app_for_changing_mobile")
          Log.instrument("913", account_id: current_account.id, hidden: true)
          render_simple_message(ok: new_my_digid_change_mobile_url)
        end

        def redirect_after_phone_number_changed
          session.delete(:change_via_app)
          reset_flow
          redirect_to(my_digid_url, notice: t("your_mobile_number_changed"))
        end

        def sms_tool_params
          params.require(:authenticators_sms_tool).permit(:phone_number, :gesproken_sms).merge(current_phone_number:current_account.phone_number).to_h.symbolize_keys
        end

        def set_flow_variables
          current_flow[:verify_app][:header] = t("change_mobile_number")
          current_flow[:qr_app][:header] = t("change_mobile_number")
          current_flow[:confirm_in_app][:header] = t("change_mobile_number")
          current_flow[:enter_pin][:header] = t("change_mobile_number")

          current_flow[:failed][:redirect_to] = failed_my_digid_change_mobile_via_app_url
          current_flow[:cancelled][:redirect_to] = cancel_my_digid_change_mobile_url
          current_flow[:mobile_change_authenticated][:redirect_to] = confirm_my_digid_change_mobile_via_app_url
        end
      end
    end
  end
end
