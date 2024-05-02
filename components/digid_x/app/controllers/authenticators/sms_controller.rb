
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

# the SmsController is for the generic screen for checking Sms-codes send
# to the user's mobile phone.
# Since this screen is used in different use cases such as authentication,
# registration and activation, it can be called by doing a
#   redirect_to check_mobiel_url
# Make sure you fill out the appropriate screen mark-up through the session:
#   sms_options = { :return_to => sms_configuration_url,
#                   :call_method => :save_changed_mobile,            optional: method to call when finished
#                   :new_number => params[:account][:mobiel_nummer], optional: overwrite the mobile number if it's not account.mobiel_nummer
#                   :succes_msg => "messages.confirm.nieuw_mobiel" } optional: display success message
module Authenticators
  class SmsController < AuthenticatorsController
    include AuthenticationSession
    include SmsType
    include SmsConcern
    include FlowBased

    before_action :find_account
    before_action :find_recovery_account
    before_action :found_account?
    before_action :check_session_time, only: [:new, :create, :confirm]
    before_action :update_session
    before_action :cancel_sms_button?,     only: [:confirm]
    before_action :activation_block?,      only: [:new, :create]
    before_action :set_sms_header,         only: [:new, :create, :confirm]
    before_action :ensure_from_webservice, only: [:new, :create, :confirm]
    before_action :set_page_title,            only: [:new, :create]
    before_action :choose_sms,             only: [:create]

    # C2E
    def new
      if session[:session] == "mijn_digid" && session[:current_flow].nil?
        return render_not_found
      end

      @page_name = "C2"

      if session[:re_request_new_mobile] == true
        @page_title = t("titles.mijn_digid.re_request_sms.C2")
      elsif session[:current_flow] == :activate_sms_authenticator
        @page_title = t("titles.mijn_digid.activate_sms_authenticator.C2")
      end

      Log.instrument("607", account_id: current_account.id) if current_account
      @smscode = Smscode.new
    end

    # creates a new sms_challenge with a new code
    #  and send it to the user.
    # then render a form
    #  and wait for the user to submit that code.
    # or, if something is wrong
    #  render the blocked msg
    #  or render the too_fast msg
    # has before_actions:
    # - set_sms_header (sets @sms_uitdaging)
    def create
      if @account.blocking_manager.blocked? && !session[:session].eql?("recover_account")
        # already blocked, wait for update_session to unblock
        flash[:notice] = session[:message]
      else

        if session[:session].eql?("activation") && !@account.midden_aanvraag?
          Log.instrument("1306", account_id: session[:account_id])
        end

        set_sms_choice

        if @account.sms_too_fast?(spoken: gesproken_sms_for_session) && !request.referer&.ends_with?("bevestig_telefoonnummer")
          flash.now[:alert] = t("texts.sms_code_too_fast.message", count: @account.next_sms_wait_time(spoken: gesproken_sms_for_session)).html_safe
          Log.instrument("69", account_id: @account.id)
        elsif !request.referer&.ends_with?("bevestig_telefoonnummer")
          sms_service = SmsChallengeService.new account: @account
          @sms_uitdaging = sms_service.create_challenge(
            sms_phone_number: DigidUtils::PhoneNumber.normalize(new_number_for_session),
            sms_gesproken_sms: gesproken_sms_for_session,
            action: session[:session],
            sms_type: sms_type,
            webservice: webservice,
            message_params: sms_message_params,
            flow: session[:current_flow]
          )
          session[:sms_options][:passed?] = false if session[:sms_options].present?
          session[:sms_options] ||= {}
          session[:sms_options][:reference] = "REF-#{@sms_uitdaging.id}"
        end
      end
      flash.keep
      redirect_to action: :new
    end

    # handle the form data for sms activation input
    # if correct code is submitted move user to method confirm_sms_correct
    # else check how many attempts
    #  if max exceeded => block the account
    #  else try again
    # has before_actions:
    # - sms_previous_button?
    # - set_sms_header (sets @sms_uitdaging)
    # - sms_expired?
    # - sms_blank?
    def confirm
      new_number = DigidUtils::PhoneNumber.normalize(session[:sms_options][:new_number]) if session[:sms_options] && session[:sms_options][:new_number]
      @smscode = Smscode.new(sms_params.merge(account: @account,
                                              session_type: session[:session],
                                              new_number: new_number,
                                              spoken: gesproken_sms_for_session,
                                              webservice: webservice))
      if @smscode.use!
        # correct!
        confirm_sms_correct(new_number || @account.sms_challenges.last&.mobile_number)
      else
        set_page_title

        if @sms_uitdaging && @sms_uitdaging.reload.max_exceeded?
          confirm_sms_block_account
        else
          # try again
          render :new
        end
      end
    end

    def can_not_receive_sms
      @page_name = "C27"
    end

    def can_not_receive_sms_continue
      case params[:choice]
      when "log_in"
        Log.instrument("1571")
        redirect_to(sign_in_url)
      when "download"
        Log.instrument(mobile_browser? ? "1572" : "1573", store: ios_browser? ? "App Store" : "Play Store")
        redirect_to(mobile_browser? ? digid_app_store_link : desktop_store_info_link)
      when "re_request_sms"
        Log.instrument("1574")
        redirect_to(my_digid_url)
      when "request"
        Log.instrument("1575")
        redirect_to(new_registration_url)
      end
    end

    private
    # check to see if the use selected the cancel button
    # send along the return path
    def cancel_sms_button?
      if session[:sms_options].present? && session[:sms_options][:cancel_to].present?
        if clicked_cancel? && session[:sms_options][:instant_cancel]
          current_account&.pending_sms_tool&.destroy
          logger.info session[:sms_options][:succes_msg]
          Log.instrument("165", account_id: session[:account_id]) if session[:session] == "mijn_digid" && session[:sms_options][:succes_msg] =~ /sms_uitbreiding/
          session.delete(:current_flow)
          redirect_to session[:sms_options][:cancel_to]
        else
          cancel_button(return_to: authenticators_check_mobiel_url, confirm_to: session[:sms_options][:cancel_to])
        end
      else
        cancel_button(return_to: authenticators_check_mobiel_url)
      end
    end

    # user submitted a correct code for sms
    # delete all previous faulty challenges
    def confirm_sms_correct(phone_number=nil)
      # delete all previous challenges for this account
      @account.sms_challenges.incorrect.delete_all

      # reset blocking only in the following flows. Successful sms logins in mijn_digid and in the registration flow
      # do not have any consequences for blocking or the number of failed logins already on record
      @account.blocking_manager.reset! if %w(sign_in activation).include? session[:session]

      if session[:sms_options].present? # && !session[:sms_options][:passed?].nil?
        session[:sms_options][:passed?] = true
        session[:change_mobile_flow] << "|sms" if session[:change_mobile_flow]
        session[:recover_password_flow] = add_flow_step(session[:recover_password_flow], "|C2") if session[:recover_password_flow]

        reference = session[:sms_options][:reference]
        SmsChallengeService.new(account: @account).send_conversion(reference: reference, phone_number: phone_number) if reference.present?
      end

      Log.instrument("511", account_id: @account.id)

      fan_out
    end

    # Maximum attempts for sms challenge/response reached:
    #  set sms status to "incorrect"
    #  block the account
    #  render a message
    def confirm_sms_block_account
      timestamp_of_first_failed_sms_login_attempt_for_this_flow = @account.time_stamp_of_first_sms_challenge(session[:session])

      if session_is_recover_account?
        # the recover_account flow has a specific treatment in the sense that
        # this flow can be chosen with an account that is already blocked
        if @account.blocking_manager.blocked?
          ::Configuration.get_int("aantal_invoerpogingen_gebruikersnaam_bsn").times do
            RecoveryAttempt.create(attempt: @account.password_authenticator.username, attempt_type: "username")
          end
          session[:session] = "not_recover_account" # so we trigger the message
          @sms_uitdaging.update(attempt: 0, status: ::SmsChallenge::Status::INCORRECT)
          @account.blocking_manager.reset!
        end
        @account.blocking_manager.register_external_blocking_failure_with_given_start_time(timestamp_of_first_failed_sms_login_attempt_for_this_flow)
        @account.void_last_sms_challenge_for_action(session[:session])
      elsif !@account.blocking_manager.blocked?
        @account.blocking_manager.register_external_blocking_failure_with_given_start_time(timestamp_of_first_failed_sms_login_attempt_for_this_flow)
        @account.void_last_sms_challenge_for_action(session[:session])
      end

      account_blocked_keep_session? do
        payload = { account_id: @account.id }
        payload[:webservice_id] = webservice.id if webservice
        Log.instrument("64", payload)
      end
    end

    # when authentication exists and the session equals sign_in
    #   show the check_mobiel (sms#create) page and proceed
    # else
    #   render message 'U bent reeds ingelogd.'
    def ensure_from_webservice
      return unless session[:session].eql?("sign_in") && (session[:authentication] || {})[:webservice_id].blank?

      flash.now[:notice] = I18n.t("messages.sms.already_signed_in")
      render_simple_message
    end

    # let's not proceed if no account has been found
    def found_account?
      raise SessionExpired unless @account
    end

    # sets the page title for check_mobiel screen
    # and gets the last sms_challenge
    def set_sms_header
      if session[:session].eql?("mijn_digid") && session[:mydigid_logged_in] && session[:sms_options].blank?
        redirect_to my_digid_url
      end

      page_step = session[:sms_options][:step] if session.key?(:sms_options) && session[:sms_options].key?(:step)
      page_step = :front_desk if !page_step && session[:session].eql?("registration") && balie_session?

      set_page_name_for_sms_not_received
      @page_title     = t(@page_name, scope: [:titles, session[:session], page_step])
      @page_header    = t(@page_name, scope: [:headers, session[:session], page_step])
      @sms_uitdaging = @account.sms_challenge action: session[:session], webservice: webservice, spoken: gesproken_sms_for_session
    end

    # in case of activation flow;
    # check if this account is blocked
    def activation_block?

      return unless session[:session].eql? "activation"

      if (@account.sms_tools.empty? || @account.sms_tools.active?) && !@account.mobiel_kwijt_in_progress_activatable?
        flash.now[:notice] = t("activate_sms_no_pending_sms_request")
        return render_simple_message(ok: activate_url, cancel: "https://www.digid.nl")
      end

      authenticator = @account.midden_aanvraag? ? :password_authenticator : :pending_sms_tool
      return unless @account.send(authenticator).reached_attempts_limit?
      block_activation(@account.send(authenticator), @account.send(authenticator).number_of_attempts)
    end

    def sms_params
      params.require(:smscode).permit(:smscode)
    end

    def session_is_recover_account?
      session[:session].eql?("recover_account")
    end

    def gesproken_sms_for_session
      if session[:sms_options].present? && session[:sms_options][:gesproken_sms]
        ["1", true].include?(session[:sms_options][:gesproken_sms]) # from the session
      else
        ["registration","activation"].include?(session[:session]) ? @account.pending_gesproken_sms : @account.gesproken_sms # from the active sms_tool (via account)
      end
    end

    def new_number_for_session
      if session[:sms_options].present? && session[:sms_options][:new_number]
        session[:sms_options][:new_number] # from the session
      else
        ["registration","activation"].include?(session[:session]) ? @account.pending_phone_number : @account.phone_number # from the active sms_tool (via account)
      end

    end

    def set_page_title # rubocop:disable CyclomaticComplexity,PerceivedComplexity
      if params[:sms_keuze] && session[:sms_options]
        if session[:sms_options][:step] == :request_sms_code_verification && session[:re_request_new_mobile] == true
          @page_title = t("titles.mijn_digid.re_request_sms.C2D")
        elsif (session[:sms_options][:step] == :activate_sms_authenticator)
          @page_title = t("titles.mijn_digid.activate_sms_authenticator.C2D")
        end
      end
    end
  end
end
