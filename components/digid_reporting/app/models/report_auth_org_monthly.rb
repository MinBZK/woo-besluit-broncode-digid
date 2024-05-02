
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

class ReportAuthOrgMonthly < AdminReport

  UC_LABELS = ['uc2.authenticeren_start_basis', 'uc2.authenticeren_start_midden', 'uc2.authenticeren_digid_app_choice',
               'uc2.authenticeren_digid_app_to_app_start', 'digid_hoog.authenticate.chose_app', 'digid_hoog.authenticate.chose_desktop_app']

  def self.report_name
    'Aantal authenticatiepogingen per organisatie per sector'
  end

  def self.report (start_date = nil)
      # CLASSNAME : repSucAuthOrgMnt
      # BRON  : week_maandRaportage.csv rij 2
      # RETURN: maand, organizations.name, organizations.Id, logs.sector_name, aantal
      # JOINS : logs.webservice_id = webservices.id AND webservices.organization_id=organizations.id
      # WHERE : 'uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt',
      uc_labels=['uc2.authenticeren_start_basis', 'uc2.authenticeren_start_midden', 'uc2.authenticeren_digid_app_to_app_start']

      # Use last month as period if no date
      # is supplied.
      start_ts, end_ts = get_month_start_end(start_date)
      logger.debug "#{__method__} ===> searching between #{start_ts} and #{end_ts}"

      counts = Log.where(created_at: (start_ts..end_ts),code: lookup_codes(UC_LABELS, nil)).group(:webservice_id, :sector_name).count

      result = list_organizations(start_ts,counts)
      logger.info "aantal_gelukte_auth_per_org_per_sector_monthly ===> Result for aantal_gelukte_auth_per_org_per_sector_monthly: #{result}"

    return result
  end

  def self.list_organizations(ts,qry_result)
    logger.debug "DEBUG #{__method__} ===> ts = #{ts}; qry_result = #{qry_result.inspect};"
    result = [["Periode", "Organization Name", "Organization Id", "Webservice Name", "Sector Name", "Counter"]]
    month = ts.strftime("%Y-%m")

    Organization.all.each do |org|
      org_id = org.id
      org_name = org.name
      webservices_found = 0
      Webservice.where(:organization_id => org_id).each do |w|
        in_resultset = 0
        qry_result.select{|key, val|key.include?(w.id)}.each do |key_arr, cnt|
          webservices_found += 1
          in_resultset += 1
          if key_arr[1].present?
            # in_resultset += 1
            result << [month, org_name, org_id, w.name, key_arr[1], cnt]
            # end
          # if 1 > in_resultset
          else
            result << [month, org_name, org_id, w.name, "", cnt]
          end
        end
        if 1 > in_resultset
          result << [month, org_name, org_id, w.name, "", 0]
        end
      end
      if 1 > webservices_found
        result << [month, org_name, org_id, "", "", 0]
      end
    end
    return result
  end
end
