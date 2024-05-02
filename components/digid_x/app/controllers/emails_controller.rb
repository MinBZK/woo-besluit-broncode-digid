
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

class EmailsController < ApplicationController
  include EmailConcern
  include FlowBased

  def edit
    check_existing_code_emails
    @page_name = "D9"
    @page_title = t(balie_session? ? "titles.D9.not_my_digid_balie" : "titles.D9.not_my_digid")
    @email = session.delete(:email) || Email.new

    current_flow.transition_to!(:change_email)

    Log.instrument("130", account_id: current_account.id)
  end

  def cancel
    Log.instrument("132", account_id: current_account.id)

    redirect_to check_email_url
  end

  # PUT /email (D9 - step 1)
  #
  # Checks the given e-mail address and if it is okay, redirects to a screen
  # to check the email address by sending a code
  def update
    @email = Email.new(email_params)

    if address_unchanged?
      flash[:notice] = t("email_address_unchanged")
      Log.instrument("562")
      redirect_via_js_or_html(edit_email_url)

    elsif @email.valid?
      current_account.email.adres = @email.adres
      EmailControlCodeMailer.new(current_account, balie_session? ? "balie" : "mijn").perform
      redirect_via_js_or_html(check_email_url)

    else
      if @email.errors.key?(:adres)
        Log.instrument("1357", account_id: current_account.id)
      else
        Log.instrument("131", account_id: current_account.id)
      end

      session[:email] = @email
      redirect_via_js_or_html(edit_email_url)
    end
  end

# C25
  def email_confirmation_expired
    @page_header = t("headers.sign_in.C25")
    @page_name = "sign_in.C25"
    @user_email = current_account.email.adres
    @confirm = Confirm.new
  end

  def email_confirmation_expired_redirect
    session[:email_confirmation_expired_showed] = true
    # question: is e-mail correct?
    @confirm = Confirm.new(confirm_params)
    if @confirm.yes?
      Log.instrument("1401", account_id: current_account.id)
      current_account.email.touch(:confirmed_at)
      redirect_to handle_email_temptation_url
    else
      Log.instrument("1402", account_id: current_account.id)
      redirect_to ask_for_email_change_url
    end
  end

  def ask_for_email_change
    @page_header = t("headers.sign_in.C26")
    @page_name = "sign_in.C26"
    @confirm = Confirm.new

    webservice = get_auth_webservice
    
    if !webservice.try(:is_mijn_digid?)
      @webservice = webservice&.name
      @show_cancel_authentication_warning = true
    else
      @show_cancel_authentication_warning = false
    end
  end

  def ask_for_email_change_redirect
    # question: want to change e-mail?
    @confirm = Confirm.new(confirm_params)
    if @confirm.yes?
      Log.instrument("1404", account_id: current_account.id)
      webservice = get_auth_webservice

      session[:redirect_change_email] = true

      if !webservice.try(:is_mijn_digid?)
        redirect_to email_update_redirect_url
      else
        redirect_to handle_email_temptation_url
      end
    else # refuses to update e-mail, ask again in 18 months
      Log.instrument("1403", account_id: current_account.id)
      current_account.email.touch(:confirmed_at)
      redirect_to handle_email_temptation_url
    end
  end

# C23
  def email_confirmation_nonexistent
    # question: want to add e-mail?
    @page_header = t("headers.sign_in.C23") 
    @page_name = "sign_in.C23"

    webservice = get_auth_webservice

    if !webservice.try(:is_mijn_digid?)
      @webservice = webservice&.name
      @show_cancel_authentication_warning = true
      @remote = true
    else
      @show_cancel_authentication_warning = false
      @remote = false
    end

    @confirm = Confirm.new
  end

  def email_confirmation_nonexistent_redirect
    session[:email_confirmation_nonexistent_showed] = true

    @confirm = Confirm.new(confirm_params)
    if @confirm.no? #redirect to ask again
      redirect_to ask_for_email_add_skip_url
    else
      webservice = get_auth_webservice

      Log.instrument("1536", account_id: current_account.id)
      if current_account.email_not_activated?
        session[:redirect_change_email] = true
        if !webservice.try(:is_mijn_digid?)
          redirect_to email_update_redirect_url
        else
          redirect_to handle_email_temptation_url
        end
      else
        session[:redirect_add_email] = true
        if !webservice.try(:is_mijn_digid?)
          redirect_to email_new_redirect_url
        else
          redirect_to handle_email_temptation_url
        end
      end
    end
  end

  def ask_for_email_add_skip
    @page_header = t("headers.sign_in.C23")
    @page_name = "sign_in.C23"
    @confirm = Confirm.new

    webservice = get_auth_webservice

    if !webservice.try(:is_mijn_digid?)
      @webservice = webservice&.name
      @show_cancel_authentication_warning = true
      @remote = true
    else
      @show_cancel_authentication_warning = false
      @remote = false
    end
  end

  def ask_for_email_add_skip_redirect
    # question(for a second time): want to add e-mail?
    @confirm = Confirm.new(confirm_params)
    if @confirm.no?
      webservice = get_auth_webservice

      Log.instrument("1536", account_id: current_account.id)
      if current_account.email_not_activated?
        session[:redirect_change_email] = true
        if !webservice.try(:is_mijn_digid?)
          redirect_to email_update_redirect_url
        else
          redirect_to handle_email_temptation_url
        end
      else
        session[:redirect_add_email] = true
        if !webservice.try(:is_mijn_digid?)
          redirect_to email_new_redirect_url
        else
          redirect_to handle_email_temptation_url
        end
      end
    else # refuses to add email, ask again in 6 months
      current_account.touch(:email_requested)
      Log.instrument("1538", account_id: current_account.id)
      redirect_to handle_email_temptation_url
    end
  end

  private

  def email_params
    params.require(:email).permit(:adres)
  end

  def address_unchanged?
    current_account.email_address_present? && current_account.adres.casecmp(@email.adres.downcase).zero?
  end

  def check_existing_code_emails
    return unless current_account.max_emails_per_day?(::Configuration.get_int("aantal_controle_emails_per_dag"))

    Log.instrument("1095", account_id: current_account.id)
    max_emails_per_day_error
  end

  def get_auth_webservice
    webservice = nil
    if !session[:authentication].nil? && !Webservice.from_authentication_session(session[:authentication]).nil?
      webservice = Webservice.from_authentication_session(session[:authentication])
    else 
      webservice = Webservice.find_by(id: (session["saml.provider_id"]))
    end
    webservice
  end
end
