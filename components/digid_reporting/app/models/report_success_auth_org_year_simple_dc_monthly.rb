
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

class ReportSuccessAuthOrgYearSimpleDcMonthly < AdminReport

  UC_LABELS = [
    "uc2.authenticeren_basis_gelukt", # 68
    "uc2.authenticeren_midden_gelukt", # 74
    "uc2.authenticeren_digid_app_gelukt", # 714
    "uc2.authenticeren_digid_app_to_app_gelukt", # 734
    "uc2.authenticeren_substantieel_gelukt", # 861
    "uc2.authenticeren_digid_app_to_app_substantieel_gelukt", # 1123
    "uc2.authenticeren_hoog_gelukt", # 691
    "uc2.authenticeren_digid_app_to_app_hoog_gelukt" # 1168
  ]

  def self.report_name
    "Aantal gelukte authenticaties per organisatie per jaar tbv doorbelasting - Dienstencatalogus overzicht per organisatie"
  end

  def self.report (start_date = nil)
    start = (start_date ? Time.parse(start_date.to_s) : Time.now.last_month )
    start_month = start.prev_year.next_month.beginning_of_month
    end_month = start.end_of_month

    logger.debug "#{__method__} ===> searching between #{start_month} and #{end_month}"
    log_scope = Log.from("#{Log.quoted_table_name} FORCE INDEX(index_logs_on_created_at)").where(created_at: start_month..end_month, code: lookup_codes(UC_LABELS, nil))

    count_data = log_scope.group(:webservice_id).count(:webservice_id)
    result = [["Vanaf maand", "T/m maand", "Organisatie", "Organisatie ID", "Actief/inactief", "Totaal aantal authenticaties Organisatie"]]

    start_month = start_month.strftime("%Y-%m")
    end_month = end_month.strftime("%Y-%m")

    count_per_connection_per_service = {}
    count_per_organization_role = {}

    Dc::Service.each_in_batches do |service|
      count_per_connection_per_service[service.connection_id] ||= 0
      count_per_connection_per_service[service.connection_id] += count_data[service.legacy_service_id] || 0
    end

    Dc::Connection.each_in_batches do |connection|
      count_per_organization_role[connection.organization_role_id] ||= {}
      count_per_organization_role[connection.organization_role_id][connection.id] = count_per_connection_per_service[connection.id]
    end

    Dc::Organization.each_in_batches do |organization|
      result << [
        start_month,
        end_month,
        organization.name,
        organization.id,
        organization.status.active ? "Actief" : "Inactief",
        count_per_organization_role.slice(*organization.organization_roles.map(&:id)).values.map(&:values).flatten.sum
      ]
    end

    logger.info "aantal_gelukte_auth_per_org_per_jaar_doorbelasting_dc ===> Result for aantal_gelukte_auth_per_org_per_jaar_doorbelasting_dc: #{result}"
    result
  end
end
