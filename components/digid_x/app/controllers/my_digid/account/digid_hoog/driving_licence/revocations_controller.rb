
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
    module DigidHoog
      module DrivingLicence
        class RevocationsController < BaseController
          include AppAuthenticationSession
          include RdwClient
          include FlowBased

          before_action :set_flow_variables, only: [:proceed]
          before_action :check_rijbewijs_switches, except: [:abort]
          before_action :check_before_proceed, only: [:proceed]
          before_action :render_not_found_if_account_deceased

          def proceed
            if params[:back]
              Log.instrument("1013", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
              redirect_to my_digid_url
            else
              Log.instrument("937", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true) if params[:doorgaan]
              Log.instrument("946", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
              @page_header = t("confirm_start_revocation", wid_type: t("document.driving_licence"))
              @page_name = "D55"
              @page_title = t("titles.D55.driving_licence")
              session[:sequence_no_chosen] = params[:sequence_no]
              flash.now[:notice] = t("revoke_driving_licence").html_safe
              @choice_to_proceed = Confirm.new(value: true)
              render "proceed"
            end
          end

          # Keuze authenticatie method
          def index
            if params[:confirm] && params[:confirm][:value] == "true"
              begin
                @driving_licence = rdw_client.get(bsn: current_account.bsn, sequence_no: session[:sequence_no_chosen]).first
              rescue DigidUtils::DhMu::RdwError => e
                current_flow[:failed][:reason] = "foutbericht_mu_not_allowed"
                return redirect_via_js_or_html my_digid_licence_revocation_abort_url
              end

              if @driving_licence.status == "Uitgereikt"
                current_flow[:start][:url] = my_digid_licence_revocation_new_with_code_url
              else
                current_flow[:start][:url] = my_digid_licence_revocation_new_url
              end

              redirect_via_js_or_html current_flow[:start][:url]
            else
              Log.instrument("1013", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
              redirect_to my_digid_url
            end
          end

          def new
            @page_name = current_flow[:choose_method][:page_name]
            @page_title = current_flow[:choose_method][:page_title]
            @page_header = current_flow[:choose_method][:header]
            current_flow.transition_to!(:choose_method)

            @confirm = Confirm.new

            redirect_to my_digid_licence_revocation_new_with_code_url if revocation_options.size == 1 && revocation_options.first[1] == :code
          end

          # D44 - Gebruiken intrekkingscode eID
          def new_with_code
            @page_name = current_flow[:verify_code][:page_name]
            @page_title = current_flow[:verify_code][:page_title]
            @page_header = current_flow[:verify_code][:header]

            unless flash[:revoke_incorrect] || flash[:revoke_invalid] || flash[:revoke_blank]
              Log.instrument("960", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
            end

            @wid = Wid.where(sequence_no: current_flow[:revocation_authenticated][:sequence_no], card_type: current_flow[:revocation_authenticated][:card_type]).first_or_create
            if @wid.blocking_manager.blocked?
             current_flow[:failed][:wid] = @wid
             current_flow[:failed][:reason] = "code_blocked"
             return redirect_to my_digid_licence_revocation_abort_url
            end

            current_flow.transition_to!(:verify_code)
            @intrekkingscode = WidRevocationCode.new

            @intrekkingscode.set_error("incorrect") if flash[:revoke_incorrect]
            @intrekkingscode.set_error("invalid") if flash[:revoke_invalid]
            @intrekkingscode.set_error("blank") if flash[:revoke_blank]
          end

          def verify_code
            @intrekkingscode = WidRevocationCode.new(params[:wid_revocation_code].values)
            @wid = Wid.where(sequence_no: current_flow[:revocation_authenticated][:sequence_no], card_type: current_flow[:revocation_authenticated][:card_type]).first_or_create
            current_flow[:revocation_authenticated][:intrekkingscode] = @intrekkingscode
            current_flow[:revocation_authenticated][:wid] = @wid

            if @wid.blocking_manager.blocked?
              current_flow[:failed][:wid] = @wid
              current_flow[:failed][:reason] = "code_blocked"
              return redirect_to my_digid_licence_revocation_abort_url
            elsif @intrekkingscode.valid?
              current_flow.transition_to!(:verify_code)
              current_flow.complete_step!(:verify_code, controller: self)
              current_flow.transition_to!(:saving_revocation)
              return redirect_to my_digid_licence_revocation_warning_url
            else
              @page_name = current_flow[:verify_code][:page_name]
              @page_title = current_flow[:verify_code][:page_title]
              @page_header = current_flow[:verify_code][:header]

              error_types = @intrekkingscode.errors.details[:intrekkingscode].collect {|x| x[:error]}

              if error_types.include?(:invalid) && !error_types.include?(:blank)
                Log.instrument("965", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                flash[:revoke_invalid] = true
              elsif error_types.include?(:blank)
                Log.instrument("966", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                flash[:revoke_blank] = true
              end

              @intrekkingscode.intrekkingscode = nil
              return redirect_via_js_or_html my_digid_licence_revocation_new_with_code_url
            end
          end

          def create
            @confirm = Confirm.new(confirm_params)

            unless @confirm.valid?
              @page_name = current_flow[:choose_method][:page_name]
              @page_title = current_flow[:choose_method][:page_title]
              @page_header = current_flow[:choose_method][:header]
              @confirm.errors.messages[:value] = [t("digid_hoog.revoke.confirm_error_licence")]
              render :new
              return
            end

            current_flow[:choose_method][:revoke_methode] = @confirm.value
            case @confirm.value
            when "code"
              Log.instrument("957", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
              redirect_to my_digid_licence_revocation_new_with_code_url
            when "app"
              current_flow[:cancelled][:redirect_to] = my_digid_licence_revocation_cancel_url

              if current_account.level == ::Account::LoginLevel::TWO_FACTOR
                Log.instrument("973", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                current_flow[:choose_method][:revoke_methode] = "app_midden"
              elsif current_account.level == ::Account::LoginLevel::SMARTCARD
                Log.instrument("1000", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                current_flow[:choose_method][:revoke_methode] = "app_substantieel"
              end

              redirect_to my_digid_controleer_app_start_url
            end
          end

          def update_status!
            current_flow.transition_to!(:saving_revocation)
            sequence_no = current_flow[:revocation_authenticated][:sequence_no]

            sequence_no = nil if current_flow[:revocation_authenticated][:intrekkingscode].try(:revocation_hash)

              begin
                rdw_client.revoke(bsn: current_account.bsn, sequence_no: sequence_no, revoke_hash: current_flow[:revocation_authenticated][:intrekkingscode].try(:revocation_hash))
                Log.instrument("1006", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
              rescue DigidUtils::DhMu::RdwFault => error
                if error.code == "SW2"
                  @wid = current_flow[:revocation_authenticated][:wid]
                  @wid.blocking_manager.register_failed_attempt!
                  Log.instrument("958", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                  flash[:revoke_incorrect] = true
                  return redirect_to my_digid_licence_revocation_new_with_code_url
                elsif error.code == "SW4"
                  Log.instrument("959", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                  current_flow[:failed][:reason] = "foutbericht_mu_not_allowed"
                  return redirect_to my_digid_licence_revocation_abort_url
                else
                  logger.error("RdwFault: "+ error.message)
                  current_flow[:failed][:reason] = "foutbericht_mu"
                  return redirect_to my_digid_licence_revocation_abort_url
                end
              end

            if current_account.email_activated?
              NotificatieMailer.delay(queue: "email").notify_intrekking_rijbewijs(account_id: current_account.id, recipient: current_account.adres)
            elsif current_account.phone_number.present?
              sms_service = SmsChallengeService.new(account: current_account)
              current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS13", wid_type: t("document.driving_licence")), spoken: false) }
            end

            current_flow.transition_to!(:revocation_saved)
            redirect_to my_digid_licence_revocation_confirm_url
          end

          # D47 -  Bevestiging intrekking eID
          def confirm
            current_flow.transition_to!(:completed)
            @page_name = current_flow[:completed][:page_name]
            @page_title = current_flow[:completed][:page_title]
            @page_header = current_flow[:completed][:header]

            app_session&.complete!

            complete_flow
            flash.now[:notice] = [t("the_log_in_with_wid_revoked", wid_type: t("document.driving_licence")), t("you_cant_log_in_with_wid_anymore", wid_type: t("document.driving_licence"))].join(" ").html_safe
          end

          def abort
            current_flow.transition_to!(:failed)
            simple_message_options = { ok: my_digid_url }
            type = { flash: :notice }
            default_key = wid_notice_key(:revoke, error: current_flow[:failed][:reason])

            message = case current_flow[:failed][:reason]
                      when "foutbericht_mu"
                        Log.instrument("955", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        Log.instrument("1337", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "foutbericht_mu_not_allowed"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), mu: "RDW", hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "code_blocked"
                        wid = current_flow[:failed][:wid]
                        Log.instrument("967", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                        t(default_key,
                           since: I18n.l(wid.blocking_manager.timestamp_first_failed_attempt, format: :date_time_text_tzone_in_brackets),
                           count: "3",
                           until: I18n.l(wid.blocking_manager.blocked_till, format: :time_text_tzone_in_brackets),
                           minutes: wid.blocking_manager.blocked_time_left_in_minutes
                          )
                      when "wid_switch_off"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        type[:flash] = :alert
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "no_app_session"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "no_active_app"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "app_switch_off"
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        type[:flash] = :alert
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "verify_via_app_cancelled_with_app"
                        Log.instrument("1036", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
                        t(default_key, wid_type: t("document.driving_licence"))
                      when "verify_via_app_failed"
                        reset_session
                        @page_name = "G4"
                        @link_back = true
                        simple_message_options = {}
                        blocked_message.html_safe
                      when "verification_code_invalid"
                        current_flow[:failed][:message]
                      when "verify_via_app_aborted"
                        redirect_to my_digid_url and return
                      else
                        Log.instrument(default_key, account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
                        t("digid_hoog.revoke.abort.unknown_error", wid_type: t("document.driving_licence"))
                      end

            app_session&.abort!

            flash.now[type[:flash]] = message.html_safe
            render_simple_message(simple_message_options)
            reset_flow
          end

          def cancel
            Log.instrument("1036", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
            current_flow.transition_to!(:cancelled)

            app_session&.cancel!

            reset_flow
            redirect_to my_digid_url
          end

          def cancel_email
            Log.instrument("1067", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl), hidden: true)
            current_flow.transition_to!(:cancelled)
            reset_flow
            redirect_to my_digid_url
          end

          def cancel_revocation_warning
            Log.instrument("1013", account_id: current_account.id, wid_type: t("document.driving_licence", locale: :nl))
            current_flow.transition_to!(:cancelled)
            reset_flow
            redirect_to my_digid_url
          end

          def revocation_warning
            flash.now[:notice] = t("digid_hoog.revoke.confirm_licence").html_safe
            render_simple_message(yes_continue_revocation: my_digid_licence_revocation_update_status_url, cancel: my_digid_licence_revocation_cancel_warning_url)
          end

          private

          def confirm_params
            params.require(:confirm).permit(:value)
          end

          def check_before_proceed
            return if params[:doorgaan] || params[:back]
            if !current_account.email_activated? && current_account.phone_number.blank?
              flash.now[:notice] = t("digid_hoog.shared.no_email_registered")
              render_simple_message(ok_fill_in: my_digid_licence_revocation_cancel_email_url, no_continue: my_digid_licence_revocation_proceed_url(card_type: "NL-Rijbewijs", sequence_no: params[:sequence_no], doorgaan: true))
            end
          end

          def check_rijbewijs_switches
            if request.referer
              return if driving_licence_partly_enabled? && show_driving_licence?(current_account.bsn)
              current_flow[:failed][:reason] = "wid_switch_off"
              redirect_via_js_or_html my_digid_licence_revocation_abort_url
            else
              check_tonen_rijbewijs_switch
            end
          end

          helper_method :revocation_options
          def revocation_options
            @revocation_options ||= [[I18n.t("accounts.wid.revocations.code", wid_type: t("document.driving_licence")), :code]].tap do |options|
              options << [I18n.t("accounts.wid.revocations.app"), :app] if current_account.app_authenticator_active? && digid_app_enabled?
            end
          end

          def set_flow_variables
            session[:flow] = RevokeDrivingLicenceFlow.new
            current_flow[:revocation_authenticated][:redirect_to] = my_digid_licence_revocation_update_status_url
            current_flow[:revocation_authenticated][:card_type] = params[:card_type]
            current_flow[:revocation_authenticated][:sequence_no] = params[:sequence_no]

            current_flow[:choose_method][:page_title] = t("titles.mijn_digid.revoke_driving_license.D43")
            current_flow[:verify_code][:page_title] = t("titles.mijn_digid.revoke_driving_license.D44")
            current_flow[:verify_app][:page_title] = t("titles.mijn_digid.revoke_driving_license.D57B")
            current_flow[:qr_app][:page_title] =  t("titles.mijn_digid.revoke_driving_license.D37A")
            current_flow[:confirm_in_app][:page_title] =  t("titles.mijn_digid.revoke_driving_license.D37B")
            current_flow[:enter_pin][:page_title] =  t("titles.mijn_digid.revoke_driving_license.D37C")
            current_flow[:completed][:page_title] = t("titles.mijn_digid.revoke_driving_license.D47")

            current_flow[:choose_method][:page_name] = "D43"
            current_flow[:verify_code][:page_name] = "D44"
            current_flow[:verify_app][:page_name] = "D57B"
            current_flow[:qr_app][:page_name] = "D37A"
            current_flow[:confirm_in_app][:page_name] = "D37B"
            current_flow[:enter_pin][:page_name] = "D37C"
            current_flow[:completed][:page_name] = "D47"

            current_flow[:choose_method][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:verify_code][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:verify_app][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:qr_app][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:confirm_in_app][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:enter_pin][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))
            current_flow[:completed][:header] = t("revoke_log_in_method_wid", wid_type: t("document.driving_licence"))

            current_flow[:cancelled][:redirect_to] = my_digid_url
            current_flow[:failed][:redirect_to] = my_digid_licence_revocation_abort_url
          end
        end
      end
    end
  end
end
