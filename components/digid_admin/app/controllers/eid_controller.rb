
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

class EidController < ApplicationController
  include Concerns::Gba

  before_action :load_account
  before_action :check_mu_switch, only: [:block_wid, :pin_wid]
  before_action :check_eid_update_privilege, only: [:block_wid, :deblock_driving_licence, :pin_wid]

  def view_wids
    if can?(:read, Account) && (can?(:update, Eid::Mu) || can?(:view, Eid::Mu))
      if Switch.rijbewijs_partly_enabled?
        @account = Account.find(params[:id])
        begin
          @driving_licences = rdw_client.get(bsn: @account.bsn).sort_by(&:last_updated_at).reverse
          instrument_logger('933', account_id: @account.id)
        rescue DigidUtils::DhMu::RdwFault => e
          Rails.logger.error e.message
          instrument_logger('digid_hoog.status_rijbewijs_rdw_fault', account_id: @account.id, message: e.message)
          @rdw_fault = true
        rescue => e
          instrument_logger('digid_hoog.status_rijbewijs_rdw_fault', account_id: @account.id, message: e.message)
          Rails.logger.error e.message
          @rdw_error = true
        end
      else
        @rdw_error = true
      end
      instrument_logger('928', account_id: @account.id) if @rdw_error

      if Switch.identiteitskaart_partly_enabled?
        begin
          @identity_cards = rvig_client.get(bsn: @account.bsn).sort_by(&:date_time_status).reverse
          instrument_logger('934', account_id: @account.id)
        rescue DigidUtils::DhMu::RvigFault => e
          Rails.logger.error e.message
          instrument_logger('digid_hoog.status_identity_card_rvig_fault', account_id: @account.id, message: e.message)
          @rvig_fault = true
        rescue => e
          Rails.logger.error e.message
          @rvig_error = true
        end
      else
        @rvig_error = true
      end
      instrument_logger('929', account_id: @account.id) if @rvig_error

      render json: {
        target: "#mu_response", body: render_to_string(partial: "accounts/mu_request_form")
      }
    end
  end

  def block_wid
    doc_type = params[:document_type]
    begin
      instrument_logger('uc16.account_mu_block_gestart', account_id: @account.id, mydigid: true, doc_type: t(doc_type))

      if doc_type == 'driving_licence'
        rdw_client.update(bsn: @account.bsn, sequence_no: params[:seq_nr], status: "Geblokkeerd")
      else
        rvig_client.update(bsn: @account.bsn, sequence_no: params[:seq_nr], status: "Geblokkeerd")
      end

      instrument_logger("uc16.account_#{doc_type}_block_gelukt", account_id: @account.id, mydigid: true, account_transactie: true)
      notify_account(account: @account, doc_type: doc_type, type: 'block')
      flash[:notice] = I18n.t('letters.forms.messages.success_block', doc_type: t(doc_type))
    rescue DigidUtils::DhMu::RdwFault, DigidUtils::DhMu::RdwError, DigidUtils::DhMu::RvigFault, DigidUtils::DhMu::RvigError => error
      Rails.logger.error "#{error.message}"
      log_mu_failure(account: @account, doc_type: doc_type, type: :block, message: error.message)
      if error.code == "SW4"
        flash[:alert] = I18n.t('letters.forms.messages.failure_block_not_allowed', doc_type: t(doc_type))
      else
        flash[:alert] = I18n.t('letters.forms.messages.failure_block', doc_type: t(doc_type))
      end
    end

    redirect_to account_path(@account.id)
  end

  def deblock_wid
    begin
      instrument_logger('uc16.account_iapi_deblock_gestart', account_id: @account.id, mydigid: true, doc_type: t(params[:document_type]))
      gba = gba_data(@account.bsn)

      gba.status = 'investigate' if gba.onderzoek_adres?

      if gba.status == 'valid'
        letter_options = {
            account_id: @account.id,
            sequence_no: params[:seq_nr],
            card_type: params[:document_type] == 'driving_licence' ? 'NL-Rijbewijs' : 'NI',
            action: "unblock"
        }

        if register_send_letter(letter_options)
          notify_account(account: @account, doc_type: params[:document_type], type: 'deblock')
          flash[:notice] = I18n.t('letters.forms.messages.success_deblock', doc_type: t(params[:document_type]))
        end
      else
        gba_status = gba.status == "rni" ? "emigrated" : gba.status # rni & emigrated is een logregel.
        Rails.logger.error "digid_hoog.request_unblock_letter.abort.invalid_#{gba_status}"
        instrument_logger("digid_hoog.request_unblock_letter.abort.invalid_#{gba_status}", account_id: @account.id, account_transactie: true)
        flash[:alert] = I18n.t("unblock_letter.#{gba_status}")
      end
    rescue HTTPClient::ReceiveTimeoutError => error
      flash[:alert] = I18n.t('letters.forms.messages.failure_timeout')
      instrument_logger('uc16.account_iapi_deblock_timeout', account_id: @account.id, account_transactie: true)
      Rails.logger.error "#{error.message}"
    rescue => error
      Rails.logger.error "#{error.message}"
    end

    redirect_to account_path(@account.id)
  end

  def pin_wid
    doc_type = params[:document_type]

    begin
      instrument_logger('1038', account_id: @account.id, mydigid: true, doc_type: t(doc_type))

      if doc_type == 'driving_licence'
        rdw_client.request_pinpuk_letter(bsn: @account.bsn, sequence_no: params[:seq_nr])
      else
        rvig_client.request_pin_mailer(bsn: @account.bsn, sequence_no: params[:seq_nr])
      end

      instrument_logger('1043', account_id: @account.id, mydigid: true, account_transactie: true, doc_type: t(doc_type))
      notify_account(account: @account, doc_type: doc_type, type: 'pin')
      flash[:notice] = I18n.t('letters.forms.messages.success_pin')
    rescue DigidUtils::DhMu::RdwFault, DigidUtils::DhMu::RdwError, DigidUtils::DhMu::RvigFault, DigidUtils::DhMu::RvigError => error
      Rails.logger.error "#{error.message}"
      log_mu_failure(account: @account, doc_type: doc_type, type: :pin, message: error.message)
      flash[:alert] = I18n.t('letters.forms.messages.failure_pin', reason: error.message, doc_type: t(doc_type))
    end

    redirect_to account_path(@account.id)
  end

  private

  def check_mu_switch
    doc_type = params[:document_type]
    type = action_name == 'block_wid' ? 'block' : 'pin'

    if doc_type == 'driving_licence'
      return if Switch.rijbewijs_partly_enabled?
    else
      return if Switch.identiteitskaart_partly_enabled?
    end

    instrument_logger("uc16.account_mu_#{type}_onmogelijk_switch_uit", account_id: @account.id, account_transactie: true, doc_type: t(doc_type))
    flash[:alert] = I18n.t("letters.forms.messages.failure_#{type}_mu_not_active", doc_type: t(doc_type), bron: doc_type == 'driving_licence' ? 'RDW' : 'RvIG')
    redirect_to account_path(@account.id)
  end

  def load_account
    @account = Account.find(params[:id])
  end

  def check_eid_update_privilege
    redirect_to account_path(@account.id) unless can?(:update, Eid::Mu) && can?(:read, Account)
  end

  def rdw_client
    @rdw_client ||= DigidUtils::DhMu::RdwClient.new({
      endpoint: APP_CONFIG.dig("urls", "external", "mu_endpoint"),
      open_timeout: ::Configuration.get_int('rdw_timeout') || 5,
      read_timeout: ::Configuration.get_int('rdw_timeout') || 5,
      log: Rails.env.test? ? false : true,
      namespaces: {'xmlns:a' => 'http://www.w3.org/2005/08/addressing'},
      status_source: "DigiD Servicecentrum"
    }.merge(ssl_params))
  end

  def rvig_client
    @rvig_client ||= DigidUtils::DhMu::RvigClient.new({
      endpoint: APP_CONFIG.dig("urls", "external", "mu_rvig_endpoint"),
      status_source: "DigiD Servicecentrum",
      log: Rails.env.test? ? false : true,
      logger: Rails.logger,
      pretty_print_xml: true
    }.merge(ssl_params))
  end

  def notify_account(account:, doc_type:, type:)
    if account.email_activated?
      NotificationMailer.delay(queue: "email-admin").send(:"notify_#{type}", account_id: account.id, doc_type: doc_type)
    elsif account.phone_number.present?
      send_sms(
        gateway: sms_gateway,
        timeout: ::Configuration.get_string_without_cache('sms_timeout'),
        sender: "DigiD",
        account_id: account.id,
        message: t("sms.messages.sms_message_#{type}", doc_type: t(doc_type, locale: account.locale), locale: account.locale),
        phone_number: DigidUtils::PhoneNumber.normalize(account.phone_number),
        code_gid: nil
      )
    end
  end

  def send_sms(options)
    begin
      iapi_x_client.post("/iapi/sms", sms: options)
    rescue => e
      instrument_logger('uc16.sms_versturen_mislukt', account_id: @account.id, account_transactie: true)
      Rails.logger.error e.message
    end
  end

  def register_send_letter(**options)
    begin
      response = iapi_x_client.post("/iapi/letter", letter: options)
      resp_obj = JSON.parse(response.body)
      return true if resp_obj["status"] == "success"

      letter_error_message(resp_obj["message"],resp_obj["next_registration_date"], options[:card_type]) if resp_obj["status"] == "error"
    rescue => e
      Rails.logger.error e.message
    end
    return false
  end

  def letter_error_message(error_message_key, date, doc_type)
    doc_type = doc_type == 'NL-Rijbewijs' ? 'this_driving_licence' : 'this_id_card'
    case error_message_key
    when "too_often" then flash[:alert] = I18n.t('letters.forms.messages.failure_deblock_too_often', this_doc_type: t(doc_type), date: date)
    when "too_soon" then flash[:alert] = I18n.t('letters.forms.messages.failure_deblock_too_soon', this_doc_type: t(doc_type))
    end
  end

  def iapi_x_client
    @iapi_x_client ||= DigidUtils::Iapi::Client.new(url: APP_CONFIG["urls"]["internal"]["x"], timeout: 15)
  end

  def log_mu_failure(account:, doc_type:, type:, message:)
    log_my_digid, log_admin = { block: ["1005", "1035"], pin: ["1042", "1041"] }[type]

    instrument_logger(log_my_digid, account_id: account.id, account_transactie: true, mydigid: true, doc_type: t(doc_type))
    instrument_logger(log_admin, account_id: account.id, account_transactie: true, reason: message, doc_type: t(doc_type))
  end

  def ssl_params
    {
      ssl_cert_key_file: APP_CONFIG["saml_ssl_cert_key_file"]&.path || "/home/ruby/www/digid/shared/config/#{APP_CONFIG['hosts']['was']}.key",
      ssl_cert_file: APP_CONFIG["saml_ssl_cert_file"]&.path || "/home/ruby/www/digid/shared/config/#{APP_CONFIG['hosts']['was']}.crt",
      ssl_cert_key_password: Rails.application.secrets.private_key_password,
      ssl_ca_cert_file: APP_CONFIG["gba_ssl_ca_cert_file"]&.path.present? ? Rails.root.join(APP_CONFIG["gba_ssl_ca_cert_file"]&.path).to_s : nil,
      ssl_verify_mode: :peer
    }
  end

  def sms_gateway
    gateways = APP_CONFIG['sms_gateway']['regular'].split(',')
    key = "sms_gateways_regular"
    Redis.current.lpop(key) || (Redis.current.rpush(key, gateways * 5) && Redis.current.lpop(key))
  end
end
