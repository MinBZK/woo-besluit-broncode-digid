
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

class ReportActAttemptsHoogWeekly < AdminReport
  def self.report_name
    'Aantal activatiepogingen DigiD Hoog per week'
  end

  def self.report(start_date = nil)
    @uc_labels = ['digid_hoog.activate.started', 'digid_hoog.activate.app_chosen', 'digid_hoog.activate.usb_chosen', 'digid_hoog.activate.success',
                  'digid_hoog.activate.abort.no_bsn', 'digid_hoog.activate.interrupted_no_email', 'digid_hoog.activate.abort.desktop_client_timeout',
                  'digid_hoog.activate.abort.desktop_clients_ip_address_not_equal', 'digid_hoog.activate.abort.no_app_session',
                  'digid_hoog.activate.abort.no_nfc', 'digid_hoog.activate.abort.nfc_forbidden', 'digid_hoog.activate.abort.eid_timeout',
                  'digid_hoog.activate.abort.wrong_doctype_sequence_no', 'digid_hoog.activate.abort.false_bsn', 'digid_hoog.activate.abort.msc_not_issued',
                  'digid_hoog.activate.abort.mu_error']

    # Use last month as period if no date is supplied.
    start_ts, end_ts = get_week_start_end(start_date)
    logger.info "aantal_activatiepogingen_digid_hoog_weekly ===> Generating report for #{start_ts} .. #{end_ts}"

    year_and_week = start_ts.strftime("%Y#%V")

    result = [["Week", "Documenttype", "Aantal gestarte activaties", "Aantal activaties gestart via DigiD app",
              "Aantal activaties gestart via Desktop app", "Aantal geslaagde activaties", "Aantal gefaalde activaties",
              "Aantal gefaald: geen BSN", "Aantal gefaald: geen e-mailadres", "Aantal gefaald: Desktop app niet geopend",
              "Aantal gefaald: ip-controle onjuist", "Aantal gefaald: ongeldig sessie-id", "Aantal gefaald: geen NFC",
              "Aantal gefaald: geen bruikbare NFC", "Aantal gefaald: eID server time-out", "Aantal gefaald: onjuist doctype/volgnr",
              "Aantal gefaald: BSN onjuist", "Aantal gefaald: onjuiste ID-status", "Aantal gefaald: foutbericht MU",
              "Aantal gefaald: overig"]]

    document_type = ["Identiteitskaart", "Rijbewijs", "Totaal"]
    document_type.each do |type|
      case type
      when "Identiteitskaart"
        # identiteitskaart
        @logs = Log.where("name like ?", "%identiteitskaart%").where(
                          created_at: (start_ts..end_ts),
                          code: lookup_codes(@uc_labels, nil)
                        ).group(:code).count

        result << [year_and_week, type, set_variables(@logs)].flatten
      when "Rijbewijs"
        @logs = Log.where("name like ?", "%rijbewijs%").where(
          created_at: (start_ts..end_ts),
          code: lookup_codes(@uc_labels, nil)
        ).group(:code).count

        result << [year_and_week, type, set_variables(@logs)].flatten
      when "Totaal"
        # totaal = identiteitskaart + rijbewijs
        @logs = Log.where(
                      created_at: (start_ts..end_ts),
                      code: lookup_codes(@uc_labels, nil)
                    ).group(:code).count

        result << [year_and_week, type, set_variables(@logs)].flatten
      end
    end
    result
  end

  private

  def self.set_variables(logs)
    started_activations = get_counted_log(logs, 145000)
    succesfull_activations = get_counted_log(logs, 145009)
    activations_started_via_app = get_counted_log(logs, 145001)
    activations_started_via_desktop_app = get_counted_log(logs, 145017)
    failed_activations = started_activations - succesfull_activations
    failed_no_bsn = get_counted_log(logs, 145002)
    failed_no_email = get_counted_log(logs, 145004)
    failed_desktop_app_not_opened = get_counted_log(logs, 145018)
    failed_not_same_device = get_counted_log(logs, 145021)
    failed_no_session = get_counted_log(logs, 145008)
    failed_no_nfc = get_counted_log(logs, 145006)
    failed_nfc_not_allowed = get_counted_log(logs, 145007)
    failed_eid_timeout = get_counted_log(logs, 145010)
    failed_wrong_doctype_or_seq_nr = get_counted_log(logs, 145013)
    failed_wrong_bsn = get_counted_log(logs, 145012)
    failed_wrong_status = get_counted_log(logs, 145014)
    failed_mu_error = get_counted_log(logs, 145011)
    other_failures = failed_activations - failed_no_bsn - failed_no_email - failed_desktop_app_not_opened - failed_not_same_device - failed_no_session - failed_no_nfc - failed_nfc_not_allowed - failed_eid_timeout - failed_wrong_doctype_or_seq_nr - failed_wrong_bsn - failed_wrong_status - failed_mu_error

    entries = [started_activations, activations_started_via_app,
              activations_started_via_desktop_app, succesfull_activations, failed_activations,
              failed_no_bsn, failed_no_email, failed_desktop_app_not_opened,
              failed_not_same_device, failed_no_session, failed_no_nfc,
              failed_nfc_not_allowed, failed_eid_timeout, failed_wrong_doctype_or_seq_nr,
              failed_wrong_bsn, failed_wrong_status, failed_mu_error,
              other_failures]
  end

  def self.get_counted_log(hash, logcode)
    hash[logcode] || 0
  end
end
