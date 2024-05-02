
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
    class SmsVerificationsController < BaseController
      include FlowBased
      include SmsType
      include SmsConcern
      before_action :load_and_transition_flow
      before_action :set_page, only: [:new, :show]
      before_action :choose_sms, only: [:new]

      helper_method :sms_challenge

      def new
        set_sms_choice
        if current_account.sms_too_fast?(spoken: gesproken_sms_for_session)
          flash[:alert] = t("texts.sms_code_too_fast.message", count: current_account.next_sms_wait_time(spoken: gesproken_sms_for_session)).html_safe
          Log.instrument("69", account_id: current_account.id)
        else
          Log.instrument("607", account_id: current_account.id)
          @sms_challenge = sms_service.create_challenge(
            sms_gesproken_sms: gesproken_sms_for_session,
            action: session[:session],
            sms_type: sms_type
          )
          session[:sms_options][:reference] = "REF-#{@sms_challenge.id}"
        end

        redirect_to action: :show
      end

      def show
        @smscode = Smscode.new
        render :show, variant: current_flow.process
      end

      def create
        @smscode = Smscode.new(create_params)

        if @smscode.valid?
          confirm_sms_correct
          current_flow.complete_step!(:verify_sms, controller: self)
          Log.instrument("511", account_id: current_account.id)
          redirect_to current_flow.redirect_to
        elsif sms_challenge.reload.max_exceeded?
          current_account.blocking_manager.register_external_blocking_failure_with_given_start_time(current_account.time_stamp_of_first_sms_challenge(session[:session]))
          current_account.void_last_sms_challenge_for_action(session[:session])
          Log.instrument("1416", account_id: current_account.id, human_process: log_process)
          reset_session
          render_blocked(i18n_key: "middel_blocked_until", page_name: "G4", show_message: false, show_expired: true)
        else
          @page_name = "C2"
          @page_title = t("C2", scope: [:titles, session[:session], current_flow.process])
          render :show, variant: current_flow.process
        end
      end

      private

      def set_page
        set_page_name_for_sms_not_received

        @page_header = t(@page_name, scope: [:headers, session[:session], "apps_sms_verification"]) if params[:sms_keuze]
        @page_title = t(@page_name, scope: [:titles, session[:session], current_flow.process])
      end

      def create_params
        params.require(:smscode).permit(:smscode).merge(account: current_account, session_type: session[:session], spoken: gesproken_sms_for_session)
      end

      # user submitted a correct code for sms
      # delete all previous faulty challenges
      def confirm_sms_correct
        sms_challenge.update_attribute(:status, ::SmsChallenge::Status::CORRECT)
        # delete all previous faulty challenges for this account
        current_account.sms_challenges.incorrect.delete_all
      end

      def load_and_transition_flow
        current_flow.transition_to!(:verify_sms)
      end

      def sms_challenge
        @sms_challenge ||= current_account.sms_challenge action: session[:session]
      end

      def sms_service
        @sms_service ||= SmsChallengeService.new account: current_account
      end
    end
  end
end
