
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
  # this controller implements entries for recovery codes for passwords
  # two flows exists:
  # - E1 -> E4 -> E6 -> E7 (authenticate -> recovery code -> new password -> done)
  # - RequestRecoverPasswords flow -> E4 -> C2 -> E6 -> E7 (authenticate -> recovery code -> sms -> new password -> done)
  class RecoverPasswordsController < RecoverAccountsController
    before_action :check_session_time,      except: [:index]
    before_action :update_session,          except: [:index]
    before_action :commit_cancelled?

    before_action :flow_check,              except: [:index]

    before_action :blank?,                  only: [:update]
    before_action :check_cookie_blocked?,   only: [:index]

    # GET /herstellen
    # GET /herstellen/code_invoeren
    def index
      reset_session
      session[:recover_account_entry_point] = "herstellen_wachtwoord_code_invoeren"
      redirect_to recover_authentication_url
    end

    # GET /herstellen/nieuw_wachtwoord
    def edit
      if session[:sms_options].present? && !session[:sms_options][:passed?]
        redirect_to(authenticators_check_mobiel_url(url_options))
      else
        @page_name = "E6"
        @authenticator = current_account.password_authenticator
      end
    end

    # POST /herstellen/nieuw_wachtwoord
    def update
      if @authenticator.errors.empty? && @authenticator.update(authenticator_params)
        recovery_code = RecoveryCode.find(session[:recovery_code_id])
        if recovery_code.via_brief
          Log.instrument("204", account_id: current_account.id)
        else
          Log.instrument("213", account_id: current_account.id)
        end
        recovery_code.to_used!
        current_account.blocking_manager.reset!
        NotificatieMailer.delay(queue: "email").notify_wachtwoord_wijziging(account_id: current_account.id, recipient: current_account.adres) if current_account.email_activated?
        @page_name = "E7"
        reset_session
      else
        if @authenticator.errors.include?(:password) || @authenticator.errors.include?(:password_confirmation)
          if @authenticator.errors.include?(:password) && @authenticator.errors.messages[:password].count == 1 && @authenticator.errors.messages[:password][0] == I18n.t("activerecord.errors.models.authenticators/password.attributes.password.confirmation")
            Log.instrument("588", account_id: current_account.id)
          else
            Log.instrument("212", account_id: current_account.id)
          end
        end        
        @page_name = "E6"
        render :edit
      end
    end

    private
    # before_action for new_password
    # checks if current_password or password_confirmation is blank
    def blank?
      @authenticator = current_account.password_authenticator
      @authenticator .errors.add(:password, I18n.t("activerecord.errors.messages.blank", attribute: t("new_password"))) if params[:account_to_recover][:password].blank?
      @authenticator .errors.add(:password_confirmation, I18n.t("activerecord.errors.messages.blank", attribute: t("repeat_password"))) if params[:account_to_recover][:password_confirmation].blank?
    end

    def commit_cancelled?
      cancel_button(return_to: new_recover_password_url) if clicked_cancel?
    end

    def flow_check
      session[:recover_password_flow] ||= ""
      redirect_to recover_passwords_url unless check_order(session[:recover_password_flow])
    end

    def authenticator_params
      params.require(:account_to_recover).permit(:password, :password_confirmation)
    end

    # herstellen_wachtwoord_code_invoeren, allowed: %w(E1,E4)
    # herstellen_wachtwoord_code_invoeren_via_email %w(E1,E4,C2), %w(E1,E2,E4,C2)
    def check_order(flow)
      case starting_point
      when "herstellen_wachtwoord_code_invoeren"
        flow.eql?("E1|E4") || flow.eql?("E1|E4|C2") || flow.eql?("E1|E4|C8")
      when "herstellen_wachtwoord_code_invoeren_via_email"
        flow.eql?("E1|E4|C2") || flow.eql?("E1|E2|E4|C2") || flow.eql?("E1|E4|C8") || flow.eql?("E1|E2|E4|C8")
      else
        false
      end
    end
  end
end

