
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

class FraudReportsController < ApplicationController
  before_action :can_do_fraud_reports
  before_action :check_report_name, only: [:create]
  before_action :illegal_combo?
  before_action :set_job_count_and_queue_time, only: [:new, :create]

  def index
    instrument_logger('608')
  end

  def new
    set_radio_button_report_type
  end

  def create # rubocop:disable Metrics/CyclomaticComplexity, Metrics/PerceivedComplexity
    content = params[:content].delete("\r").tr(';', ',').split("\n").join(',').split(',').uniq.delete_if(&:blank?)
    fraud_bsn_or_ip = (%w(bsn ip zip).include? params[:fraud_bsn_or_ip]) ? params[:fraud_bsn_or_ip] : 'bsn'

    content = Registration.find_bsns_from_postcodes(content) if fraud_bsn_or_ip.eql?('zip')
    if content.size > 100 || content.empty?
      flash.now[:alert] = 'Te veel data opgegeven (maximaal 100 items)' if content.size > 100
      if content.empty? && fraud_bsn_or_ip.eql?('zip')
        flash.now[:alert] = 'Geen burgerservicenummers gevonden voor de opgegeven postcode(s).'
      elsif content.empty?
        flash.now[:alert] = 'Geen data opgegeven'
      end
    else
      report_type = (%w(log account transactie).include? params[:report_type]) ? params[:report_type] : 'log'

      case report_type
      when 'log'
        create_log_report_request(fraud_bsn_or_ip, content)
        message = case fraud_bsn_or_ip
                  when 'bsn' then 'uc28.rapportage_aanvraag_logging_bsn_onderzoek_gelukt'
                  when 'ip'  then 'uc28.rapportage_aanvraag_logging_ip_onderzoek_gelukt'
                  when 'zip' then 'uc28.rapportage_aanvraag_logging_zip_onderzoek_gelukt'
                  end
        instrument_logger(message, subject_type: 'FraudReport')
      when 'account'
        message = case fraud_bsn_or_ip
                  when 'bsn' then 'uc28.rapportage_aanvraag_account_bsn_onderzoek_gelukt'
                  when 'ip'  then 'uc28.rapportage_aanvraag_account_ip_onderzoek_gelukt'
                  when 'zip' then 'uc28.rapportage_aanvraag_account_zip_onderzoek_gelukt'
                  end
        instrument_logger(message, subject_type: 'FraudReport')
        create_gba_requests(content)
      when 'transactie'
        create_transactie_report_request(content)
        instrument_logger('613', subject_type: 'FraudReport')
      end

      report_name = params[:report_name].blank? ? 'geen naam opgegeven' : params[:report_name]
      flash.now[:notice] = "Rapportage (#{report_name}) staat gepland met #{content.size} " + 'item'.pluralize(content.size)
    end

    set_radio_button_report_type
    render :new
  end

  private

  def can_do_fraud_reports
    return if can?(:create_log, FraudReport) || can_fraud_gba_helper || can?(:create_tx, FraudReport)
    raise CanCan::AccessDenied
  end

  def illegal_combo?
    return unless !params[:fraud_bsn_or_ip].eql?('bsn') && params[:report_type].eql?('transactie')

    flash.now[:alert] = 'Ongeldige combinatie (BSN / Transactiegegevens)'
    render :new
  end

  # / \ ? & * : | (pipe) " (quote) < > zijn niet toegestaan.
  def check_report_name
    return unless params[:report_name] =~ %r{\/|\\|\?|\&|\*|:|\||\"|<|>}

    flash.now[:alert] = 'Illegaal karakter in Rapport naam (/\\?&*:|<> zijn niet toegestaan.)'
    render :new
  end

  def create_log_report_request(fraud_bsn_or_ip, content)
    report_request = LogReportRequestJob.perform_async(
      report_type:    'log',
      periode_start:  date_select_helper(:period, 'from'),
      periode_end:    date_select_helper(:period, 'to'),
      data_type:      fraud_bsn_or_ip,
      data:           content,
      report_name:    params[:report_name],
      manager_id:     session[:current_user_id]
    )
  end

  def create_gba_requests(content)
    fraud_report = FraudReport.new(report_name: params[:report_name], manager_id: session[:current_user_id], bsns: content)
    fraud_report.schedule
  end

  def create_transactie_report_request(content)
    report_request = TransactieReportRequestJob.perform_async(
      report_type:    'tx',
      data:           content,
      report_name:    params[:report_name],
      manager_id:     session[:current_user_id]
    )
  end

  def set_radio_button_report_type
    if can?(:create_log, FraudReport)
      @report_preselect = 'log'
    elsif can_fraud_gba_helper
      @report_preselect = 'gba'
    elsif can?(:create_tx, FraudReport)
      @report_preselect = 'tx'
    end
  end

  def can_fraud_gba_helper
    can?(:create_gba, FraudReport) && can?(:gba_request, Account)
  end

  def set_job_count_and_queue_time
    @jobs_count = Sidekiq::Queue.new('bulk-brp').size
    @queue_time = [0, SchedulerBlock.find_or_create('bulk-brp').candidate - Time.zone.now].max
  end
end
