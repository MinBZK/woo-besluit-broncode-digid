
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
  module Verifications
    class PasswordVerificationsController < BaseController
      include FlowBased
      include RdwClient
      include RvigClient
      include HoogSwitchConcern
      include VerificationConcern

      before_action :load_and_transition_flow

      def new
        set_page_title_and_page_header("D11", current_flow)
        @password_verification = PasswordVerification.new(account: current_account)
        render :new, variant: current_flow.process
      end

      def create
        @password_verification = PasswordVerification.new(password_verification_params.merge(account: current_account))

        if @password_verification.valid?
          current_account.register_authentication
          current_account.blocking_manager.reset!
          Log.instrument("1346", account_id: current_account.id)
           # WORKAROUND to stay backwards compatible with old flow spec
          if current_flow[:verified].present?
            current_flow.transition_to!(:verified)
          else
            current_flow.complete_step!(:verify_password, controller: self)
          end
          redirect_to current_flow.redirect_to
        elsif current_account.blocking_manager.blocked?
          Log.instrument("1416", human_process: log_process, account_id: current_account.id)
          reset_session
          reset_flow
          render_blocked(i18n_key: "middel_blocked_until", page_name: "G4", show_message: false, show_expired: true)
        else
          Log.instrument("1417", human_process: log_process, account_id: current_account.id)
          set_page_title_and_page_header("D11", current_flow)
          render :new, variant: current_flow.process
        end
      end

      private

      def load_and_transition_flow
        if current_flow[:verified].present?  # WORKAROUND to stay backwards compatible with old flow spec
          current_flow.transition_to!(:verify_with_password)
        else
          current_flow.transition_to!(:verify_password)
        end
      end

      def password_verification_params
        params.require(:password_verification).permit(:password)
      end
    end
  end
end
