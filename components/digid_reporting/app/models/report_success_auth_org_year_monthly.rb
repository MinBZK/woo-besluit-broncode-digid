
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

class ReportSuccessAuthOrgYearMonthly < AdminReport

  UC_LABELS = ['uc2.authenticeren_basis_gelukt', 'uc2.authenticeren_midden_gelukt','uc2.authenticeren_digid_app_gelukt', 'uc2.authenticeren_digid_app_to_app_gelukt',
               'uc2.authenticeren_substantieel_gelukt', 'uc2.authenticeren_digid_app_to_app_substantieel_gelukt', 'uc2.authenticeren_hoog_gelukt',
               'uc2.authenticeren_digid_app_to_app_hoog_gelukt' ]

  def self.report_name
    'Aantal gelukte authenticaties per organisatie per jaar tbv doorbelasting'
  end

  def self.report (start_date = nil)
    start = (start_date ? Time.parse(start_date.to_s) : Time.now.last_month )
    start_month = start.prev_year.next_month.beginning_of_month
    end_month = start.end_of_month

    logger.debug "#{__method__} ===> searching between #{start_month} and #{end_month}"
    log_scope = Log.from("#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)").where(created_at: start_month..end_month, code: lookup_codes(UC_LABELS, nil))

    count_data = log_scope.group(:webservice_id).count(:webservice_id)
    result = [["Vanaf maand", "T/m maand", "Organisatie", "OrganisatieID", "Totaal aantal authenticaties", "Actief/inactief"]]

    start_month = start_month.strftime("%Y-%m")
    end_month = end_month.strftime("%Y-%m")

    Webservice.group(:organization_id).select("organization_id, group_concat(id) as ids").map {|o| [o.organization_id, o.ids]}.each do |(organization_id, webservices)|
      organization = Organization.find(organization_id)

      result << [
        start_month,
        end_month,
        organization.name,
        organization_id,
        webservices.split(",").map{|i| count_data[i.to_i].to_i}.sum,
        organization.webservices.active.exists? ? "Actief" : "Inactief"
      ]
    end

    logger.info "aantal_gelukte_auth_per_org_per_jaar_doorbelasting ===> Result for aantal_gelukte_auth_per_org_per_jaar_doorbelasting: #{result}"
    result
  end
end
