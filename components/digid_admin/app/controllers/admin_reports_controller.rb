
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

class AdminReportsController < ApplicationController
  skip_before_action :verify_authenticity_token, only: :alarm
  respond_to :html, :csv

  skip_before_action :authenticate!, only: [:alarm]
  before_action :authorize_on_report_type, except: [:alarm, :overview]

  def index
    @admin_reports = AdminReport.where(report_type: report_type).order(created_at: :desc).page(params[:page]).per(params[:per_page])
    code = case report_type
      when "monthly" then "367"
      when "weekly" then "368"
      when "std" then "369"
      when "fraud" then "370"
      when "integrity" then "371"
      when "sec" then "372"
    end
    instrument_logger(code, manager_id: session[:current_user_id]) if code
    report_selector_options = @admin_reports.map do |r|
      if r.interval_start && r.interval_end
        "<option>#{I18n.l(r.interval_start.to_date, format: :default)} - #{I18n.l(r.interval_end.to_date, format: :default)}</option>"
      end
    end
    @report_selector = report_selector_options.uniq.to_s
    case report_type
      when "adhoc_log"
        instrument_logger('609')
      when "adhoc_gba"
        instrument_logger('610')
      when "adhoc_tx"
        instrument_logger('611')
    end
  end

  def show
    admin_report = AdminReport.where(report_type: report_type).find(params[:id])
    send_data admin_report.csv_content,
              filename: "#{admin_report.name}.csv",
              disposition: 'attachment',
              type: 'csv'
    instrument_logger('uc28.rapportage_download_gelukt', bestand: "#{admin_report.name}.csv")
  end

  def overview
    authorize! :read_overview, AdminReport
    @search = AdminReport.filtered.ransack(params[:q])
    if params[:q]
      @admin_reports = @search.result(distinct: true).where(report_type: permissions).page(params[:page])
      instrument_logger('admin_report.filter_overview_success', subject_type: Log::SubjectTypes::ADMIN_REPORT)
    else
      @admin_reports = AdminReport.overview.where(report_type: permissions).page(params[:page]).per(params[:per_page])
      instrument_logger('admin_report.overview_success', subject_type: Log::SubjectTypes::ADMIN_REPORT)
    end
    @permissions = permissions_human
  end

  def download_bundle
    admin_reports = AdminReport.where(report_type: report_type)

    return redirect_to admin_reports_path(report_type: report_type), flash: { alert: 'Er zijn geen rapportages gevonden!' } if admin_reports.empty?

    dates = params[:report_bundler].split(' - ')
    admin_reports = admin_reports.where(interval_start: Time.parse(dates[0])..Time.parse(dates[1]))

    file = Tempfile.new('download_bundle.zip')

    Zip::ZipOutputStream.open(file.path) do |zos|
      admin_reports.each do |admin_report|
        zos.put_next_entry "#{admin_report.name}-#{admin_report.id}.csv"
        zos.puts admin_report.csv_content
      end
    end

    send_data file.read,
              filename: "all-#{params[:report_bundler].delete(' ')}.zip",
              disposition: 'attachment',
              type: 'application/zip'
  end

  def alarm
    AlarmNotifier::AdminReport.new(params).notify
    head 200
  end

  private

  def authorize_on_report_type
    authorize! :"read_#{report_type}", AdminReport
  end

  def report_type
    @report_type ||= params[:report_type]
  end

  def permissions_human
    perms = {}
    permissions.each do |report_type|
      perms[t("report.#{report_type}_short", default: t("report.#{report_type}"))] = report_type
    end
    perms
  end

  def permissions
    permissions = []
    %w(std integrity fraud sec weekly monthly).each do |report_type|
      permissions << report_type if can?(:"read_#{report_type}", AdminReport)
    end
    %w(adhoc_log adhoc_gba adhoc_tx).each do |report_type|
      permissions << report_type if can?(:"create_#{report_type.gsub('adhoc_', '')}", FraudReport)
    end
    permissions
  end
end
