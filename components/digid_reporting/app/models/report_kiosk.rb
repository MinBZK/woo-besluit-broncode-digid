
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

# Report app counters
#
# The reports counts usage of the DigiD Kiosk.
# The numbers of events are counted by logging codes from the logs
# table.
class ReportKiosk < AdminReport
  # Construct a report object. The period parameter describe the report
  # period. Default is a week.
  # The period is a constant from the ReportParam class.
  def initialize(period = ReportParam::Week)
    @report_class_name = 'ReportKiosk'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "beschrijving van de teller", "aantal"]
    @period = period
    @report_name = 'Overzicht gebruik Kiosk'
    @nr_active_appauthenticators = 0
    @nr_active_app_without_sms = 0
    # List of counters in this report.
    @counters = [
        "report_start_verhogen_substantieel_via_kiosk",
        "report_successful_verhogen_substantieel_via_kiosk",
        "report_failed_verhogen_substantieel_via_kiosk",
        "report_cancelled_verhogen_substantieel_via_kiosk",
        "report_error_kiosk_switch_off",
        "report_error_kiosk_invalid_version",
        "report_error_kiosk_missing_headers",
        "report_error_kiosk_unknown",
        "report_error_kiosk_inactive",
        "report_error_authentication_failed",
        "report_error_no_bsn"
    ]
  end

  # Generic worker method. Will be called by it's
  # siblings ReportApp<period>
  #
  # The start_date parameter must lay in the requested period. The default,
  # when no parameter is supplied, is the last, completed, period.
  # I.E. last week or last month.
  #
  # This report consists of a number counters, each require their own
  # database query. Queries are execute in their own method and added
  # to the resulting CSV.
  #
  # Returns the generated data as CSV with a header row.
  def report(start_date = nil)
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    result = [@header]  #result = []

    # Build the report
    @counters.each do |counter|
      result << [@rep_param.period_value.to_s, send(counter)].flatten
    end

    logger.info "INFO -> #{@report_class_name} ===> Result: #{result}"
    return result
  end

  def report_start_verhogen_substantieel_via_kiosk
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal gestarte verhogingen"
    count = count_logcodes(['uc5.kiosk_app_to_substantial_start'])
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_successful_verhogen_substantieel_via_kiosk
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal geslaagde verhogingen"
    count = count_logcodes(['uc5.kiosk_app_to_substantial_successful'])
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_failed_verhogen_substantieel_via_kiosk
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal mislukte verhogingen"
    count_started = count_logcodes(['uc5.kiosk_app_to_substantial_start'])
    count_succesful = count_logcodes(['uc5.kiosk_app_to_substantial_successful'])
    count = count_started - count_succesful
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_cancelled_verhogen_substantieel_via_kiosk
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Aantal geannuleerde verhogingen"
    count = count_logcodes('uc5.kiosk_app_to_substantial_failed_cancelled')
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_kiosk_switch_off
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Switch kiosk staat uit"
    count = count_logcodes('uc5.kiosk_app_to_substantial_switch_kiosk_disabled')
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_kiosk_invalid_version
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Ongeldige kioskversie"
    count = count_logcodes(['digid_kiosk.update_warning', 'digid_kiosk.force_update', 'digid_kiosk.kill_app', 'digid_kiosk.unknown_version'])
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_kiosk_missing_headers
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Ontbrekende headers"
    count = count_logcodes('digid_kiosk.missing_headers')
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_kiosk_unknown
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Kiosk onbekend"
    count = count_logcodes('uc5.kiosk_app_to_substantial_kiosk_unknown')
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_kiosk_inactive
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Kiosk niet actief"
    count = count_logcodes(['uc5.kiosk_app_to_substantial_kiosk_not_active'])
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_authentication_failed
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Authenticatie mislukt"
    count = count_logcodes('uc5.kiosk_app_to_substantial_authentication_failed')
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  def report_error_no_bsn
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate data"
    name = "Foutsituatie: Account heeft geen BSN"
    count = count_logcodes(['uc5.kiosk_app_to_substantial_authentication_failed_no_bsn'])
    result = [name, count]
    logger.debug "DEBUG -> #{me} ===> result: #{result}"
    return result
  end

  private

  def count_logcodes(uc_labels)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> count all occurences of the given logcodes: #{uc_labels}"
    result = 0
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    codes = lookup_codes(uc_labels, nil)
    result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => codes).count
    return result
  end

  def subtract_logcodes(subtract_labels)
    uc_label = subtract_labels.shift
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> subtract all occurences of the given logcodes: #{uc_label} from the following logcodes: #{subtract_labels}"
    result = 0
    idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
    codes = lookup_codes(uc_label, nil)
    subtract_codes = lookup_codes(subtract_labels, nil)
    result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => codes).count
    subtract_result = Log.from(idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => subtract_codes).count
    return result - subtract_result
  end

end
