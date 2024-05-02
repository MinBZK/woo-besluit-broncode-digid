
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

module FrontendApi
  class DataController < ApplicationController
    include RvigClient
    include RdwClient
    include DwsClient

    include SwitchHelper
    include MyDigidHelper

    before_action :find_account
    before_action :check_session_time, except: :show_config
    before_action :update_session, only: :touch_session
    before_action :verify_authenticity_token, only: :touch_session

    rescue_from(SessionExpired, with: :my_digid_session_expired)

    def news_item
      if params[:page].present?
        items = news_items(params[:page]).map do |item|
          { name: item.send("name_#{I18n.locale}"), body: item.send("body_#{I18n.locale}")}
        end
      end

      render json: { news_items: items || []}
    end

    def get_session_data
      render json: {
        timestamp: wrapped_session.try(:expires_at),
        minutes: ::Configuration.get_int("session_expires_in"),
        csrf: form_authenticity_token
      }
    end

    def touch_session
      render json: { timestamp: wrapped_session.try(:expires_at) }
    end

    def app_data
      apps = @account.app_authenticators.active_or_pending.map do |app|
        attributes = app.attributes.with_indifferent_access
        attributes[:substantieel_activated_at] ||= attributes[:wid_activated_at]
        attributes[:substantieel_document_type] ||= attributes[:wid_document_type]
        attributes.slice(*app_authenticator_fields).merge(app_code: app.app_code)
      end

      render json: {
        status: digid_app_enabled? ? "OK" : "DISABLED",
        app_authenticators: apps
      }
    end

    def account_data
      phone_number = @account.sms_in_uitbreiding? || @account.mobiel_kwijt_in_progress? ? @account.pending_phone_number : @account.phone_number

      render json: {
        status: "OK",
        combined_account_data: {
          bsn: mask_personal_number(@account.bsn),
          email: @account.email,
          gesproken_sms: (@account.sms_tools.active? && @account.gesproken_sms) || (@account.pending_activatable? && @account.pending_gesproken_sms),
          phone_number: mobile_number(mobile_number: phone_number),
          pref_lang: @account.locale,
          sms: @account.sms_tools.last&.status || "inactive",
          two_faactivated_date: @account.last_change_security_level_at,
          username: @account.password_authenticator&.username,
          zekerheidsniveau: @account.zekerheidsniveau,
          deceased: @account.deceased?,
          last_sign_in_at: @account.last_sign_in_at
        }
      }
    end

    def log_data
      subquery = Sectorcode.select(:sectoraalnummer).where(account_id: @account.id).map(&:sectoraalnummer)
      all_account_ids = Sectorcode.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
      all_account_ids << SectorcodesHistory.select("DISTINCT account_id").where(sectoraalnummer: subquery).map(&:account_id)
      all_account_ids = all_account_ids.flatten.uniq

      page_size = params[:size].presence&.to_i || 10
      transactions_scope = params[:query].present? && !APP_CONFIG["skip_log_search"] ? AccountLog.history_search(all_account_ids, params[:query]) : AccountLog.history(all_account_ids)
      @transactions = transactions_scope.page(params[:page] || 0).per(page_size)

      render json: {
        status: "OK",
        account_logs: @transactions,
        total_items: transactions_scope.size,
        total_pages: (transactions_scope.size / page_size.to_f).ceil
      }
    end

    def documents_data
       begin
        if Switch.driving_licence_partly_enabled?
          @driving_licences = fetch_driving_licences

          # check if documents are allowed to reset pin
          if Switch.driving_licence_pin_reset_enabled? && @driving_licences.present?
            @driving_licences.map do |dl|
              begin
                response = check_puk_request(dl, current_account.bsn)
              rescue => e
                 Rails.logger.info("ERROR: DWS call mislukt: #{e.message}")
              end

              next unless response.present?

              dl[:allow_pin_reset] = response["status"] == "OK"
            end
          end

          log_invalid_driving_licence_status(@driving_licences, @account) if @driving_licences.present?
        else
          @rdw_error = "switch_off"
        end
      rescue DigidUtils::DhMu::RdwError => e
        handle_errors(e)
      end

      begin
        if Switch.identity_card_partly_enabled?
          @identity_cards = fetch_identity_cards
          log_invalid_identity_card_status(@identity_cards, @account) if @identity_cards.present?
        else
          @rvig_error = "switch_off"
        end
      rescue DigidUtils::DhMu::RvigError => e
        handle_errors(e)
      end

      @identity_cards ||= []
      @driving_licences ||= []

      formatter = ->(i, type) {{
        doc_num: mask_personal_number(i.document_no),
        sequence_no: i.sequence_no,
        activated_at: i.last_updated_at.to_s,
        active: i.active?,
        status: i.human_status,
        status_mu: formatted_status(i.try(:status_mu)),
        existing_unblock_request: @account.unblock_letter_requested?(bsn: @account.bsn, sequence_no: i.sequence_no, card_type: type),
        paid: i.try(:eidtoeslag) != "Niet betaald",
        allow_pin_reset: i[:allow_pin_reset] || false
      }}

      render json: {
        status: "OK",
        driving_licences: @driving_licences.map { |i| formatter.call(i, "NL-Rijbewijs") },
        show_driving_licences: show_driving_licence?(@account.bsn),
        pin_reset_driving_licences: driving_licence_pin_reset_enabled? || driving_licence_pin_reset_partly_enabled?,
        identity_cards: @identity_cards.map { |i| formatter.call(i, "NI") },
        show_identity_cards: show_identity_card?(@account.bsn),
        rdw_error: @rdw_error,
        rvig_error: @rvig_error
      }
    end

    def notification
      result = {}

      result[:information] = flash[:notice] if flash[:notice].present?
      result[:error] = flash[:alert] if flash[:alert].present?

      render json: result
    end

    def show_config
      render json: {
        analytics: {
          enabled: APP_CONFIG["analytics"]["enabled"],
          site_id: APP_CONFIG["analytics"]["site_id"],
          host: APP_CONFIG["analytics"]["host"]
        }
      }
    end

    private

    def find_account
      @account = Account.find(session[:account_id]) if session[:account_id].present?

      if @account.nil?
        render json: { message: "Session expired." }, status: 401
      end
    end


    def my_digid_session_expired
      remove_session
      reset_session
      render json: { message: "Session expired." }, status: 401
    end

    def handle_errors(error)
      Rails.logger.error "Start handling RDW/RvIG error"

      case error
      when DigidUtils::DhMu::RdwFault
        @rdw_error = parse_rdw_fault(error)
      when DigidUtils::DhMu::RdwTimeout, DigidUtils::DhMu::RdwHttpError
        @rdw_error = "try_again"
        Log.instrument("927", account_id: @account.id, hidden: true)
      when DigidUtils::DhMu::RvigFault
        @rvig_error = parse_rvig_fault(error)
      when DigidUtils::DhMu::RvigTimeout, DigidUtils::DhMu::RvigHttpError
        @rvig_error = "try_again"
        Log.instrument("932", account_id: @account.id, hidden: true)
      end
    end

    def parse_rdw_fault(error)
      if error.code == "OG1"
        Log.instrument("1044", message: error.message, account_id: current_account.id, hidden: true)
        "try_again"
      elsif error.code.present?
        Log.instrument("1044", message: error.message, account_id: current_account.id, hidden: true)
        "contact"
      else
        Rails.logger.error "ERROR: No response from RDW"
        Log.instrument("927", account_id: current_account.id, hidden: true)
        "contact"
      end
    end

    def parse_rvig_fault(error)
      if error.message == "TechnicalFault" # rvig OG1 in use case documentation
        Log.instrument("1045", message: error.message, account_id: current_account.id, hidden: true)
        "try_again"
      elsif error.message == "NotFound" # rvig OG2 in use case documentation
        Log.instrument("1045", message: error.message, account_id: current_account.id, hidden: true)
        "contact"
      else
        Rails.logger.error "ERROR: No response from RvIG"
        Log.instrument("932", account_id: current_account.id, hidden: true)
        "contact"
      end
    end

    def log_invalid_driving_licence_status(driving_licences, account)
      # Log invalid driving licences once per session
      return if session[:driving_licence_invalid_logged].present?

      driving_licences.each do |driving_licence|
        unless driving_licence.valid?
          session[:driving_licence_invalid_logged] = true
          Log.instrument("1050", status: driving_licence.status, account_id: @account.id, hidden: true) if !driving_licence.status_valid?
          Log.instrument("1050", status: driving_licence.status_mu, account_id: @account.id, hidden: true) if !driving_licence.status_mu_valid?
        end
      end
    end

    def log_invalid_identity_card_status(identity_cards, account)
      # Log invalid driving licences once per session
      return if session[:identity_card_invalid_logged].present?
      identity_cards.each do |identity_card|
        if !identity_card.valid?
          session[:identity_card_invalid_logged] = true
          Log.instrument("1051", status: identity_card.status, account_id: @account.id, hidden: true) if !identity_card.status_valid?
        end
      end
    end

    def app_authenticator_fields
      [:activated_at, :created_at, :device_name, :id, :app_code, :substantieel_activated_at, :substantieel_document_type, :last_sign_in_at, :status]
    end

    def check_puk_request(driving_licence, bsn)
      dws_client.check_puk_request(sequence_no: driving_licence[:e_id_volg_nr], document_type: "NL-Rijbewijs", bsn: bsn)
    end

    def formatted_status(mu_status)
      {
        "Actief": "actief",
        "Inactief": "niet_actief",
        "Gerevoceerd": "ingetrokken"
      }[mu_status&.to_sym] || "actief"
    end
  end
end
