
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

module Iapi
  class AppController < ApplicationController
    include MyDigidHelper
    include HsmClient
    include MscClient
    include LogConcern

    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token

    before_action :set_account, only: [:get_account_status, :create_letter, :poll_letter, :send_sms, :confirm_sms, :request_wid,
                                       :send_notification_message, :check_bsn, :activate_account_with_code, :activate_account, :get_bsn]

    def new
      unless params[:ad_session].present?
        render(json: { message: "Missing parameters." }, status: 400)
        return
      end

      ad_session = AdSession.find(session_id: params[:ad_session])

      app_session = App::Session.create(
        flow: "confirm_session",
        state: "AWAITING_QR_SCAN",
        ad_session_id: params[:ad_session],
        authentication_level: ad_session&.authentication_level || 10,
        webservice: Webservice.find(ad_session&.legacy_webservice_id)&.name,
        webservice_id: ad_session&.legacy_webservice_id
      )

      render json: { app_session_id: app_session.id }
    end

    def create_registration
      registration = Registration.create(
        burgerservicenummer: params[:BSN],
        geboortedatum_dag: params[:date_of_birth][6..7],
        geboortedatum_maand: params[:date_of_birth][4..5],
        geboortedatum_jaar: params[:date_of_birth][0..3],
        postcode: params[:postal_code],
        huisnummer: params[:house_number],
        huisnummertoevoeging: params[:house_number_additions],
        gba_status: "pending"
      )

      return render_invalid_parameters(debug_message: registration.errors.details) unless registration.valid?

      # Renders NOK if true
      return if gba_blocked_by_bsn?(bsn: registration.burgerservicenummer)
      # Renders NOK if true
      return if gba_blocked_by_address?(
        postal_code: registration.postcode,
        house_number: registration.huisnummer,
        date_of_birth: registration.geboortedatum
      )

      BrpRegistrationJob.perform_async("account_request_app", registration.id, nil, nil)

      Log.instrument("6", registration_id: registration.id, hidden: true)

      render(json: { status: "OK", registration_id: registration.id }, status: 200)
    end

    def get_registration_by_account
      account = Account.find(params[:account_id])
      return unless account

       # Legacy app request via letter (with already activated account)
       # Necessary if account is created through stubs
      registration = Registration.find_by(burgerservicenummer: account.bsn, gba_status: "valid_app_extension")

      registration ||= Registration.joins(:activation_letters)
                                    .where(burgerservicenummer: account.bsn)
                                    .where(gba_status: "valid")
                                    .where(activation_letters: { letter_type: "activeringscode_aanvraag_via_digid_app" }).last

      render(json: { status: "OK", registration_id: registration&.id, has_bsn: account.bsn.present? }, status: 200)
    end

    def finish_registration
      registration = Registration.find(params[:registration_id])
      return unless registration

      account = Account.find(params[:account_id])
      return unless account

      if params[:flow_name] == "activate_app_with_password_letter_flow"
        registration.finish_letters(ActivationLetter::LetterType::ACTIVATION_APP_ONE_DEVICE)
      else
        registration.update(status: Registration::Status::REQUESTED)
        registration.finish_letters(ActivationLetter::LetterType::AANVRAAG_ACCOUNT_ACTIVATIE_VIA_APP)

        account.status = Account::Status::REQUESTED
        account.save!
      end

      return render json: { status: "PENDING", activation_code: registration.activation_letters.last.controle_code, geldigheidstermijn: geldigheidstermijn_activatie_brief }
    end

    def validate_account
      auth = Authentication.new({
        username: params[:username],
        password: params[:password],
        type_account: "basis",
        level: "basis",
        session_type: "sign_in",
        webservice_id: mijn_digid_webservice.id,
        remember_login: 0
      })

      auth.session_type = "activation" if auth.account_or_ghost.is_a?(Account) && auth.account_or_ghost&.state == Account::Status::REQUESTED
      @account = auth.account_or_ghost

      if auth.valid?
        @account.blocking_manager.reset!

        unless account_has_bsn?
          return render(json: { status: "NOK", error: "no_bsn_on_account", account_id: @account.id})
        end

        activation_method = determine_activation_method

        render json: {
          status: "OK",
          account_id: @account.id,
          issuer_type: determine_issuer_type || "",
          activation_method: activation_method,
          sms_check_requested: activation_method == "request_for_account" && @account.sms_tools.present?,
          has_bsn: @account.bsn.present?
        }
      else
        auth.account_or_ghost.password_authenticator.seed_old_passwords(auth.password) if auth.account_or_ghost.is_a?(Account)

        render_validate_account_error(auth)
      end
    end

    def get_account_status
      error = "classified_deceased" if @account.deceased?
      render json: { status: @account.status, error: error }
    end

    def create_letter
      fake_registration = Registration.create_fake_aanvraag(@account.bsn)

      if params[:re_request_letter] == true
        registration = Registration.where(burgerservicenummer: @account&.bsn, status: ::Registration::Status::REQUESTED).last
      end

      error, payload = letter_allowed?(fake_registration)
      error ||= "too_many_letter_requests" if registration && registration.activation_letters.count >= 2

      letter_request = LetterRequest.new(
        account_id: @account.id,
        action: "app_extension",
        request_max: "blokkering_digid_app_aanvragen",
        request_speed_limit: "snelheid_aanvragen_digid_app",
        registration: registration || fake_registration
      )

      if error.present?
        response = {status: "NOK", error: error }
        response[:payload] = payload

        return render json: response
      end

      clean_pending_app_activation_letter(@account) if params[:re_request_letter] == true

      letter_request.prepare_letter(params[:activation_method])
      render json: {status: "OK", registration_id: letter_request.registration.id }
    end

    def poll_letter
      registration = Registration.find(params[:registration_id])
      controle_code = registration.activation_letters.first&.controle_code
      postcode = registration.activation_letters.first&.postcode
      issuer_type = BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? ? IssuerType::LETTER_SECURE_DELIVERY : IssuerType::LETTER

      if params[:re_request_letter] == true
        registration.update_attribute(:status, ::Registration::Status::REQUESTED)
        if @account.status == Account::Status::ACTIVE || (@account.status == Account::Status::REQUESTED && @account.issuer_type == IssuerType::MUNICIPAL)
          registration.finish_letters(ActivationLetter::LetterType::ACTIVATION_APP_ONE_DEVICE)
        else
          registration.finish_letters(ActivationLetter::LetterType::AANVRAAG_ACCOUNT_ACTIVATIE_VIA_APP)
        end
      end

      render json: { status: "OK", gba_status: registration.gba_status, controle_code: controle_code, issuer_type: issuer_type }
    end

    def get_account_request_gba_status
      registration = Registration.find(params[:registration_id])
      return unless registration

      case registration.gba_status
      when "valid"
        if Registration.application_too_soon?(bsn: registration.burgerservicenummer)
          Log.instrument("16", registration_id: registration.id)
          render(
            json: {
              status: "NOK",
              error: "application_too_soon",
              no_of_days_between_account_applications: ::Configuration.get_int("snelheid_aanvragen")
            },
            status: 200
          )
          return
        elsif Registration.too_many_applications_this_month?(bsn: registration.burgerservicenummer)
          Log.instrument("17", registration_id: registration.id)
          render(
            json: {
              status: "NOK",
              error: "too_many_applications_this_month",
              max_amount_of_account_application_per_month: ::Configuration.get_int("blokkering_aanvragen")
            },
            status: 200
          )
          return
        end

        render(json: { status: "OK" }, status: 200)
      when "pending"
        render(json: { status: "PENDING" }, status: 200)
      else
        render(json: { status: "NOK", error: registration.gba_status }, status: 200)
      end
    end

    def get_gba_status
      registration = Registration.find(params[:registration_id])
      controle_code = registration.activation_letters.last&.controle_code
      postcode = registration.activation_letters.last.postcode
      issuer_type = BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? ? IssuerType::LETTER_SECURE_DELIVERY : IssuerType::LETTER

      render json: { status: "OK", gba_status: registration.gba_status, controle_code: controle_code, issuer_type: issuer_type }
    end

    def get_existing_application
      registration = Registration.find(params[:registration_id])
      return unless registration

      bsn = registration.burgerservicenummer

      former_application_present = Account.with_bsn(bsn).where(status: Account::Status::REQUESTED).exists?

      if former_application_present
        Log.instrument("20", registration_id: registration.id)
        return render(json: { status: "PENDING" }, status: 200)
      end

      render(json: { status: "OK" }, status: 200)
    end

    def replace_existing_application
      unless params[:replace_application].is_a?(TrueClass) || params[:replace_application].is_a?(FalseClass)
        return render_invalid_parameters(debug_message: "'replace_application' not a boolean")
      end

      registration = Registration.find(params[:registration_id])
      return unless registration

      bsn = registration.burgerservicenummer

      if params[:replace_application]
        Log.instrument("21", registration_id: registration.id)

        Registration.where(burgerservicenummer: bsn).where.not(id: registration.id).each do |registration|
          registration.activation_letters.each do |letter|
            Log.instrument(letter.state.finished? ? "22" : "23", registration_id: registration.id)
          end
        end

        Account.with_bsn(bsn).where(status: Account::Status::REQUESTED).each do |account|
          account.destroy
        end

        return render(json: { status: "OK" }, status: 200)
      end

      Log.instrument("24", account_id: Account.with_bsn(bsn).where(status: Account::Status::REQUESTED).first&.id)
      render(json: { status: "NOK" }, status: 200)
    end

    def get_existing_account
      registration = Registration.find(params[:registration_id])
      return unless registration

      bsn = registration.burgerservicenummer

      if existing_account = Account.with_bsn(bsn).first
        Log.instrument("25", registration_id: registration.id)

        if existing_account.status == Account::Status::SUSPENDED
          Log.instrument("26", registration_id: registration.id)
          return render(json: { status: "NOK" }, status: 200)
        elsif existing_account.password_authenticator&.status == Authenticators::Password::Status::BLOCKED
          # FO en IB zijn nog in overleg; password blocked zou niet aanvragen moeten tegenhouden. Ook de logregel slaat nu nergens op
          # DD-4074
          Log.instrument("26", registration_id: registration.id)
          return render(json: { status: "NOK" }, status: 200)
        elsif existing_account.status == Account::Status::ACTIVE && existing_account&.replace_account_blocked
          Log.instrument("1556", registration_id: registration.id, account_id: existing_account&.id)
          return render(json: { status: "NOK" }, status: 200)
        elsif existing_account.status == Account::Status::ACTIVE
          return render(json: { status: "PENDING" }, status: 200)
        end
      end


      account = Account.create_for_bsn(bsn: bsn, locale: params[:language], issuer_type: IssuerType::REGULAR_APP)

      render(json: { status: "OK", account_id: account.id }, status: 200)
    end

    def replace_existing_account
      unless params[:replace_account].is_a?(TrueClass) || params[:replace_account].is_a?(FalseClass)
        return render_invalid_parameters(debug_message: "'replace_application' not a boolean")
      end

      registration = Registration.find(params[:registration_id])
      return unless registration

      bsn = registration.burgerservicenummer

      unless params[:replace_account]
        Log.instrument("28", registration_id: registration.id)
        return render(json: { status: "NOK" }, status: 200)
      end

      Log.instrument("27", registration_id: registration.id)

      Account.with_bsn(bsn).where(status: [Account::Status::ACTIVE]).each do |account|
        next unless account&.email_activated?

        NotificatieMailer.delay(queue: "email").notify_heraanvraag(account_id: account.id, recipient: account.adres)
      end

      account = Account.create_for_bsn(bsn: bsn, locale: params[:language], issuer_type: IssuerType::REGULAR_APP)

      render(json: { status: "OK", account_id: account.id }, status: 200)
    end

    def send_sms
      sms_tool = params[:activation_method] == "request_for_account" ? @account.pending_sms_tool : @account.active_sms_tool
      spoken = params[:spoken] == "" ? sms_tool.gesproken_sms || false : params[:spoken]

      if (seconds_until_next_attempt = @account.next_sms_wait_time(spoken: spoken)).to_i > 0
        return render(json: { status: "NOK", error: "sms_too_fast", seconds_until_next_attempt: seconds_until_next_attempt })
      end

      send_sms_code(sms_tool.phone_number, spoken, params[:activation_method])

      render json: { status: "OK", phonenumber: mobile_number(mobile_number: sms_tool.phone_number, mask: true)}
    end

    def confirm_sms
      sms_tool = @account.sms_tool_active? ? @account.active_sms_tool : @account.pending_sms_tool
      spoken = params[:spoken] == "" ? sms_tool.gesproken_sms || false : params[:spoken]

      smscode = Smscode.new(
        account: @account,
        session_type: "activation",
        webservice: mijn_digid_webservice,
        smscode: params[:smscode],
        spoken: spoken
      )

      valid = smscode.use!

      if @account.blocking_manager.blocked?
        confirm_sms_block_account(@account)
        return render json: { status: "NOK", error: "smscode_blocked"}
      end

      unless valid
        sms_error = smscode.errors.details[:smscode].first[:error]
        case sms_error
        when :not_send
          render json: { status: "NOK", error: "smscode_not_send" }
        when :expired
          render json: { status: "NOK", error: "smscode_expired" }
        when :incorrect
          render json: { status: "NOK", error: "smscode_incorrect" }
        else
          render json: { status: "NOK", error: "smscode_invalid" }
        end
        return
      end

      render json: { status: "OK" }
    end

    def request_wid
      wid_request = WidRequest.create(account: @account)
      render json: { wid_request_id: wid_request.id }
    end

    def get_wid_status
      wid_request = WidRequest.find(params[:wid_request_id])

      return render json: { status: "PENDING" } unless wid_request.ready?

      log_wid_job_results(wid_request) unless wid_request.completed_with_errors?

      if wid_request.valid? && wid_request.documents.any?
        return render json: {
          status: "OK",
          travel_documents: wid_request.travel_documents,
          driving_licences: wid_request.driving_licences
        }
      elsif wid_request.completed_with_errors?
        return render json: { status: "NOK" }
      elsif !wid_request.not_found_in_brp? && wid_request.documents.none?
        return render json: { status: "NO_DOCUMENTS", brp_identifier: wid_request.brp_response&.identifier }
      elsif wid_request.documents.none?
        return render json: { status: "NOK" }
      end

      render json: { status: "NOK" }
    end

    def send_notification_message
      if @account.email_activated?
        NotificatieMailer.delay(queue: "email").send_by_email_ref(email_ref: params[:email_ref], account_id: @account.id, recipient: @account.adres)
      elsif @account.active_sms_tool.present?
        sms_service = SmsChallengeService.new(account: @account)
        @account.with_language { sms_service.send_sms(message: t("sms_message.#{params[:sms_ref]}"), spoken: params[:spoken].to_s == "true") }
      end
    end

    def decrypt_ei
      bsn = Crypto.decrypt_encrypted_identity(params[:ei])
      account = Account.with_bsn(bsn).sort_by(&:status).first

      render json: { account_id: account&.id }
    end

    def send_notification_letter
      registration_id = params[:registration_id]
      if registration_id.present?
        registration = Registration.find(registration_id)
        registration.update_attribute(:status, ::Registration::Status::REQUESTED)
        registration.finish_letters(ActivationLetter::LetterType::APP_NOTIFICATION_LETTER)
      end
    end

    def letter_send_date
      registration = Registration.find(params[:registration_id])
      render(json: { status: "OK", date: registration&.activation_letters&.last&.created_at&.to_date&.iso8601}, status: 200)
    end

    def check_bsn
      return render(json: { status: "NOK"}, status: 200) unless params[:bsn] == @account&.bsn

      render(json: { status: "OK"}, status: 200)
    end

    def activate_account_with_code
      password_authenticator = @account&.password_authenticator

      if password_authenticator.expired?
        return render json: { status: "NOK", error: "activation_code_invalid", days_valid: password_authenticator.geldigheidstermijn.to_i }
      end

      @activation_code = Activationcode.new(account: @account,
                                           authenticator: password_authenticator,
                                           activationcode: params[:activation_code])

      if password_authenticator.reached_attempts_limit?
        render json: { status: "NOK", error: "activation_code_blocked" }
      elsif @activation_code.valid?
        Log.instrument("93", account_id: @account.id, fields: log_fields(@account))
        @account.activate!(send_mail: false)
        @account.activate_sms_tool! if Authenticators::SmsTool.where(account_id: @account.id)&.last&.status == Authenticators::SmsTool::Status::PENDING
        @account.destroy_other_accounts(sms_in_uitbreiding: @account.sms_in_uitbreiding?)
        password_authenticator.delete_attempts
        render json: { status: "OK", issuer_type: password_authenticator.issuer_type}
      else
        render json: password_authenticator.reached_attempts_limit? ? { status: "NOK", error: "activation_code_blocked" } :
                       { status: "NOK", error: "activation_code_not_correct", remaining_attempts: password_authenticator.max_number_of_failed_attempts - password_authenticator.number_of_attempts }
      end
    end

    def activate_account
      return render json: { status: "NOK" } if @account.nil?

      if @account.status == Account::Status::REQUESTED
        Log.instrument("93", account_id: @account&.id, fields: log_fields(@account))
        if params[:issuer_type] == "gemeentebalie"
          Log.instrument("1396", account_id: @account&.id, hidden: true)
        else
          Log.instrument("1509", account_id: @account&.id, hidden: true)
        end
        @account.activate!
        @account.destroy_other_accounts
      end

      render json: { status: "OK" }
    end

    def get_bsn
      render json: { bsn: @account.bsn }
    end

    private

    def geldigheidstermijn_activatie_brief
      ::Configuration.get_int("geldigheid_brief")
    end

    def render_invalid_parameters(debug_message:)
      Rails.logger.warn("Invalid parameter: '#{debug_message}'")
      if APP_CONFIG["dot_environment"]
        render(json: { status: "NOK", error: "invalid_parameters", debug: debug_message }, status: 200)
      else
        render(json: { status: "NOK", error: "invalid_parameters" }, status: 200)
      end
    end

    def gba_blocked_by_bsn?(bsn:)
      gba_block = GbaBlock.find_by_bsn(bsn: bsn)

      if gba_block && gba_block.blocked_till > Time.zone.now
        Log.instrument("18", date: I18n.l(gba_block.blocked_till, format: :sms))
        minutes_delay = ((gba_block.blocked_till - Time.zone.now) / 60).to_i
        render(json: { status: "NOK", error: "gba_blocked", delay: minutes_delay }, status: 200)
        return true
      end

      false
    end

    def gba_blocked_by_address?(postal_code:, house_number:, date_of_birth:)
      gba_block = GbaBlock.find_by_address(
        postal_code: postal_code,
        house_number: house_number
      )

      if gba_block && gba_block.blocked_till > Time.zone.now
        # The log 19 requires date_of_birth, but the block doens't have to match on date_of_birth
        Log.instrument("19", date_of_birth: date_of_birth, postcode: postal_code, house_number: house_number, date: I18n.l(gba_block.blocked_till, format: :sms))
        minutes_delay = ((gba_block.blocked_till - Time.zone.now) / 60).to_i
        render(json: { status: "NOK", error: "gba_blocked", delay: minutes_delay }, status: 200)
        return true
      end

      false
    end

    def determine_activation_method
      if @account.status == Account::Status::REQUESTED
        "request_for_account"
      elsif @account.active_sms_tool.present?
        "standalone"
      else
        "password"
      end
    end

    def account_has_bsn?
      if @account.bsn.blank? && @account.oeb.blank? && @account.status != Account::Status::REQUESTED
        Log.instrument("1074", account_id: @account.id)
        false
      else
        true
      end
    end

    def determine_issuer_type
      if @account.status == Account::Status::REQUESTED && @account.password_authenticator&.issuer_type == IssuerType::LETTER_INTERNATIONAL
        Authenticators::AppAuthenticator::LETTER_INTERNATIONAL
      elsif @account.status == Account::Status::REQUESTED && @account.issuer_type == IssuerType::DESK
        Authenticators::AppAuthenticator::FRONT_DESK
      elsif @account.status == Account::Status::REQUESTED
        bsn = Sectorcode.where(sector_id: Sector.get("bsn"), account_id: @account.id)&.first&.sectoraalnummer
        postcode = Registration.where(burgerservicenummer: bsn)&.last&.postcode
        BeveiligdeBezorgingPostcodeCheck.new(postcode).positive? ? IssuerType::LETTER_SECURE_DELIVERY : IssuerType::LETTER
      end
    end

    def render_validate_account_error(auth)
      if @account.deceased?
        render(json: { status: "NOK", error: "classified_deceased", account_id: @account.id })
      elsif auth.errors.messages[:authentication].present? # password incorrect
        render json: { status: "NOK", error: "invalid"}
      elsif auth.account_or_ghost.is_a?(Account) && [Account::Status::ACTIVE, Account::Status::REQUESTED].exclude?(auth.account_or_ghost.status)
        render json: { status: "NOK", error: "account_inactive"}
      elsif auth.errors.messages.key?(:blocked)
        render json: { status: "NOK", error: "account_blocked", payload: {
          since: I18n.l(auth.account_or_ghost.blocking_manager.timestamp_first_failed_attempt, format: :date_time_text),
          count: auth.account_or_ghost.blocking_manager.max_number_of_failed_attempts,
          until: I18n.l(auth.account_or_ghost.blocking_manager.blocked_till, format: :time_text),
          minutes: auth.account_or_ghost.blocking_manager.blocked_time_left_in_minutes }
        }
      else
        render json: { status: "NOK", error: "invalid"}
      end
    end

    def clean_pending_app_activation_letter(account)
      registration = Registration.where(burgerservicenummer: account.bsn, gba_status: "valid_app_extension").order(created_at: :desc).try(:first)
      if registration
        if registration.activation_letters.last.try(:state) == ActivationLetter::Status::SENT
          Log.instrument("908", account_id: account.id, registration_id: registration&.id)
        else
          Log.instrument("909", account_id: account.id, registration_id: registration&.id)
        end
      end
    end

    def mijn_digid_webservice
      Webservice.find_by(name: "Mijn DigiD")
    end

    def set_account
      @account ||= Account.find(params[:account_id])
    end

    def send_sms_code(phone_number, spoken, activation_method)
      sms_type = { request_for_account: "SMS03", standalone: "SMS09" }[activation_method.to_sym]
      sms_service = SmsChallengeService.new account: @account
      @sms_uitdaging = sms_service.create_challenge(
        sms_phone_number: phone_number,
        sms_gesproken_sms: spoken,
        action: "activation",
        sms_type: sms_type,
        webservice: mijn_digid_webservice
      )
      session[:sms_options] ||= {}
      session[:sms_options][:reference] = "REF-#{@sms_uitdaging.id}"
    end

    def confirm_sms_block_account(account)
      timestamp_of_first_failed_sms_login_attempt_for_this_flow = account.time_stamp_of_first_sms_challenge("activation")
      account.blocking_manager.register_external_blocking_failure_with_given_start_time(timestamp_of_first_failed_sms_login_attempt_for_this_flow)
      account.void_last_sms_challenge_for_action("activation")
    end

    def log_wid_job_results(wid_request)
      account_id = wid_request&.account_id

      if wid_request.brp_response.success?
        Log.instrument("1217", hidden: true, account_id: account_id)
      else
        if wid_request.brp_response.status == "deceased"
          Log.instrument("1210", hidden: true, account_id: account_id)
        elsif !wid_request.brp_response.error?
          Log.instrument("1211", hidden: true, account_id: account_id)
        end
      end

      if wid_request.travel_documents.any?
        Log.instrument("1205", hidden: true, account_id: account_id)
      else
        Log.instrument("1204", hidden: true, account_id: account_id)
      end

      if wid_request.driving_licences.any?
        Log.instrument("1207", hidden: true, account_id: account_id)
      else
        Log.instrument("1206", hidden: true, account_id: account_id)
      end

      if wid_request.documents.empty?
        Log.instrument("1220", hidden: true, account_id: account_id)
      end
    end

    def letter_allowed?(registration)
      if registration.registration_too_soon?("snelheid_aanvragen_digid_app", ["valid_app_extension", "valid"])
        ["too_soon", nil]
      elsif next_possible_registration = registration.registration_too_often?("blokkering_digid_app_aanvragen", ["valid_app_extension", "valid"])
        ["too_often", { next_registration_date: next_possible_registration, blokkering_digid_app_aanvragen: ::Configuration.get_int("blokkering_digid_app_aanvragen") }]
      end
    end
  end
end
