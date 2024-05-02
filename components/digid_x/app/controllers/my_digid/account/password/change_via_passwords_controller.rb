
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
      class ChangeViaPasswordsController < ::MyDigid::Account::Password::PasswordsController
        include PasswordConcern
        include FlowBased

        before_action :return_if_no_active_password_tool, only: [:show, :update]
        before_action :render_not_found_if_account_deceased

        # GET [mijn.digid.nl] /wachtwoord_wijzigen
        def show
          if (!digid_app_enabled? || !current_account.app_authenticator_active?) && current_account.blocking_manager.blocked?
            Log.instrument("1412", account_id: current_account.id)
            return render_blocked(i18n_key: "password_blocked_changing_password_not_allowed",
                                  page_name: "G3",
                                  show_message: true,
                                  show_expired: false)
          end

          if flow_active?
            current_flow.transition_to!(:with_password)
          else
            session[:flow] = ChangePasswordFlow.new
          end

          @page_name = "D7"
          Log.instrument("134", account_id: current_account.id)
          Log.instrument("895", account_id: current_account.id)
        end

        # D7: "DigiD: Mijn DigiD | Wijzigen wachtwoord"
        def update
          try_to_change_password unless current_session_is_being_expired_because_account_blocked?
        end

        protected

        def current_password_check
          current_account.password_authenticator.verify_password(params[:password_changing_account][:current_password])
        end

        def password_attributes
          params[:password_changing_account][:current_password].blank? || params[:password_changing_account][:password].blank? || params[:password_changing_account][:password_confirmation].blank?
        end

        def change_password
          current_account.password_authenticator.change_password(params[:password_changing_account][:password], params[:password_changing_account][:password_confirmation]) # correct current password
        end

        def do_redirect_for_password_change
          current_flow.transition_to!(:completed)
          do_redirect_after_change_password
        end
      end
    end
  end
end
