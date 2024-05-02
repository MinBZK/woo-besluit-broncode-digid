
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
  class AuthenticationsController < RecoverAccountsController
    skip_before_action :check_session_time,       only: [:new]
    skip_before_action :update_session,           only: [:new]
    before_action :through_link?
    before_action :commit_cancelled?

    # GET /herstellen/persoonsgegevens
    def new
      session_variable_to_remember = session[:recover_account_entry_point]
      start_session("recover_account")
      session[:recover_account_entry_point] = session_variable_to_remember

      Log.instrument("617")
      @news_items = news_items("Herstelpagina")
      @page_name = "E1"
      @recover_account = RecoverAccount.new

      render_partial_or_return_json("#recover-account-form", :form, :new)
    end

    # POST /herstellen/persoonsgegevens
    def create
      @news_items = news_items("Herstelpagina")
      @recover_account = RecoverAccount.new(authentication_params.merge(starting_point: starting_point))
      if @recover_account.valid?
        if @recover_account.account.deceased?
          Log.instrument("1480", account_id: @recover_account.account.id, hidden: true)
          flash[:notice] = t("account.deceased.recovery_code")
          redirect_via_js_or_http(recover_authentication_error_url)
        else
          Log.instrument("192", account_id: @recover_account.account.id)
          set_recovery_session
        end
      else
        @page_name = "E1"
        @recover_account.errors[:authentication].each do |error|
          error.gsub!("[ACTIVATE]", activate_url)
          error.gsub!("[REQUEST]", new_registration_url)
        end

        if @recover_account.errors[:blocked].any?
          flash[:notice] = @recover_account.errors[:blocked].first
          redirect_via_js_or_http(recover_authentication_error_url)
        else
          render_partial_or_return_json("#recover-account-form", :form, :new)
        end
      end
    end

    def show_error
      render_simple_message(ok: APP_CONFIG["urls"]["external"]["digid_home"])
    end

    private
    def authentication_params
      params.require(:recover_account).permit(:burgerservicenummer, :gebruikersnaam)
    end

    def through_link?
      session_expired unless session[:recover_account_entry_point].present?
    end

    # handle cancel buttons on all recover account screens
    def commit_cancelled?
      cancel_button(return_to: request_recover_password_url) if clicked_cancel?
    end

    def set_recovery_session
      recover_account_entry_point = starting_point
      reset_session
      start_session("recover_account")
      session[:recovery_account_id] = @recover_account.account.id
      session[:recovery_account_type] = @recover_account.account.sms_tools.active? ? "midden" : "basis"
      session[:recovery_by_letter] = current_account.recovery_codes.by_letter.not_used.not_expired.exists?
      session[:recover_account_entry_point] = recover_account_entry_point
      session[:recover_password_flow] = "E1"
      session[:recovery_method] = recovery_method
      case recover_account_entry_point
      when "herstellen_wachtwoord"
        if current_account.wachtwoordherstel_allowed_with_sms?
          redirect_via_js_or_html(new_request_recover_password_url)
          return
        end

        Log.instrument("577", account_id: current_account.id, hidden: true)
        if current_account.recovery_codes.by_letter.not_used.not_expired.exists?
          redirect_via_js_or_http(existing_recovery_request_url)
        else
          redirect_to(recovery_code_gba_check_url)
        end
      when "herstellen_wachtwoord_code_invoeren"
        redirect_via_js_or_html(new_recover_code_url)
      else
        redirect_to request_recover_password_url
      end
    end
  end
end
