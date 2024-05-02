
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
  class RequestStationsController < ApplicationController
    include ApplicationHelper
    include RedisClient
    include RequestStationSession

    skip_before_action :verify_authenticity_token
    authorize_with_token :iapi_token, only: [:validate_app_activation, :app_activation]
    before_action :check_switches, :check_json_body

    def authenticate_account
      auth = Authentication.new({
                                  username: params[:username],
                                  password: params[:password],
                                  type_account: "basis",
                                  level: "basis",
                                  session_type: "sign_in",
                                  webservice_id: mijn_digid_webservice.id,
                                  webservice_name: "Mijn DigiD",
                                  remember_login: 0
                                })
      if auth.valid?
        render json: { status: "OK", account_id: auth.account.id }
        return
      else
        if auth.errors.messages[:authentication].present? # password incorrect
          render json: { status: "NOK", error: "invalid"}
          return
        elsif auth.account_or_ghost.is_a?(Account) && auth.account_or_ghost.status != Account::Status::ACTIVE
          render json: { status: "NOK", error: "account_inactive"}
          return
        elsif auth.errors.messages.key?(:blocked)
          render json: { status: "NOK", error: "account_blocked" }
          return
        else
          render json: { status: "NOK", error: "invalid"}
          return
        end
      end
    end

    def account_standing
      bsn = params[:bsn]

      standing = if invalid_account?(bsn)
        'invalid'
      elsif existing_account?(bsn)
        'existing'
      elsif new_account?(bsn)
        'new'
      else
        'error'
                 end

      sector_ids = Sector.fetch(Sector.get("bsn"))
      account_id_with_bsn = Account&.with_bsn(bsn)&.first&.id
      account_id_with_bsn_initial = Account.with_bsn(bsn)&.initial&.first&.id
      account_id_active = Account.find_by_sectoraalnummer_and_sector_ids_and_status(bsn, sector_ids, ::Account::Status::ACTIVE)&.first&.id
      account_id_active_or_requested = Account.find_by_sectoraalnummer_and_sector_ids_and_status(bsn, sector_ids, [::Account::Status::ACTIVE, ::Account::Status::REQUESTED])&.first&.id

      body = { standing: standing }
      body[:account_id_with_bsn] = account_id_with_bsn if account_id_with_bsn
      body[:account_id_with_bsn_initial] = account_id_with_bsn_initial if account_id_with_bsn_initial
      body[:account_id_active] = account_id_active if account_id_active
      body[:account_id_active_or_requested] = account_id_active_or_requested if account_id_active_or_requested
      body[:initial] = Account.with_bsn(bsn)&.initial&.first ? "true" : "false"

      render json: body
    end

    def check_document_in_brp
      brp_result = brp_check(params[:bsn], params[:document_number])
      if brp_result[:error_code] == "valid" && brp_result[:valid_document_present]
        status = "OK"
        document_type = brp_result[:valid_document_type] == "NI" ? "id_card" : "passport"
        Log.instrument("1388")
      else
        status = "NOK"
        error_code = brp_result[:error_code]
      end

      body = { status: status }
      body[:document_type] = document_type if document_type.present?
      body[:error_code] = error_code if error_code.present?
      render json: body
    end

    def create_activation_letter
      bsn = params[:bsn]
      create_account = params[:create_account]
      sector_ids = Sector.fetch(Sector.get("bsn"))
      registration_letter = create_registration_and_letter(bsn: bsn)

      if create_account
        account = Account.create_for_request_station(sectornummers(@registration.burgerservicenummer, @registration.id))
      else
        account = Account.find_by_sectoraalnummer_and_sector_ids_and_status(@registration.burgerservicenummer, sector_ids, [::Account::Status::ACTIVE, ::Account::Status::REQUESTED]).first
        account.update(issuer_type: "gemeentebalie")
      end

      if registration_letter[:status] == "OK"
        clean_pending_app_activation(account, bsn, @registration.id)
        body = { status: "OK", controle_code: @controle_code }
      else
        body = { status: registration_letter[:status], error_code: registration_letter[:error_code] }
      end

      body[:account_id] = account.id
      render json: body
    end

    def validate_app_activation # Parameters: bsn, transaction_id, station_id, document_number
      Log.instrument("1384", transactieid: params[:iapi][:transaction_id], aanvraagstationid: params[:iapi][:station_id])

      bsn = params[:iapi][:bsn]
      if invalid_account?(bsn)
        Log.instrument("1394", account_id: Account.with_bsn(bsn).first.id)
        Log.instrument("1385", account_id: Account.with_bsn(bsn)&.initial&.first&.id) if Account.with_bsn(bsn)&.initial&.first
        status = 'NOK'
        error_code = 'E_DIGID_ACCOUNT_NOT_VALID'
      elsif existing_account?(bsn)
        Log.instrument("1394", account_id: Account.with_bsn(bsn).first.id)
          brp_result = brp_check(bsn, params[:iapi][:document_number])
          if brp_result[:error_code] == "valid" && brp_result[:valid_document_present]
            status = "OK"
            document_type = brp_result[:valid_document_type] == "NI" ? "id_card" : "passport"
            redis.hset(transaction_key, "document_type", document_type)
            Log.instrument("1388")
          else
            status = "NOK"
            error_code = brp_result[:error_code]
          end
      elsif new_account?(bsn)
        brp_result = brp_check(bsn, params[:iapi][:document_number])
        if brp_result[:error_code] == "valid" && brp_result[:valid_document_present]
          status = 'OK'
          document_type = brp_result[:valid_document_type] == "NI" ? "id_card" : "passport"
          redis.hset(transaction_key, "document_type", document_type)
          Log.instrument("1388")
        else
          status = 'NOK'
          error_code = brp_result[:error_code]
        end
      else
        status = 'NOK'
        error_code = 'E_GENERAL'
      end

      redis.mapped_hmset(transaction_key, { bsn: bsn, documentnummer: params[:iapi][:document_number] })
      redis.expire(transaction_key, ::Configuration.get_int("RvIG-Aanvraagstation_session_expiration").minutes)

      body = { status: status }
      body[:error_code] = error_code if error_code.present?
      render json: body
    end

    def app_activation # Parameters: activation_code, transaction_id
      Log.instrument("1389", transactieid: params[:iapi][:transaction_id])
      app_session_id = redis.hget(activation_code_key, "app_session_id")

      if app_session_id.present? && redis.exists?(transaction_key)
        status = "OK"
        sector_ids = Sector.fetch(Sector.get("bsn"))
        bsn = redis.hget(transaction_key, "bsn")
        document_type = redis.hget(transaction_key, "document_type")
        authenticated = redis.hget(activation_code_key, "authenticated")
        device_name = redis.hget(activation_code_key, "device_name")
        instance_id = redis.hget(activation_code_key, "instance_id")
        user_app_id = redis.hget(activation_code_key, "user_app_id")

        if authenticated == "true"
          # Account aanwezig (REQUESTED or ACTIVE) en geauthenticeerd
          account = Account.find_by_sectoraalnummer_and_sector_ids_and_status(bsn, sector_ids, ::Account::Status::ACTIVE).first
          redis.mapped_hmset(activation_code_key, { account_id: account&.id })

          auth_app = create_app_authenticator(account, user_app_id, device_name, instance_id, document_type)

          if less_than_max_amount_of_apps?(bsn, instance_id)
            redis.mapped_hmset(activation_code_key, { user_app_id: auth_app.user_app_id, activation_status: "OK" })
            Log.instrument("1393")
          else
            Log.instrument("1386", account_id: Account.with_bsn(bsn).first.id)
            redis.mapped_hmset(activation_code_key, { user_app_id: auth_app.user_app_id, activation_status: "TOO_MANY_APPS" })
          end
        else
          if invalid_account?(bsn)
            # Account has status SUSPENDED, REVOKED or INITIAL
            status = 'NOK'
            error_code = 'E_DIGID_ACCOUNT_NOT_VALID'
            redis.hset(activation_code_key, "activation_status", "NOK" )
          elsif existing_account?(bsn)
            # Account present (REQUESTED or ACTIVE) but not authenticated
            letter = process_letter(bsn: bsn,
                                    create_account: false,
                                    sector_ids: sector_ids,
                                    user_app_id: user_app_id,
                                    device_name: device_name,
                                    instance_id: instance_id,
                                    document_type: document_type,
                                    activation_code: params[:iapi][:activation_code])

            if !less_than_max_amount_of_apps?(bsn, instance_id)
              Log.instrument("1386", account_id: Account.with_bsn(bsn).first.id)
              redis.hset(activation_code_key, "activation_status", "TOO_MANY_APPS" )
            end

            status = letter[:status]
            error_code = letter[:error_code]
          elsif new_account?(bsn)
            # Account Not present or has the status REMOVED or EXPIRED
            letter = process_letter(bsn: bsn,
                                    create_account: true,
                                    sector_ids: sector_ids,
                                    user_app_id: user_app_id,
                                    device_name: device_name,
                                    instance_id: instance_id,
                                    document_type: document_type,
                                    activation_code: params[:iapi][:activation_code])
            status = letter[:status]
            error_code = letter[:error_code]
          else
            status = 'NOK'
            error_code = 'E_GENERAL'
            redis.hset(activation_code_key, "activation_status", "NOK" )
          end
        end
      else
        status = "NOK"
        error_code = "E_APP_ACTIVATION_CODE_NOT_FOUND"
        redis.hset(activation_code_key, "activation_status", "NOK" )
        Log.instrument("1391")
      end

      body = { status: status }
      body[:error_code] = error_code if error_code.present?
      render json: body
    end

    private

    def mijn_digid_webservice
      Webservice.find_by(name: "Mijn DigiD")
    end

    def destroy_apps_with_current_instance_id(app_authenticators, instance_id)
      return if app_authenticators.nil? || instance_id.nil?
      app_authenticators.where(instance_id: instance_id).destroy_all
    end

    def sectornummers(bsn, registration_id)
      sectornummers = []
      sectornummers << [Sector.get("bsn"), bsn]
      a_nummer = Registration.get_a_nummer(registration_id)
      sectornummers << [Sector.get("a-nummer"), a_nummer] if a_nummer
      sectornummers
    end

    def existing_account?(bsn)
      sector_ids = Sector.fetch(Sector.get("bsn"))
      Account.account_already_exists(bsn, sector_ids).exists? || Account.registration_currently_in_progress(bsn, sector_ids).exists?
    end

    def new_account?(bsn)
      sector_ids = Sector.fetch(Sector.get("bsn"))
      Account.count_account_already_exists(bsn, sector_ids) < 1 || Account.account_removed(bsn, sector_ids).exists? || Account.account_expired(bsn, sector_ids).exists?
    end

    def invalid_account?(bsn)
      sector_ids = Sector.fetch(Sector.get("bsn"))
      Account.account_suspended(bsn, sector_ids).exists? || Account.account_revoked(bsn, sector_ids).exists? || Account.account_initial(bsn, sector_ids).exists?
    end

    def brp_check(bsn, documentnummer)
      brp_response = BrpRequestJob.new.perform_request(bsn)
      error_code = nil
      brp_response&.status = "investigate_address" if brp_response&.onderzoek_adres?

      if brp_response&.status
        case brp_response.status
        when "valid", "rni", "emigrated", "ministerial_decree"
          brp_response.status = "valid"
        when "not_found"
          Log.instrument("1399", hidden: true)
          error_code = "E_BSN_NOT_VALID"
        when "deceased"
          Log.instrument("1210", hidden: true)
          error_code = "E_DOCUMENT_NOT_VALID"
        else
          Log.instrument("1211", hidden: true) if %w(not_found investigate_address suspended_error suspended_unknown).include? brp_response.status
          error_code = "E_DOCUMENT_NOT_VALID"
        end
      else
        Log.instrument("158", hidden: true)
        error_code = "E_GENERAL"
      end

      brp_response&.reisdocumenten&.each do |doc|
        if doc.nummer_reisdocument == documentnummer
          if document_expired?(doc)
            Log.instrument("1407", hidden: true)
            error_code = "E_DOCUMENT_NOT_VALID"
          else
            @valid_document_present = true
            @valid_document_type = doc.soort_reisdocument
          end
        end
      end

      # A valid bsn but no valid documents
      error_code = "E_DOCUMENT_NOT_VALID" if brp_response.status == "valid" && @valid_document_present.blank?

      @valid_document_present.present? ? Log.instrument("1408", hidden: true) : Log.instrument("1406", hidden: true)

      { error_code: error_code || brp_response&.status, valid_document_present: @valid_document_present, valid_document_type: @valid_document_type }
    end

    def process_letter(bsn:, create_account:, sector_ids:, user_app_id:, device_name:, instance_id:, document_type:, activation_code:)
      registration_letter = create_registration_and_letter(bsn: bsn)
      if create_account
        account = Account.create_for_request_station(sectornummers(@registration.burgerservicenummer, @registration.id))
      else
        account = Account.find_by_sectoraalnummer_and_sector_ids_and_status(@registration.burgerservicenummer, sector_ids, [::Account::Status::ACTIVE, ::Account::Status::REQUESTED]).first
        account.update(issuer_type: "gemeentebalie")
      end

      if registration_letter[:status] == "OK"
        clean_pending_app_activation(account, bsn, @registration.id)
        auth_app = create_app_authenticator_for_letter(account, user_app_id, device_name, instance_id, document_type, @controle_code)
        redis.mapped_hmset(activation_code_key, { user_app_id: auth_app.user_app_id, activation_status: "OK", account_id: account&.id })
        Log.instrument("1393")
        Log.instrument("905", account_id: auth_app&.account_id, app_code: auth_app&.app_code, device_name: auth_app&.device_name, app_authenticator_id: auth_app&.id)
        { status: registration_letter[:status], error_code: registration_letter[:error_code] }
      else
        redis.hset(activation_code_key(activation_code), "activation_status", "NOK" )
        { status: registration_letter[:status], error_code: registration_letter[:error_code] }
      end
    end

    def clean_pending_app_activation(account, bsn, registration_id)
      previous_registration = Registration.not_me(registration_id).requested
                                  .where("burgerservicenummer = ?", bsn)
                                  .where("gba_status IN (?)", "valid_app_extension").last
      if previous_registration
        if previous_registration.activation_letters&.first.try(:state) == ActivationLetter::Status::SENT
          Log.instrument("908", registration_id: previous_registration.id)
        else
          Log.instrument("909", registration_id: previous_registration.id)
        end
      end
      account.app_authenticators.pending.try(:first).try(:destroy)
    end

    def create_registration_and_letter(bsn:)
      @registration = Registration.create_fake_aanvraag(bsn)
      @registration.update_attribute(:status, ::Registration::Status::REQUESTED)
      letter_status = letter_allowed(@registration)
      if letter_status[:status] == "OK"
        if BrpRegistrationJob.new.perform_request(bsn: bsn, request_type: "app_extension", registration_id: @registration.id)
          @registration.reload
          if @registration&.gba_status
            case @registration.gba_status
            when "valid_app_extension", "rni", "emigrated", "ministerial_decree"
              status = "OK"
              error_code = nil
            when "not_found"
              Log.instrument("1399", hidden: true)
              status = "NOK"
              error_code = "E_BSN_NOT_VALID"
            when "deceased"
              Log.instrument("1210", hidden: true)
              status = "NOK"
              error_code = "E_DOCUMENT_NOT_VALID"
            else
              Log.instrument("1211", hidden: true) if %w(not_found investigate_address suspended_error suspended_unknown).include? @registration.gba_status
              status = "NOK"
              error_code = "E_DOCUMENT_NOT_VALID"
            end
          else
            Log.instrument("158", hidden: true)
            status = "NOK"
            error_code = "E_GENERAL"
          end

          @controle_code = @registration.activation_letters.last&.controle_code
          @registration.finish_letters(ActivationLetter::LetterType::ACTIVATION_APP_ONE_DEVICE)
          { status: status, error_code: error_code }
        else
          Log.instrument("158", hidden: true)
          { status: "NOK", error_code: "E_GENERAL" }
        end
      else
        { status: "NOK", error_code: "E_GENERAL" }
      end
    end

    def letter_allowed(registration)
      if registration.registration_too_soon?("snelheid_aanvragen_digid_app", ["valid_app_extension"])
        status = "NOK"
        error_code = "TOO_SOON"
      elsif registration.registration_too_often?("blokkering_digid_app_aanvragen", "valid_app_extension")
        status = "NOK"
        error_code = "TOO_OFTEN"
      else
        status = "OK"
        error_code = ""
      end

      { status: status, error_code: error_code }
    end

    def create_app_authenticator(account, user_app_id, device_name, instance_id, document_type)
      destroy_apps_with_current_instance_id(account.app_authenticators, instance_id)
      account.app_authenticators.create(
        user_app_id: user_app_id,
        status: Authenticators::AppAuthenticator::Status::PENDING,
        device_name: device_name,
        issuer_type: "gemeentebalie",
        instance_id: instance_id,
        substantieel_activated_at: Time.zone.now,
        substantieel_document_type: document_type
      )
    end

    def create_app_authenticator_for_letter(account, user_app_id, device_name, instance_id, document_type, controle_code)
      destroy_apps_with_current_instance_id(account.app_authenticators, instance_id)
      account.app_authenticators.create(
        user_app_id: user_app_id,
        status: Authenticators::AppAuthenticator::Status::PENDING,
        activation_code: controle_code,
        device_name: device_name,
        issuer_type: "gemeentebalie",
        instance_id: instance_id,
        geldigheidstermijn: ::Configuration.get_int("geldigheid_brief"),
        requested_at: Time.now,
        substantieel_activated_at: Time.zone.now, # TODO: deze tijd klopt niet in geval van activeren na ontvangen brief
        substantieel_document_type: document_type
      )
    end

    def date_of_birth_day(date_of_birth)
      date_of_birth[8..-1]
    end

    def date_of_birth_month(date_of_birth)
      date_of_birth[5, 2]
    end

    def date_of_birth_year(date_of_birth)
      date_of_birth[0, 4]
    end

    def document_expired?(document)
      document.vervaldatum < Time.zone.now || document.onderzoek? || document.vermist?
    end

    def less_than_max_amount_of_apps?(bsn, instance_id)
      sector_ids = Sector.fetch(Sector.get("bsn"))
      number_of_active_or_pending_apps = Account.account_already_exists(bsn, sector_ids).first&.app_authenticators&.active_or_pending&.count.to_i
      number_of_active_or_pending_apps_belonging_to_instance_id = Account.account_already_exists(bsn, sector_ids).first&.app_authenticators&.active_or_pending&.where(instance_id: instance_id)&.count.to_i

      if number_of_active_or_pending_apps > 1  && number_of_active_or_pending_apps_belonging_to_instance_id > 0
        number_of_active_or_pending_apps - number_of_active_or_pending_apps_belonging_to_instance_id < max_amount_of_apps
      else
        number_of_active_or_pending_apps.nil? || number_of_active_or_pending_apps < max_amount_of_apps
      end
    end

    def max_amount_of_apps
      @max_amount_of_apps ||= ::Configuration.get_int("Maximum_aantal_DigiD_apps_eindgebruiker")
    end

    def check_json_body
      render(json: { message: "Only JSON body is accepted" }, status: 400) unless request.content_type == "application/json"
    end

    def check_switches
      if !Switch.request_station_enabled?
        Log.instrument("1398", hidden: true)
        render json: { status: "NOK", error_code: "E_FEATURE_NOT_AVAILABLE" }
      elsif !Switch.digid_app_enabled?
        Log.instrument("1397", hidden: true)
        render json: { status: "NOK", error_code: "E_FEATURE_NOT_AVAILABLE" }
      end
    end
  end
end
