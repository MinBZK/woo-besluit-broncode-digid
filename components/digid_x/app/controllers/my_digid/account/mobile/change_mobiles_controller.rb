
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
      class ChangeMobilesController < BaseController
        include AppAuthenticationSession
        include MobileConcern
        include FlowBased
        before_action :flow_check_change_mobile, only: [:new, :password, :edit, :verify, :update, :confirm]
        before_action :verify_authorisation, except: [:fail]
        before_action :render_not_found_if_account_deceased

        def new
          delete_change_mobile_sessions
          @page_name = "D27"
          Log.instrument("147", account_id: current_account.id)
          session[:flow] = ChangePhoneNumberFlow.new
          session[:current_flow] = session[:flow].process

          @confirm = Confirm.new
        end

        def cancel
          Log.instrument("148", account_id: current_account.id)
          delete_change_mobile_sessions
          reset_flow
          redirect_to my_digid_url
        end

        def password
          @confirm = Confirm.new(confirm_params)
          if @confirm.via_digid_app?
            session[:change_via_app] = { return_to: verify_my_digid_change_mobile_url }
            redirect_via_js_or_html(my_digid_change_mobile_via_app_url)

          elsif @confirm.via_letter?
            session[:change_via_letter] = { return_to: verify_my_digid_change_mobile_url }
            session[:change_mobile_while_old_number_not_usable] = true
            redirect_to request_sms_url

          elsif @confirm.yes?
            # Needed for log #1416 and #1417
            session[:flow] = ChangePhoneNumberFlow.new
            Log.instrument("896", account_id: current_account.id)
            session[:check_pwd] = { return_to: verify_my_digid_change_mobile_url,
                                    steps: { at: 1, of: 4 },
                                    method: :put,
                                    page_name: "D11",
                                    page_header: t("headers.mijn_digid.confirm_new_mobile_number.D11"),
                                    page_title: I18n.t("titles.D11.change_mobile")}
            redirect_via_js_or_html(password_check_url)
          else
            session[:change_mobile_while_old_number_not_usable] = true
            redirect_to request_sms_url
          end
        end

        def verify
          if session[:change_via_letter]
            sms_options(:confirm_via_letter, edit_my_digid_change_mobile_url)
          else
            sms_options(:confirm_old_mobile_number, edit_my_digid_change_mobile_url)
          end

          redirect_to authenticators_check_mobiel_url
        end

        # D12: Mijn DigiD | Wijzigen telefoonnummer - nieuw nummer
        def edit
          @page_name = "D12"
          session[:new_new_number] = true
          @sms_tool = Authenticators::SmsTool.new
        end

        # step 2) change the number if valid
        def create
          @page_name = "D12"
          @sms_tool = build_pending_sms_tool(account: current_account, **sms_tool_params.merge(issuer_type: current_account.active_sms_tool.issuer_type))

          if @sms_tool.valid?
            # params[:authenticators_sms_tool][:phone_number] = sms_tool.phone_number # valid also cleans-up mobile number
            session[:change_mobile_flow] << "|nr" if session[:change_mobile_flow]
            # delete sms_challenges when it's a new number
            if current_account.phone_number != @sms_tool.phone_number
              current_account.sms_challenges.each { |sms| sms.destroy if sms.state.incorrect? || sms.state.pending? }
            end
            sms_options(:confirm_new_mobile_number, confirm_my_digid_change_mobile_url, gesproken_sms: @sms_tool.gesproken_sms, new_number: @sms_tool.phone_number )
            redirect_to authenticators_check_mobiel_url
          else
            Log.instrument("150", account_id: current_account.id)
            render :edit
          end
        end

        # step 4) aftermath for D12, saves the new mobile number
        def confirm
          # in case we're changing numbers through MijnDigiD, save the new number and forget about the old
          current_account.activate_sms_tool!

          current_account.destroy_old_email_recovery_codes do |account|
            Log.instrument("889", account_id: account.id, attribute: "telefoonnummer", hidden: true)
          end

          Log.instrument("153", account_id: current_account.id)
          delete_change_mobile_sessions
          redirect_to my_digid_url, notice: t("your_mobile_number_changed")

          # send notify telefoonnummer wijziging email
          NotificatieMailer.delay(queue: "email").notify_telefoonnummer_wijziging(account_id: current_account.id, recipient: current_account.adres) if current_account.email_activated?
        end

        def fail
          Log.instrument("567", account_id: current_account.id)
          mobile = current_account.pending_phone_number
          flash.now[:notice] = t("you_requested_the_sms_function_within_my_digid", mobile_number: view_context.mobile_number(mobile_number: mobile)).html_safe
          render_message button_to: my_digid_url, no_cancel_to: true
        end

        private

        def delete_change_mobile_sessions
          [:change_mobile_flow, :change_via_app, :change_via_letter,
           :check_pwd, :change_mobile_while_old_number_not_usable].each { |x| session.delete(x) }
        end

        # the process for changing mobile number, which is
        # step 0) password check (screen D11)
        # step 1) send sms to the old number (screen C2)
        # step 2) change number (screen D12)
        # step 3) send sms to the new number (screen C2)
        # step 4) save the new data
        # return to mijn_digid (screen D1)
        # gets skipped if chosen to change via DigiD app or letter
        def flow_check_change_mobile
          return if session[:change_via_app]
          session[:change_mobile_flow] ||= ""
          case params[:action]
          when "verify"
            redirect_to new_my_digid_change_mobile_url unless session[:change_mobile_flow] =~ /^[\|pwd]+/
          when "edit"
            redirect_to new_my_digid_change_mobile_url unless session[:change_mobile_flow] =~ /^[\|pwd]+[\|sms]+/
          when "confirm"
            redirect_to new_my_digid_change_mobile_url unless session[:change_mobile_flow] =~ /^[\|pwd]+[\|sms]+[\|nr]+[\|sms]+/
          end
        end

        def sms_tool_params
          params.require(:authenticators_sms_tool).permit(:phone_number, :gesproken_sms).merge(current_phone_number:current_account.active_sms_tool.try(:phone_number)).to_h.symbolize_keys
        end

        def verify_authorisation
          return unless current_account.sms_in_uitbreiding? || current_account.mobiel_kwijt_in_progress?

          redirect_via_js_or_html(fail_my_digid_change_mobile_path)
        end
      end
    end
  end
end
