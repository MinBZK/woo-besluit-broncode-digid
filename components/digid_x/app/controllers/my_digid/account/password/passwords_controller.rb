
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
    module Password
      class PasswordsController < BaseController
        protected

        before_action :change_password_blank?, only: [:update]

        # D7: "DigiD: Mijn DigiD | Wijzigen wachtwoord"
        def try_to_change_password
          if !current_password_check
            current_account.blocking_manager.register_failed_attempt!
            Log.instrument("1417", account_id: current_account.id, human_process: log_process) unless current_account.blocking_manager.blocked?
            return if current_session_is_being_expired_because_account_blocked?

            password_authenticator.errors.add(:current_password, I18n.t("activerecord.errors.models.account.attributes.current_password.incorrect"))
            render :show
          elsif change_password
            pw_check = PasswordCheck.new(username: password_authenticator.username, password:  params[:password_changing_account][:password])

            if !pw_check.password_not_username?
              do_redirect_for_password_change
            else
              flash.now[:alert] = pw_check.errors.messages.values.flatten.first.html_safe
              Log.instrument("135", account_id: current_account.id)
              render :show
            end
          else
            if (password_authenticator.errors.messages.keys & %i(current_password password password_confirmation)).any?
              if password_authenticator.errors.has_key?(:password) && password_authenticator.errors.messages[:password].count == 1 && password_authenticator.errors.messages[:password][0] == I18n.t("activerecord.errors.models.authenticators/password.attributes.password.confirmation")
                Log.instrument("136", account_id: current_account.id)
              else
                Log.instrument("135", account_id: current_account.id)
              end
            end
            render :show
          end
        end

        # checks if current_password or password_confirmation is blank
        def change_password_blank? # private
          @page_name = "D7"

          return unless password_attributes

          { current_password: "current_password", password: "new_password", password_confirmation: "repeat_password" }.each do |field, label|
            password_authenticator.errors.add(field, t("activerecord.errors.messages.blank", attribute: t(label))) if params[:password_changing_account][field].blank?
          end
          Log.instrument("135", account_id: current_account.id)
          render :show
        end

        private

        def password_authenticator
          current_account.password_authenticator
        end
      end
    end
  end
end
