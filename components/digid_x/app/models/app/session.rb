
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
module App
  class Session < ActiveResource::Base
    self.site = "#{APP_CONFIG["urls"]["internal"]["app"]}/iapi/"

    schema do
      string "id","created_at","action","url","confirm_secret","rda_session_id","rda_session_timeout_in_seconds",
            "rda_session_status","flow","error","wid_request_id","user_app_id","activation_method","webservice",
            "return_url","authentication_level","document_type","device_name","instance_id","state","challenge",
            "registration_id","account_id","iv","spoken","with_bsn","language","nfc_support","multiple_devices",
            "challenge_received","attempts","webservice_id","first_attempt","eid_session_id",
            "eid_session_timeout_in_seconds","ad_session_id","sequence_no","card_status","polymorph_identity",
            "polymorph_pseudonym","abort_code","remove_old_app","ockto_browser","app_authentication_level",
            "app_to_destroy","reason","vpuk_status","app_ip", "relay_state", "app_id", "saml_session_key", "saml_provider_id", "eidas_uit",
            "new_authentication_level", "new_level_start_date", "account_id_flow", "ttl"
    end

    self.headers['X-Auth-Token'] = Rails.application.secrets[:iapi_token]
    self.include_format_in_path = false

    def cancel!
      self.state = "CANCELLED"
      save
    end

    def abort!
      self.state = "ABORTED"
      save
    end

    def complete!
      self.state = "COMPLETED"
      save
    end

    def error!(error)
      self.error = error
      save
    end
  end
end
