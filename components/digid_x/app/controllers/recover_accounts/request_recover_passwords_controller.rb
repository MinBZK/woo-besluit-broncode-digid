
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
  # this controller implements requests for recovery codes to recover lost passwords
  # three flows exists:
  # - E1 -> E3 (authenticate -> done)
  # - E1 -> E2 -> E3 (authenticate -> choose letter -> done)
  # - E1 -> E2 -> E4 (authenticate -> choose email -> start RecoverPassword flow)
  class RequestRecoverPasswordsController < RecoverAccountsController
    skip_before_action :check_session_time,       only: [:index]
    skip_before_action :update_session,           only: [:index]
    before_action :commit_cancelled?
    before_action :check_cookie_blocked?, only: [:index]

    # GET /herstellen
    # GET /herstellen/wachtwoord
    def index
      save_the_flash = flash[:notice]
      reset_session
      flash[:notice] = save_the_flash
      session[:recover_account_entry_point] = "herstellen_wachtwoord"
      redirect_to recover_authentication_url
    end

    # GET /herstellen/method
    def new
      @page_name = "E2"
      @recover_account_method = RecoverAccountMethod.new(method: :email)

      render_partial_or_return_json("#recover-account-method-form", :form, :new)
    end

    # POST /herstellen/method
    def create
      @recover_account_method = RecoverAccountMethod.new(create_params)
      if @recover_account_method.method.eql?("letter") && @recover_account_method.valid?
        Log.instrument("197", account_id: current_account.id)
        session[:recovery_by_choice] = true
        session[:recovery_by_letter] = true
        session[:recovery_method] = store_recovery_method
        if current_account.recovery_codes.by_letter.not_used.not_expired.exists?
          redirect_via_js_or_http(existing_recovery_request_url)
        else
          redirect_to(recovery_code_gba_check_url)
        end
      elsif @recover_account_method.method.eql?("email") && @recover_account_method.valid?
        # send the email
        Log.instrument("642", account_id: current_account.id)
        if create_and_send_recovery_code
          Log.instrument("207", account_id: current_account.id)
          session[:recovery_by_choice] = true
          session[:recovery_method] = store_recovery_method
          session[:recover_account_entry_point] = "herstellen_wachtwoord_code_invoeren_via_email"
          session[:recover_password_flow] = add_flow_step(session[:recover_password_flow], "|E2") if session[:recover_password_flow]
          redirect_via_js_or_html(new_recover_code_url)
        else
          Log.instrument("208", account_id: current_account.id)
          @recover_account_method.errors.add(:base, I18n.t("activemodel.errors.models.recover_account.attributes.recovery_email_failure"))
          @page_name = "E2"
          render_partial_or_return_json("#recover-account-method-form", :form, :new)
        end
      else
        session[:recovery_method] = store_recovery_method

        flash[:notice] =  @recover_account_method.errors.full_messages.first
        redirect_via_js_or_http(recover_authentication_error_url)
      end
    end

    def again
      @recover_account_method = RecoverAccountMethod.new(method: "email", account_id: current_account.id)
      if @recover_account_method.valid?
        if create_and_send_recovery_code
          Log.instrument("194", account_id: current_account.id)
          session[:recovery_by_choice] = true
          session[:recovery_method] = store_recovery_method
          redirect_to new_recover_code_url
        else
          Log.instrument("208", account_id: current_account.id)
          @recover_account_method.errors.add(:base, I18n.t("activemodel.errors.models.recover_account.attributes.recovery_email_failure"))
          @page_name = "E2"
          render_partial_or_return_json("#recover-account-method-form", :form, :new)
        end
      else
        redirect_to new_recover_code_url, alert: @recover_account_method.errors.full_messages.join("<br/>")
      end
    end

    # POST /herstellen/success
    def success
      if session[:sms_options].present? && !session[:sms_options][:passed?]
        redirect_to check_mobiel_url
      else
        finish_letters
        set_registration_to_aanvraag
        current_account.delete_recovery_code_attempts

        @page_name = "E3"
        Log.instrument("195", account_id: session[:recovery_account_id])
        Log.instrument("583", account_id: session[:recovery_account_id], hidden: true) # 583
        reset_session
      end
    end

    private
    def create_params
      params.require(:recover_account_method).permit(:method).merge(account_id: current_account.id)
    end

    def commit_cancelled?
      return unless clicked_cancel?

      cancel_button(return_to: new_request_recover_password_url)
    end

    def create_and_send_recovery_code
      code_data = { account_id: current_account.id,
                    recovery_method: "password",
                    code: nil,
                    send_letter_anyway: false,
                    via_brief: false }

      @code = RecoveryCode.new
      @code.send_new_recovery_code(current_account, @code.create_recovery_code(code_data))
    end

    def finish_letters
      return unless session[:registration_id].present?

      letters = ActivationLetter.created.where(registration_id: session[:registration_id])
      letters.each do |letter|
        code = RecoveryCode.new
        code_data = { account_id: session[:recovery_account_id],
                      recovery_method: "password",
                      code: letter.controle_code,
                      send_letter_anyway: true,
                      via_brief: true }
        code.create_recovery_code(code_data)
        letter.update(status: ::ActivationLetter::Status::FINISHED,
                                 letter_type: ActivationLetter::LetterType::RECOVER_PWD,
                                 geldigheidsduur: (code.expiry_date(code_data).to_i / 86_400)
                                )
      end
    end

    def set_registration_to_aanvraag
      registration = Registration.find(session[:registration_id])
      registration.update_attribute(:status, ::Registration::Status::REQUESTED)
    end
  end
end
