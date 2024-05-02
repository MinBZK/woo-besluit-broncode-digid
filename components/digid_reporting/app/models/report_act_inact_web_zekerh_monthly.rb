
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
# Counts the active and inactive webservices grouped by authentication
# level and sector
#
class ReportActInactWebZekerhMonthly < AdminReport
  ACTIVE = 1
  INACTIVE = 0
  AUTH_LVL_BASIS = 10
  AUTH_LVL_MIDDEN = 20
  METHOD_ASELECT = 'aselect'
  METHOD_SAML = 'saml'
  AUTH_LVL_BASIS_TXT = 'basis'
  AUTH_LVL_MIDDEN_TXT = 'midden'
  AUTH_LVL_ONBEKEND_TXT = 'onbekend'

  def self.report_name
  'Aantal actieve inactieve webdiensten per zekerheidsniveau per sector'
  end

  def self.report (start_date = nil)
    row_headers = ["Periode", "Webdienst ID", "Webdienstnaam", "Organisatienaam",
          "Organisatie ID", "Sector", "Authenticatie methode", "Zekerheidsniveau", "Actief/niet actief",
          "Status Redirect controle", "Redirect URL", "App-ID", "Status SSO", "SSO Domain", "Datum actief van",
          "Datum actief tot", "Omschrijving", "Certificaat Distinguished Name", "Certificaat Subject Alternative Name",
          "Certificaat Geldig van", "Certificaat Geldig tot"]

    # Use last month as period if no date
    # is supplied.
    start_ts, end_ts = get_month_start_end(start_date)
    a_month = format_report_period(ReportParam::DAY, start_ts.end_of_month)
    end_ts = ActiveRecord::Base.sanitize(end_ts)
    start_ts = ActiveRecord::Base.sanitize(start_ts)
    logger.debug "DEBUG #{__method__} ===> searching between #{start_ts} and #{end_ts}"
    w_conn = Webservice.connection
    # W.website_url,
    # SP.entity_id,
    webs_act = w_conn.execute(
        "SELECT W.id as 'Webdienst ID',
        W.name as Webdienstnaam,
        O.name as Organisatienaam,
        O.id as 'Organisatie ID',
        S.name as Sector,
        W.authentication_method as 'Authenticatie methode',
        AW.assurance_level as Zekerheidsniveau,
        CASE WHEN  W.active
          AND ((W.active_from < #{end_ts}
            OR W.active_from IS NULL)
          OR (W.active_until >= #{end_ts}
            OR W.active_until IS NULL)) THEN #{ACTIVE}
        ELSE #{INACTIVE} END AS 'Actief/niet actief',
        W.check_redirect_url AS 'Status Redirect controle',
        W.redirect_url_domain AS 'Redirect URL',
        AW.app_id AS 'App-ID',
        SP.allow_sso AS 'Status SSO',
        SSD.name AS 'SSO Domain',
        W.active_from AS 'Datum actief van',
        W.active_until AS 'Datum actief tot',
        W.description AS 'Omschrijving',
        C.distinguished_name AS 'Certificaat Distinguished Name',
        C.cached_certificate
        FROM webservices AS W
          LEFT JOIN (sector_authentications AS SA
          LEFT JOIN sectors AS S ON S.id = SA.sector_id) ON W.id = SA.webservice_id
          LEFT JOIN aselect_webservices AS AW ON AW.webservice_id = W.id
          LEFT JOIN organizations AS O ON O.id = W.organization_id
          LEFT JOIN saml_providers AS SP ON  SP.webservice_id = W.id
          LEFT JOIN saml_sso_domains AS SSD on SSD.id = SP.sso_domain_id
          LEFT JOIN certificates AS C on C.webservice_id = W.id
        ORDER BY W.name, O.name;"
     )

    values = []
    result = [row_headers]

    webs_act.each do |values|
      row = [a_month]
      values.each do |field|
        row << field
      end
      logger.debug "#{__method__} QQQ ==> row_act are: #{row}"
      row = extract_certificate(row)
      result << row
    end

    logger.debug "#{__method__} DEBUG ==> result is: #{result}"
    return result
  end

  # -------------------------------------------------------------------------
  private
  # -------------------------------------------------------------------------
  # Extract certicate details from cached certificate
  #   ~/Projects/digid_admin(master) $ rails c
  # [12] pry(main)> c = Certificate.first
  #   Certificate Load (0.4ms)  SELECT  `certificates`.* FROM `certificates`   ORDER BY `certificates`.`id` ASC LIMIT 1
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  # [14] pry(main)> OpenSSL::X509::Certificate.new(c.cached_certificate).not_before
  # => 2010-03-29 14:52:05 UTC
  # [15] pry(main)> OpenSSL::X509::Certificate.new(c.cached_certificate).not_after
  # => 2011-03-29 14:52:05 UTC
  def self.extract_certificate(row)
    logger.debug "DEBUG #{__method__} ===> row: #{row}"
    last_index = row.count - 1
    logger.debug "DEBUG #{__method__} ===> index: #{last_index}"
    if row[last_index].present?
      cached_certificate = row[last_index]
      logger.debug "DEBUG #{__method__} ===> certificate #{cached_certificate}"
      subject = OpenSSL::X509::Certificate.new(cached_certificate).subject.to_s
      not_before = OpenSSL::X509::Certificate.new(cached_certificate).not_before
      not_after = OpenSSL::X509::Certificate.new(cached_certificate).not_after
      # Modify and return row
      row[last_index] = subject
      row << not_before
      row << not_after
    else
      row << ''
      row << ''
    end
    return row
  end

end
