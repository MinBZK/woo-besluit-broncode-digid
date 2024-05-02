
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

module RecoverAccounts
  class RecoverCodesController < RecoverAccountsController
    before_action :check_session_time
    before_action :update_session
    before_action :any_codes_for_this_account?
    before_action :commit_cancelled?

    def new
      @page_name = "E4"
      @page_title = t("titles.E4E") if account_has_herstelcode_email?
      Log.instrument("591", account_id: current_account.id)
      @recovery_code = RecoveryCode.new
    end

    def create
      @recovery_code = RecoveryCode.new(recovery_code_params.merge(account_id: session[:recovery_account_id]))

      if @recovery_code.valid?
        if @recovery_code.expired?
          Log.instrument("211", account_id: current_account.id)
          flash[:alert] = I18n.t("activemodel.errors.models.recover_account.attributes.recovery_code_expired")
          redirect_to(recover_code_expired_url)
        else
          correct_recovery_code
        end
      else
        allowed_attempts = ::Configuration.get_int("aantal_invoerpogingen_herstelcode")
        if current_account.too_many_recover_attempts?(allowed_attempts)
          Log.instrument("590", account_id: current_account.id)
          flash[:notice] = t("messages.notifications.three_strikes", count: allowed_attempts)
          @recovery_code.remove_recovery_codes(current_account.id)
          render_message(button_to: request_recover_password_url, button_to_options: { method: :get })
        else
          Log.instrument("210", account_id: current_account.id)
          @page_name = "E4"
          @page_title = t("titles.E4E") if account_has_herstelcode_email?
          render :new
        end
      end
    end

    def expired
      render_simple_message(yes: request_recover_password_url, nope: APP_CONFIG["urls"]["external"]["digid_home"])
    end

    private
    def any_codes_for_this_account?
      return if current_account.recovery_codes.where(used: false).any?

      flash.now[:notice] = t("you_do_not_have_any_recovery_codes")
      Log.instrument("592", account_id: current_account.id)

      @confirm_options = { method: "get" }
      @confirm_to = request_recover_password_url
      @page_name  = "G4"
      @return_to  = APP_CONFIG["urls"]["external"]["digid_home"]

      render_partial_or_return_json(".main-content", "shared/cancel", "shared/_cancel")
    end

    def recovery_code_params
      params.require(:recovery_code).permit(:herstelcode)
    end

    def account_has_herstelcode_email?
      current_account.present? && current_account.recovery_codes.where(via_brief: false).any?
    end
    helper_method(:account_has_herstelcode_email?)

    def commit_cancelled?
      cancel_button(return_to: new_recover_code_url) if clicked_cancel?
    end

    def correct_recovery_code
      Log.instrument("196", account_id: current_account.id)
      session[:recovery_code_id] = @recovery_code.code_id
      session[:recover_password_flow] = add_flow_step(session[:recover_password_flow], "|E4") if session[:recover_password_flow]
      session[:recover_account_entry_point] = "herstellen_wachtwoord_code_invoeren_via_email" if @recovery_code.check_sms?

      options = case starting_point
      when "herstellen_wachtwoord_code_invoeren"
        sms_options_for_password
      when "herstellen_wachtwoord_code_invoeren_via_email"
        sms_options_for_password_via_email
      end

      if current_account.sms_tools.active? && options
        session[:sms_options] = options
        redirect_to(authenticators_check_mobiel_url(url_options))
      else
        redirect_to new_recover_password_url
      end
    end

    # return check_mobiel screen parameters for wachtwoordherstel code invoeren
    def sms_options_for_password
      {
        cancel_to: home_url,
        page_name: "C2",
        return_to: new_recover_password_url,
        step: :enter_recover_code
      }
    end

    # return check_mobiel screen parameters for wachtwoordherstel via email
    def sms_options_for_password_via_email
      {
        cancel_to: home_url,
        page_name: "C2",
        return_to: new_recover_password_url,
        step: :request_recover_password_via_email
      }
    end
  end
end
