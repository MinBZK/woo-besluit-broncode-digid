
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

# This controller is largely duplicated from the PasswordVerificationsController.  There some
# differences that cannot easily be resolved such as a different `page_name` and step name in the
# wizard. We also don't want to allow weird  transition jumps from :verify_password to :finalize in
# the app_authenticator flow.

# TODO: Check if this class is still being used, otherwise remove this class
module MyDigid
  module Verifications
    class RepeatPasswordVerificationsController < BaseController
      include FlowBased
      before_action :load_and_transition_flow

      def new
        @page_name = current_flow.page_name
        @page_title = t(current_flow.page_name, scope: [:titles, session[:session], current_flow.process])
        @password_verification = PasswordVerification.new(account: current_account)
        render :new, variant: current_flow.process
      end

      def create
        @password_verification = PasswordVerification.new(password_verification_params.merge(account: current_account))

        if @password_verification.valid?
          current_account.register_authentication
          current_flow.complete_step!(:repeat_password, controller: self)
          redirect_to current_flow.redirect_to
        elsif current_account.blocking_manager.blocked?
          Log.instrument("1416", account_id: current_account.id, human_process: log_process)
          reset_session
          reset_flow
          render_blocked(i18n_key: "middel_blocked_until", page_name: "G4", show_message: false, show_expired: true)
        else
          @page_name = current_flow.page_name
          @page_title = t(current_flow.page_name, scope: [:titles, session[:session], current_flow.process])
          render :new, variant: current_flow.process
        end
      end

      private

      def load_and_transition_flow
        current_flow.transition_to!(:repeat_password)
      end

      def password_verification_params
        params.require(:password_verification).permit(:password)
      end
    end
  end
end
