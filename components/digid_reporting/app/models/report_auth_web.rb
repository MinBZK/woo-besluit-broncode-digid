
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

#
# CLASSNAME : repSecAuthWebdnstMnt
# BRON  : week_maandRaportage.csv rij 4
# RETURN: maand, webservices.name, webservices.Id, aselect_webservices.app_id, logs.sector_naam,aantal
# JOINS : logs.webservice_id = webservices.id AND webservices.id=aselect_webservices.id
# WHERE : "uc2.authenticeren_basis_gelukt"  => 102017,
#         "uc2.authenticeren_midden_gelukt" => 102018,
class ReportAuthWeb < AdminReport

  def initialize(period = ReportParam::WEEK, labels=nil)
    @report_name = 'Aantal gelukte authenticaties per webdienst per sector.'
    @report_class_name = 'ReportAuthWeb'
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Instantiate report class #{@report_class_name} Report range is a #{period}"
    @rep_param = prepare_report_param(period, Time.now, @report_class_name)
    @header = ["periode", "webservice name", "webservice id", "appid", "sector name", "counter"]
    @uc_labels = labels
    @period = period
    @idx_hint = "#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)"
  end

  #
  # Run the report and return an array with the results
  def report (start_date = nil)
    # ToDo Needs refinement
    @rep_param = prepare_report_param(@period, start_date, @report_class_name)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> Generate report with startdate #{start_date}"

    counts = Log.from(@idx_hint).where(
      :created_at => (@rep_param.start_ts..@rep_param.end_ts),
      :code       => lookup_codes(@uc_labels, nil)
    ).group(:webservice_id, :sector_name).count

    result = list_webservices(counts)
    logger.info "DEBUG #{me}  ===>  Result is: #{result}"

    return result
  end

  private

  #
  # Extracts all webservices from db, if service has aselect
  # enrich with application id. Counts number of authenticatons for
  # the given period and add counters to webservice.
  def list_webservices(qry_result)
    me = "#{@report_class_name}.#{__method__}"
    logger.debug "DEBUG #{me}  ===> listing webservices for #{qry_result.count} authentications."
    result = [@header]
    month = @rep_param.period_value
    Webservice.all.each do |webs|
      webs_id = webs.id
      webs_name = webs.name
      if webs.aselect_webservice.nil?
        webs_aselect_webservice_app_id =  ""
      else
        webs_aselect_webservice_app_id =   webs.aselect_webservice.app_id
      end
      # Mantis 9060, always show a sector. If undefined, use first sector.
      if webs.sectors.first.nil?
        default_sector = ""
      else
        default_sector = webs.sectors.first.name
      end
      ws_in_logs = false
      qry_result.select{|key, val|key.include?(webs.id)}.each do |key_arr, cnt|
        sector = key_arr[1]
        sector = default_sector if sector.blank?
        result << [month, webs_name, webs_id, webs_aselect_webservice_app_id, sector, cnt]
        ws_in_logs = true
      end
      if !ws_in_logs
        result << [month, webs_name, webs_id, webs_aselect_webservice_app_id, default_sector, 0]
      end
    end
    logger.debug "DEBUG #{me}  ===> result is #{result}."
    return result
  end
end
