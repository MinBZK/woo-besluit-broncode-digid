
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

# AccountsController;
# contains methods for setting up and processing webforms
# that are concerned with creating an Account
#
class AccountsController < ApplicationController
  include FlowBased

  before_action :find_account
  before_action :check_session_time
  before_action :update_session
  before_action :set_header_new,        only: [:new, :update]
  before_action :update_other_buttons?, only: [:update]

  # GET /accounts/new
  # set the chosen account_type
  # from here we render either:
  #  views/accounts/new
  def new
    Account.transaction do
      current_flow.transition_to!(:new_account)
      dispatch_on_balie_account(current_registration.burgerservicenummer) if balie_session? && !session[:account_id]

      registration = Registration.find(session[:registration_id])
      registration.update_attribute(:status, ::Registration::Status::INITIAL)
      bsn = registration.burgerservicenummer
      if session[:account_id]
        @account = Account.find_by(id: session[:account_id])
      else
        # create an account
        @account             = Account.create_initial(sectornummers(bsn),
                                                      web_registration_id: session[:web_registration_id], registration_id: session[:registration_id])
        session[:account_id] = @account.id
      end

      if clicked_ok? || clicked_cancel?
        registration.update_attribute(:status, Registration::Status::ABORTED)
        @account.destroy
        session.delete(:account_id)
        redirect_to(balie_session? ? new_balie_registration_url : new_registration_url) if clicked_ok?
        cancel_button(return_to: new_account_url) if clicked_cancel?
      else
        build_associations
      end
    end
  end

  # PUT /accounts/1
  # try to process the form, if all is well:
  #  complete the associated email object with the submitted email address
  #  send an email
  # move on with the process (either check email / check_mobiel / finished)
  # when a form validation fails, go back to the form with error messages
  def update
    current_flow.transition_to!(:new_account_verified)

    blank_password_fields?

    if (balie_session? || web_registration_not_oeb?) && params[:account][:email_attributes][:adres].blank?
      @account.assign_attributes(account_params)
      build_associations
      if params[:account][:email_attributes][:adres].blank?
        @account.email.errors.add(:adres, I18n.t("activerecord.errors.messages.blank", attribute: I18n.t("email_address")))
        Log.instrument("36", account_id: @account.id)
      end
      if params[:account][:sms_tools_attributes][:phone_number].blank?
        @account.sms_tools.first.errors.add(:adres, I18n.t("activerecord.errors.messages.blank", attribute: I18n.t("phone_number")))
        Log.instrument("36", account_id: @account.id)
      end
    elsif (@account.password_authenticator.blank? || @account.password_authenticator.errors.empty?) && @account.update(account_params)
      Log.instrument("1359", account_id: @account.id) if account_params["email_attributes"]["no_email"] == "1"
      if @account.email_address_present?
        EmailControlCodeMailer.new(@account, balie_session? ? "balie" : "mijn").perform
      end

      Log.instrument("34", account_id: @account.id)
      logcode = @account.pending_sms_tool.blank? ? 29 : 31
      Log.instrument(logcode, account_id: @account.id)
      Log.instrument("33", account_id: @account.id) if @account.pending_sms_tool&.gesproken_sms == "1"
      dispatch_after_account_creation
      return
    else
      build_associations
    end

    Account.log_registration_errors(@account.errors, @account.id)

    if @account.errors.details[:"sms_tools.phone_number"].any? { |x| x.dig(:error) == t("you_have_reached_the_mobile_numbers_maximum") }
      Log.instrument("45", account_id: @account.id)
    end

    if @account.errors.details[:"password_authenticator.username"].any? { |x| x.dig(:error) == :unique }
      Log.instrument("44", account_id: @account.id)
      if @account.username_registration_attempts.count >= ::Configuration.get_int("pogingen_username_registratie")
        @account.username_registration_attempts.destroy_all
        reset_session
        flash.now[:notice] = t("username_registration_attempt_limit_reached").html_safe
        render_simple_message(ok: new_registration_url)
        return
      else
        @account.username_registration_attempts.create!
      end
    end

    render(:new)
  end

  # setup the final screen
  def confirm_account # rubocop:disable Metrics/PerceivedComplexity,Metrics/CyclomaticComplexity
    current_flow.skip_sms_verification! if session[:sms_options] && session[:sms_options][:passed?]
    current_flow.skip_email_verification! if session[:check_email_options] && session[:check_email_options][:passed?]

    current_flow.transition_to!(:completed)

    session.delete(:sms_options)
    session.delete(:check_email_options)

    # rubocop:disable Metrics/BlockNesting
    if @account.confirm session
      @url = WebRegistration.get_webdienst_url(session).presence
      @page_name = "A8"
      @page_title = @account.via_balie? ? I18n.t("titles.A8B") : I18n.t("titles.A8")

      log_confirm_account
      @account.update_attribute(:zekerheidsniveau, ::Account::LoginLevel::PASSWORD)

      @account.password_authenticator.update(status: Authenticators::Password::Status::PENDING) unless balie_session?

      if @account.email_activated?
        if balie_session?

          sms_service = SmsChallengeService.new(account: @account)
          @account.with_language do
            sms_service.send_sms(
              message: t("sms_message.#{::Configuration.get_boolean("balie_aanvraag_voor_rni") ? "SMS28" : "SMS01"}", code: current_registration.baliecode),
              code_gid: current_registration.to_global_id,
              sms_phone_number: @account.pending_phone_number,
              spoken: false
            )
          end

          Log.instrument("500", account_id: @account.id)
          if @account.email_address_present?
            AanvraagVoltooidMailer.delay(queue: "email").aanvraag_voltooid_buitenland(
              account_id: @account.id, recipient: @account.adres, baliecode: current_registration.baliecode
            )
          end
          nationality = Registration.find(session[:registration_id]).try(&:nationality).try(&:id)
          Log.instrument("428", account_id: @account.id, nationality_id: nationality)
        elsif @account.email_address_present?
          if svb_session?(@account)
            AanvraagVoltooidMailer.delay(queue: "email").aanvraag_voltooid_svb(account_id: @account.id, recipient: @account.adres)
          elsif @account.oeb.present?
            AanvraagVoltooidMailer.delay(queue: "email").aanvraag_voltooid_oep(account_id: @account.id, recipient: @account.adres)
          else
            AanvraagVoltooidMailer.delay(queue: "email").aanvraag_voltooid(
              account_id: @account.id, recipient: @account.adres, beveiligde_bezorging: current_delivery_postcode_marked_for_beveiligde_bezorging?
            )
          end
        end
      end
      notify_old_account
    end
    @account&.pending_sms_tool&.update times_changed: 0

    # rubocop:enable Metrics/BlockNesting
    complete_flow
    reset_session
  end

  private

  def issuer_type
    postcode = Registration.find_by_id(session[:registration_id]).try(:postcode) if session[:registration_id]

    if session[:web_registration_id] && WebRegistration.find_by(id: session[:web_registration_id])
      "letter_international"
    elsif balie_session?
      "front_desk"
    elsif BeveiligdeBezorgingPostcodeCheck.new(postcode).positive?
      "letter_secure_delivery"
    else
      "letter"
    end
  end

  def build_associations
    @account.build_email unless @account.email_address_present?
    @account.sms_tools.build if @account.sms_tools.empty?
  end

  # Check if the first step of the request is OK, by checking if incoming request is from SVB or Balie or GBA Status is OK
  def first_step_completed?
    # webregistraties (zoals SVB en OEB) zijn altijd goed
    return true if session[:web_registration_id] && WebRegistration.find_by(id: session[:web_registration_id])

    gba_status = current_registration.try(:gba_status) || ""
    if balie_session?
      ["rni", "emigrated", "ministerial_decree"].include?(gba_status)
    else
      gba_status == "valid"
    end
  end

  def notify_old_account
    Account.account_already_exists(@account.bsn, Sector.get("bsn")).each do |old_account|
      next unless old_account&.email_activated?

      if old_account.adres != @account.email.try(:adres)
        NotificatieMailer.delay(queue: "email").notify_heraanvraag(account_id: old_account.id, recipient: old_account.adres)
      else
        Log.instrument("399", account_id: @account.id)
      end
    end
  end

  def account_params
    params.require(:account)
          .permit(email_attributes: [:adres, :no_email], sms_tools_attributes: [:phone_number, :gesproken_sms],
                  password_authenticator_attributes: [:username, :password, :password_confirmation]).tap do |p|
      p["sms_tools_attributes"]["0"]["id"] = @account.pending_sms_tool.id if @account.pending_sms_tool.present?
      p["sms_tools_attributes"]["0"]["issuer_type"] = issuer_type
      p["password_authenticator_attributes"]["issuer_type"] = issuer_type
      p["locale"] = I18n.locale
    end
  end

  # Initiates the account and sends user to new account form.
  def dispatch_on_balie_account(bsn)
    current_registration.update_attribute(:status, ::Registration::Status::INITIAL)

    sectornumbers = [[Sector.get("bsn"), bsn]]
    a_number = Registration.get_a_nummer(session[:registration_id])
    sectornumbers << [Sector.get("a-nummer"), a_number] if a_number

    @account = Account.create_initial_balie(sectornumbers)
    session[:account_id] = @account.id
  end

  # When creating an account, attach the sectors.
  # Add bsn as a sectorcode to this account.
  # Then go into the gba data (stored in activation_letter), to retrieve a-nummer
  def sectornummers(bsn)
    sectornummers = []
    if session[:web_registration_id]
      web_reg = WebRegistration.find(session[:web_registration_id])
      sectornummers << [web_reg.sector_id, bsn]
      sectornummers << [Sector.get("a-nummer"), web_reg.anummer] if web_reg.anummer
    else
      sectornummers << [Sector.get("bsn"), bsn]
      a_nummer = Registration.get_a_nummer(session[:registration_id])
      sectornummers << [Sector.get("a-nummer"), a_nummer] if a_nummer
    end
    sectornummers
  end

  def log_confirm_account
    sector_ids = Sector.fetch(Sector.get("bsn"))
    if Account.count_account_already_exists(current_registration.burgerservicenummer, sector_ids) > 0
      if balie_session?
        Log.instrument("501", account_id: @account.id, balie_id: session[:balie_id])
      else
        Log.instrument("55", account_id: @account.id)
      end
    elsif balie_session?
      Log.instrument("429", account_id: @account.id, balie_id: session[:balie_id])
    else
      Log.instrument("54", account_id: @account.id)
    end
    Log.instrument("49", account_id: @account.id) if params["skip_code"]
  end

  # after account creation, the user either goes to:
  # 1) basic with e-mail => email_check => done
  # 2) basic without e-mail => done
  # 3) middle => check_mobiel
  def dispatch_after_account_creation
    if @account.email_address_present?
      session[:check_email_options] = { return_to: confirm_account_url }
      path = check_email_url
    else
      session[:check_email_options] = { passed?: true }
      path = confirm_account_url
    end

    if @account.pending_phone_number.present?
      session[:sms_options] = { return_to: path } # legacy code for shared sms challenge
      path = authenticators_check_mobiel_url # override path
    else
      session[:sms_options] = { passed?: true }
    end

    redirect_to(path)
  end

  def set_header_new
    @page_name = balie_session? ? "A4B" : "A4"
  end

  def current_delivery_postcode
    current_registration&.postcode
  end

  def current_delivery_postcode_marked_for_beveiligde_bezorging?
    BeveiligdeBezorgingPostcodeCheck.new(current_delivery_postcode).positive?
  end

  helper_method :current_delivery_postcode_marked_for_beveiligde_bezorging?

  # handles previous and cancel button from digid_gegevens
  def update_other_buttons?
    if clicked_ok?
      redirect_to new_account_url
    elsif clicked_cancel?
      cancel_button(return_to: new_account_back_url)
    end
  end

  def blank_password_fields?
    { password: "password", password_confirmation: "repeat_password" }.each do |field, label|
      if params[:account][:password_authenticator_attributes][field].blank?
        @account.password_authenticator.errors.add(field, t("activerecord.errors.messages.blank", attribute: t(label)))
      end
    end
  end
end
