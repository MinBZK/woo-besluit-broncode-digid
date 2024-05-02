
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

# frozen_string_literal: true

module WidErrors
  extend ActiveSupport::Concern

  def wid_account_log?(status_description)
    status_description == "app.cancel"
  end

  def wid_log_for(status_description)
    key = status_index(status_description)
    log_keys[key]
  end

  def wid_flash_for(status_description)
    key = status_index(status_description)
    t(*status_messages[key]).html_safe
  end

  def document_type
    session[:document_type]
  end

  def status_index(key)
    status_description_index[key].present? ? status_description_index[key] : status_description_index["default"]
  end

  def log_keys
    {
      bsn_error: "1488",
      bvbsn_error: "1491",
      bvbsn_timeout_error: "1492",
      crb_error: "863",
      crb_internal_error: "882",
      crb_timeout: "855",
      crb_no_driving_license: "847",
      app_cancel: "879",
      app_timeout: "869",
      rda_cancel: "876",
      rda_authn_error: "873",
      rda_timeout: "877",
      rda_chip_error: "870",
      rda_active_authentication_failed: "874",
      rda_generic_error: "875",
      rda_non_match: "878",
      rda_ip_check_error: "1358",
      digid_rda_generic_error: "1200"
    }
  end

  def status_messages
    {
      crb_error: "digid_app.substantial.cis_abort.crb.error",
      crb_internal_error: "digid_app.substantial.cis_abort.crb.error",
      crb_timeout: "digid_app.substantial.cis_abort.crb.timeout",
      crb_no_driving_license: "digid_app.substantial.cis_abort.crb.no_driving_license",
      app_cancel: "digid_app.substantial.cancelled",
      app_timeout: "digid_app.substantial.app_timeout",
      rda_cancel: "digid_app.substantial.cancelled",
      rda_authn_error: "digid_app.substantial.cis_abort.rda.authn_error",
      rda_timeout: "digid_app.substantial.cis_abort.rda.timeout",
      rda_chip_error: ["digid_app.substantial.cis_abort.rda.chip_error", document_type: I18n.t(document_type, scope: "document_options")],
      rda_active_authentication_failed: "digid_app.substantial.cis_abort.rda.generic_error",
      rda_generic_error: "digid_app.substantial.cis_abort.rda.generic_error",
      rda_non_match: "digid_app.substantial.cis_abort.rda.generic_error"
    }
  end

  def status_description_index
    {
      "app.cancel" => :app_cancel,
      "app.timeout" => :app_timeout,
      "bsn.error" => :bsn_error,
      "bvbsn.error" => :bvbsn_error,
      "bvbsn.timeout_error" => :bvbsn_timeout_error,
      "crb.error" => :crb_error,
      "crb.internal_error" => :crb_internal_error,
      "crb.timeout" => :crb_timeout,
      "crb.no_driving_license" => :crb_no_driving_license,
      "default" => :rda_authn_error,
      "CANCELLED" => :digid_rda_generic_error,
      "ABORTED" => :digid_rda_generic_error,
      "COM" => :digid_rda_generic_error,
      "CHALLENGE" => :digid_rda_generic_error,
      "BAC" => :digid_rda_generic_error,
      "READ_FILE" => :digid_rda_generic_error,
      "PARSE_FILE" => :digid_rda_generic_error,
      "PASSIVE_AUTHENTICATION" => :digid_rda_generic_error,
      "BAP" => :digid_rda_generic_error,
      "NON_MATCH" => :digid_rda_generic_error,
      "AUTHENTICATE" => :digid_rda_generic_error,
      "SECURE_MESSAGING" => :digid_rda_generic_error,
      "MRZ_CHECK" => :digid_rda_generic_error,
      "TIMEOUT" => :digid_rda_generic_error,
      "ACTIVE_AUTHENTICATION" => :digid_rda_generic_error,
      "IP_CHECK" => :rda_ip_check_error
    }
  end
end
