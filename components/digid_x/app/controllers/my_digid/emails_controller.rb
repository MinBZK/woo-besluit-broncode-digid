
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
  class EmailsController < BaseController
    include AppLinkHelper
    include FlowBased
    include AppSessionConcern
    include NsClient

    before_action :check_existing_code_emails, except: [:destroy, :confirm_destroy, :cancel]
    before_action :render_not_found_if_account_deceased

    # GET [mijn.digid.n] /email (D8 - step 1)
    #
    # Allows the user to add/edit his e-mail address.
    def new
      @page_name = "D8"
      @email = session.delete(:email) || MyDigid::Email.new

      session[:flow] = AddEmailFlow.new

      set_flow_redirects
      current_flow[:verified][:redirect_to] = confirm_update_my_digid_email_url

      Log.instrument("540", account_id: current_account.id)
    end

    def create
      @email = MyDigid::Email.new(email_params)
      if @email.valid?
        session[:new_email] = @email.address
        redirect_to my_digid_new_verification_url
      else
        if @email.errors.key?(:address)
          Log.instrument("1357", account_id: current_account.id)
        else
          Log.instrument("541", account_id: current_account.id)
        end

        session[:email] = @email
        redirect_via_js_or_html(new_my_digid_email_url)
      end
    end

    def edit
      @page_name = "D9"
      @page_title = t("titles.D9.my_digid")
      @email = session.delete(:email) || MyDigid::Email.new

      session[:flow] = ChangeEmailFlow.new

      set_flow_redirects
      current_flow[:verified][:redirect_to] = confirm_update_my_digid_email_url

      Log.instrument("130", account_id: current_account.id)
    end

    def cancel
      if ["no_active_app", "app_switch_off"].include?(current_flow[:failed][:reason])
        Log.instrument("1418", human_process: log_process, account_id: current_account.id, hidden: true)
        flash.now[:alert] = t("#{current_flow.process}_via_digid_app_temporarily_not_possible")
        render_simple_message(ok: my_digid_url)
      elsif current_flow.state == :failed && current_app_session.any? && current_app_session(true).dig(:abort_code) == "not_equal_to_current_user_app_id"
        if logged_in_web_to_app?
          Log.instrument("1349", account_id: current_account.id)
          reset_session
          flash.now[:notice] = t("signed_out_digid_app_not_found")
          @link_back = true
          @heading_with_icon = "digid_app_not_found"
          render_simple_message(page_name: "D64")
        end
      else
        redirect_via_js_or_html(my_digid_verification_cancelled_url)
      end
    end

    # PUT /email (D9 - step 1)
    #
    # Checks the given e-mail address and if it is okay, redirects to a screen
    # to validate the user's password
    def update
      @email = MyDigid::Email.new(email_params)

      if deleting_existing_address?
        flash[:notice] = t("use_delete_email_message")
        redirect_via_js_or_html(edit_my_digid_email_url)

      elsif address_unchanged?
        flash[:notice] = t("email_address_unchanged")
        Log.instrument("562")
        redirect_via_js_or_html(my_digid_url)

      elsif @email.valid?
        session[:new_email] = @email.address
        redirect_to my_digid_new_verification_url
      else
        if @email.errors.key?(:address)
          Log.instrument("1357", account_id: current_account.id)
        else
          Log.instrument("131", account_id: current_account.id)
        end

        session[:email] = @email
        redirect_via_js_or_html(edit_my_digid_email_url)
      end
    end

    def confirm_update
      if [:add_email, :change_email].include?(current_flow.process) && current_flow.verified?
        current_account = ::Account.find(session[:account_id])
        if current_account.email_address_present?
          old_address = current_account.adres
          current_account.email.destroy
        end
        current_account.update_attribute(:email, ::Email.create(adres: session[:new_email]))
        current_account.destroy_old_email_recovery_codes do |account|
          Log.instrument("889", account_id: account.id, attribute: "e-mailadres", hidden: true)
        end

        balie_email = (session[:balie] || current_account&.try(:via_balie?))
        EmailControlCodeMailer.new(current_account, balie_email ? "balie" : "mijn").perform

        if old_address.present?
          Log.instrument("133", account_id: current_account.id)
          save_old_address(current_account, old_address)
          NotificatieMailer.delay(queue: "email").notify_email_wijziging(account_id: current_account.id, recipient: old_address)
          pushnotification_type = "PSH05"
        else
          Log.instrument("543", account_id: current_account.id)
          pushnotification_type = "PSH04"
        end

        ns_client.send_notification(current_account.id, pushnotification_type, "", current_account.locale.upcase)
        current_flow.transition_to!(:completed)

        app_session&.complete!

        redirect_to check_email_my_digid_url
      else
        redirect_to my_digid_url, notice: t("email_address_unchanged")
      end
    end

    # GET [mijn.digid.nl] /email/verwijderen (D10)
    def confirm_destroy
      Log.instrument("547", account_id: current_account.id)
      session[:flow] = RemoveEmailFlow.new

      set_flow_redirects
      current_flow[:verified][:redirect_to] = get_destroy_my_digid_email_url
      current_flow[:verify_with_password][:page_name] = "D8"

      @page_name = "D10"

      return redirect_to(my_digid_url) unless current_account.email_address_present?
    end

    # # DELETE [mijn.digid.nl] /email (D10)
    def destroy
      if current_flow.process == :remove_email && current_flow.verified?
        current_account = ::Account.find(session[:account_id])

        NotificatieMailer.delay(queue: "email").notify_email_verwijderen(account_id: current_account.id, recipient: current_account.adres)
        current_account.email.destroy
        current_account.destroy_old_email_recovery_codes do |account|
          Log.instrument("889", account_id: account.id, attribute: "e-mailadres", hidden: true)
        end
        Log.instrument("146", account_id: current_account.id)

        flash[:notice] = t("your_email_address_is_deleted")
        current_flow.transition_to!(:completed)

        app_session&.complete!

        redirect_to my_digid_url
      elsif current_flow.process == :remove_email && current_flow.state == :start
        redirect_to my_digid_new_verification_url
      else
        @page_name = "D10"
        render(:confirm_destroy) unless current_session_is_being_expired_because_account_blocked?
      end
    end

    private

    def address_unchanged?
      current_account.email_address_present? && current_account.adres.casecmp(@email.address.downcase).zero?
    end

    def check_existing_code_emails
      # don't use account#email_scheduled, because it returns a boolean and we
      # need the time of the last scheduled message
      if current_account.max_emails_per_day?(::Configuration.get_int("aantal_controle_emails_per_dag"))
        flash.now[:notice] = t("email_max_verification_mails_changed",
                               max_number_emails: ::Configuration.get_int("aantal_controle_emails_per_dag"),
                               date: l(Time.zone.now.next_day, format: :date).strip)
        Log.instrument("1095", account_id: current_account.id)
        render_simple_message(ok: my_digid_url)
      end
    end

    def deleting_existing_address?
      current_account.email_address_present? && @email.address.blank?
    end

    def save_old_address(account, address)
      account.account_histories.create(
        email_adres: address,
        gebruikersnaam: account.password_authenticator&.username,
        mobiel_nummer: account.phone_number
      )
    end

    def email_params
      params.require(:email).permit(:address)
    end

    def set_flow_redirects
      current_flow[:failed][:redirect_to] = cancel_my_digid_email_url
      current_flow[:cancelled][:redirect_to] = cancel_my_digid_email_url
      current_flow[:verify_with_wid][:abort_url] = my_digid_abort_wid_url
    end
  end
end
