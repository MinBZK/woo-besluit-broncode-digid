
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

class IapiController < ApplicationController
  skip_before_action :verify_authenticity_token, except: [:update_cronjobs]
  authorize_with_token :iapi_token, except: [:update_cronjobs]

  def log_definitions
    log_definitions = {}
    YAML.load_file("#{Rails.root}/config/locales/nl/log.nl.yml")["nl"]["digid"].each do |key, value|
      log_definitions[key] = { translation: value, id_number: key } if key.to_i > 0
    end
    SYSTEM_MONITOR_MAPPINGS.each do |key, value|
      log_definitions[key] = { translation: t("digid.#{key}"), id_number: value }
    end
    render json: log_definitions
  end

  def app_instances
    safe_params = params.require(:iapi).permit!
    app_authenticators = Account.with_bsn(safe_params[:bsn]).last&.app_authenticators&.order("status ASC")
    if app_authenticators.present?
      render json: { results: app_authenticators.pluck(:id, :user_app_id, :instance_id, :device_name, :status, :symmetric_key) }
    else
      render json: { results: [] }
    end
  end

  def log
    safe_params = params.require(:iapi).permit!
    payload = safe_params[:payload].to_hash.symbolize_keys

    payload[:app_code] ||= Authenticators::AppAuthenticator.where(user_app_id: payload[:user_app]).first&.app_code
    payload[:human_process] = t("process_names.log.#{payload[:human_process]}", locale: :nl) if payload[:human_process].present?
    payload[:document_type] = t(payload[:document_type].downcase, scope: :document, locale: :nl) if payload[:document_type].present?

    if safe_params[:key] == "digid_ns_send_notifications"
      log_ns(safe_params)
      return
    end

    accounts = []
    if safe_params.has_key?(:a_number) || safe_params.has_key?(:bsn)
      accounts = Account.with_anummer(safe_params[:a_number]) if safe_params.has_key?(:a_number)
      accounts = Account.with_bsn(safe_params[:bsn]) if accounts.empty? && safe_params.has_key?(:bsn)
      return render json: { error: "DIGID_ACCOUNT_NOT_FOUND" }, status: 200 if accounts.empty?
    end

    if accounts.size > 0
      accounts.each do |account|
        payload[:account_id] = account.id
        Log.instrument(safe_params[:key], payload)
      end
    else
      Log.instrument(safe_params[:key], payload)
    end

    head :no_content
  end

  def sms
    options = params.require(:sms).permit(:account_id, :gateway, :timeout, :sender, :message, :phone_number, :code_gid, :spoken)
    if options[:phone_number].blank?
      account = Account.find(options[:account_id])
      options[:phone_number] = account.phone_number || account.pending_phone_number
    end
    if options[:spoken] == "true"
      options[:spoken] = true
    elsif options[:spoken] == "false"
      options[:spoken] = false
    end
    SmsJob.perform_async(options.to_h.to_json)
    head :no_content
  end

  def letter
    options = params.require(:letter).permit(:account_id, :sequence_no, :card_type, :action)

    service = RemoteLetterService.new account_id: options[:account_id], sequence_no: options[:sequence_no], card_type: options[:card_type], action: options[:action]
    if service.valid?
      service.prepare_letter
      render json: { status: "success" }
    else
      render json: { status: "error", message: service.error_message, next_registration_date: service.next_registration_date }
    end
  end

  def task
    safe_params = params.require(:iapi).permit!
    task_name = safe_params[:name]

    ExecuteXCronJob.perform_async(task_name)
    head :no_content
  end

  def reupload_letter
    UploadLettersFileJob.new.perform_single(params.require(:id))

    head :no_content
  end

  private
  def log_ns (safe_params)
    payload = safe_params[:payload].to_hash.symbolize_keys
    if payload[:successful_apps].present?
      notified_apps = payload[:successful_apps]
      notified_apps.each do |app|
        app_authenticator = Authenticators::AppAuthenticator.where(user_app_id: app["appId"]).first
        Log.instrument("1454", title: payload[:title], app_code: app_authenticator&.app_code, device_name: app["deviceName"], account_id: app_authenticator&.account_id)
      end
    end
    if payload[:failed_apps].present?
      failed_notified_apps = payload[:failed_apps]
      failed_notified_apps.each do |app|
        app_authenticator = Authenticators::AppAuthenticator.where(user_app_id: app["appId"]).first
        Log.instrument("1455", title: payload[:title], app_code: app_authenticator&.app_code, device_name: app["deviceName"], account_id: app_authenticator&.account_id, hidden: true)
      end
    end
  end
end
