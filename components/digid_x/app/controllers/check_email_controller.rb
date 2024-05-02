
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

# the CheckEmailController is a generic screen for checking email-controlecodes
#
# Since this screen is used in different use cases such as registration and My DigiD, it can be called by doing a
#   redirect_to check_email_url
# Make sure you fill out the appropriate screen mark-up through the session:
#   email_options = { :return_to => xxx_url }
class CheckEmailController < ApplicationController
  include FlowBased

  before_action :find_account
  before_action :found_account?
  before_action :check_session_time
  before_action :update_session
  before_action :email_other_buttons?, only: :confirm_email
  before_action :check_flow, only: :check_email

  # setup the form for email control code
  # views/accounts/check_email
  # the form submit button points to method confirm_account
  def check_email
    page_step = :front_desk if session[:session].eql?("registration") && balie_session?

    @page_name   = "A7"
    @page_title  = t(@page_name, scope: [:titles, session[:session], page_step])
    @page_header = t(@page_name, scope: [:headers, session[:session], page_step])

    set_step_counter

    if @account.email.reached_attempts_limit? && !session[:skip_step_check_email]
      show_error_message_wrong_code_x_times
    elsif session[:check_email_options]
      session[:check_email_options][:passed?] = false
    end
    session.delete(:skip_step_check_email)
  end

  # confirm the account:
  # check if a controle code is submitted
  # check if correct controle code is submitted
  #  when not correct:
  #    count attempts and redirect to either max_reaches or try_again
  #  when correct
  #    delete all precious attempts
  #    check expiration date
  #    if not ok: show a msg and terminate
  #    if ok:
  #      set email status to "checked"
  #      and confirm the account
  #      and send an email
  def confirm_email
    if email_code.valid?
      @account.email.delete_attempts

      # everything ok
      # set email to 'checked'
      @account.email.update(status: ::Email::Status::VERIFIED, confirmed_at: Time.zone.now)
      flash[:notice] = I18n.t("messages.confirm.email_checked")

      if session[:check_email_options].present? && !session[:check_email_options][:passed?].nil?
        session[:check_email_options][:passed?] = true
      end

      if session[:session] == :registration
        Log.instrument("97", account_id: @account.id)
        current_flow.transition_to!(:verify_email_complete)
      else
        Log.instrument("145", account_id: @account.id)
      end
      redirect_url = session[:check_email_options][:return_to]
      cleanup_session

      redirect_to redirect_url
    elsif (days = @account.email.expired?)
      @account.email.delete_attempts
      # expired
      # do not record this as an email_attempt
      Log.instrument("557", account_id: @account.id)
      flash[:notice] = t("code_is_no_longer_valid", count: days)
      render_message(button_to: session[:check_email_options][:return_to])
    elsif @account.email.reached_attempts_limit? || @account.email.status == ::Email::Status::BLOCKED
      show_error_message_wrong_code_x_times
    else # just faulty
      page_step = :front_desk if session[:session].eql?("registration") && balie_session?
      @page_name = "A7"
      @page_title = t("A7", scope: [:titles, session[:session], page_step])
      @page_header = t("A7", scope: [:headers, session[:session], page_step])

      set_step_counter

      render :check_email
    end
  end

  private

  def set_step_counter
    if URI(request.referer || "").path != "/"
      @current_step, @total_steps = if logged_in_with_app? && !logged_in_web_to_app?
        [6,6]
      elsif logged_in_with_app? && logged_in_web_to_app?
        [2,2]
      elsif logged_in_with_wid? && !logged_in_with_desktop_wid? && !logged_in_web_to_app?
        [5,5]
      else
        [3,3]
      end
    end
  end

  def show_error_message_wrong_code_x_times
    if session[:session] == "registration"
      notice_text = balie_or_svb_session? ? "you_have_entered_the_wrong_code_x_times_balie" : "you_have_entered_the_wrong_code_x_times_general"
      url = session[:check_email_options][:cancel_to]
      url ||= balie_or_svb_session? ? "https://www.digid.nl" : confirm_account_path
      session[:skip_step_check_email] = true
      Log.instrument("51", account_id: @account.id)
    elsif session[:session] == "mijn_digid"
      notice_text = "you_have_entered_the_wrong_code_x_times_my_digid"
      url = session[:check_email_options][:return_to]
      Log.instrument("141", account_id: @account.id)
    end

    flash.now[:notice] = t(notice_text || "you_have_entered_the_wrong_code_x_times_general", x: @account.email.number_of_attempts)
    render_message(button_to: url || APP_CONFIG["urls"]["external"]["digid_home"], button_to_options: { method: :get }, no_cancel_to: true)
  end

  def check_flow
    if session[:session].to_sym == :registration
      if session[:sms_options] && session[:sms_options][:passed?]
        current_flow.skip_sms_verification!
      end
      current_flow.transition_to!(:verify_email)
    end
  end

  # no account found, so route to home
  def found_account?
    redirect_to(home_url) if @account.nil?
  end

  # check to see if the user selected the previous button
  # since there are 3 ways to get to the email confirm screen,
  # we must redirect to 3 different screen depending on
  # the account type in case of the previous button
  # in case user selected cancel, handle that
  def email_other_buttons?
    if clicked_skip?
      redirect_to(confirm_account_url(skip_code: true))
    elsif clicked_cancel?
      Log.instrument("644", account_id: current_account.id)
      if session[:check_email_options] && session[:check_email_options][:instant_cancel]
        redirect_to(session[:check_email_options][:cancel_to])
      else
        cancel_button(return_to: check_email_url)
      end
    end
  end

  def email_code
    @email_code ||= if params[:email_code].present?
                      EmailCode.new(email_code_params.merge(account: @account))
                    else
                      EmailCode.new(account: @account)
                    end
  end
  helper_method(:email_code)

  def email_code_params
    params.require(:email_code).permit(:code)
  end

  def cleanup_session
    session.delete(:check_email_options)
  end
end
