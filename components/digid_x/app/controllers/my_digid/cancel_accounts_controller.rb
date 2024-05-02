
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
  class CancelAccountsController < BaseController
    include FlowBased
    include AppSessionConcern
    before_action :render_not_found_if_account_deceased

    # GET [mijn.digid.nl] /bevestigen_opheffen_digid
    def confirm
      session[:flow] = CancelAccountFlow.new

      set_flow_redirects
      @page_name = "D24" # omgebouwde D24 zou dit zijn.
      Log.instrument("175", account_id: current_account.id)
    end

    def dispatch_choice_to_proceed
      if Confirm.new(confirm_params).yes?
        redirect_to my_digid_new_verification_url
      else
        current_flow.transition_to!(:cancelled)
        Log.instrument("1405", account_id: current_account.id)
        redirect_to my_digid_url
      end
    end

    def cancel
      current_flow.transition_to!(:cancelled) unless current_flow.state == :failed
      if ["no_active_app","app_switch_off"].include?(current_flow[:failed][:reason])
        Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)
        flash.now[:alert] = t("cancel_account_via_digid_app_temporarily_not_possible")
        render_simple_message(ok: my_digid_url)
      elsif current_flow.state == :failed && current_app_session.any? && current_app_session(true).dig(:abort_code) == "not_equal_to_current_user_app_id"
        if logged_in_web_to_app?
          Log.instrument("1349", account_id: current_account.id)
          reset_session
          flash.now[:notice] = t("signed_out_digid_app_not_found")
          @link_back = true
          @heading_with_icon = "digid_app_not_found"
          return render_simple_message(page_name: "D64")
        end
      else
        redirect_via_js_or_html(my_digid_verification_cancelled_url)
      end
    end

    # DELETE [mijn.digid.nl] /opheffen_digid
    def destroy
      current_account.reload
      if (current_flow.process == :cancel_account && current_flow.verified?) && !current_account.blocking_manager.blocked?
        notify_user_account_is_removed
        Log.instrument("177", account_id: current_account.id)
        current_account.destroy

        current_flow.transition_to!(:completed)

        # app continues polling during hoog and expects confirmed state instead of verified
        app_session&.confirm! if app_session&.flow == "authenticate_app_with_eid"

        reset_session
        @page_name = "D25"
      else
        render_not_found
      end
    end

    private

    def notify_user_account_is_removed
      if current_account.email_activated?
        NotificatieMailer.delay(queue: "email").notify_account_removed(account_id: current_account.id, recipient: current_account.adres)
      elsif current_account.phone_number.present?
        sms_service = SmsChallengeService.new(account: current_account)
        current_account.with_language { sms_service.send_sms(message: t("sms_message.SMS25"), spoken: false) }
      end
    end

    def set_flow_redirects
      current_flow[:verified][:redirect_to] = my_digid_voltooi_opheffen_digid_redirect_url
      current_flow[:failed][:redirect_to] = my_digid_annuleren_opheffen_digid_url
      current_flow[:cancelled][:redirect_to] = my_digid_annuleren_opheffen_digid_url
      current_flow[:verify_with_wid][:abort_url] = my_digid_abort_wid_url
    end
  end
end
