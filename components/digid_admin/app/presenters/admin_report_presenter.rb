
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

class AdminReportPresenter < BasePresenter
  presents :admin_report

  delegate :id, :name, :report_type, :batch_started_at, :report_started_at, :report_ended_at, :result, :lines, to: :admin_report

  def interval_start
    @object.interval_start && format_date(@object.interval_start.to_date)
  end

  def interval_end
    @object.interval_end && format_date(@object.interval_end.to_date)
  end

  def report_type
    t("report.#{@object.report_type}_short", default: t("report.#{@object.report_type}"))
  end

  def batch_started_at
    return l(@object.batch_started_at, format: :default) if @object.batch_started_at.present?
    ''
  end

  def report_started_at
    return l(@object.report_started_at, format: :default) if @object.report_started_at.present?
    ''
  end

  def report_ended_at
    return l(@object.report_ended_at, format: :default) if @object.report_ended_at.present?
    ''
  end

  def duration
    if @object.report_started_at.present? && @object.report_ended_at.present?
      total_seconds = @object.report_ended_at - @object.report_started_at
      seconds = total_seconds % 60
      minutes = (total_seconds / 60) % 60
      hours = total_seconds / (60 * 60)
      return format('%02d:%02d:%02d', hours, minutes, seconds)
    else
      ''
    end
  end

  def result
    @object.result ? t('yes') : t('no')
  end

  def manager
    return unless @object.manager_id
    manager = Manager.find(@object.manager_id)
    manager.name if manager
  end
end
