
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

class Aselect::VerificationRequest < Aselect::Base
  attributes :rid, :aselect_credentials, :shared_secret, :a_select_server, :aselect_request, :distinguished_name, :fingerprint

  validates :rid,
            :aselect_credentials,
            :shared_secret,
            :a_select_server,
            presence: true

  def self.hash_contains_required_keys?(hash)
    (["request", "rid", "aselect_credentials", "shared_secret", "a_select_server"] - hash.keys).empty? && hash[:request] == "verify_credentials"
  end

  def session
    @session ||= Aselect::Session.checked_credentials(aselect_credentials)
  end

  #Determine the result code
  def determine_result_code
    check_webservice ||
        check_a_select_server ||
        check_credentials ||
        (session && session.result_code) ||
        Aselect::ResultCodes::SUCCESS
  end

  # Check the webservice
  def check_webservice
    if webservice.blank?
      Aselect::ResultCodes::WEBSERVICE_UNAUTHORIZED
    elsif webservice.inactive?
      Aselect::ResultCodes::WEBSERVICE_INACTIVE
    end
  end

  # Check credentials
  def check_credentials
    if session == nil
      Aselect::ResultCodes::AUTH_VERIFICATION_FAILED
    elsif session.rid != rid || webservice.id != session.webservice_id
      Aselect::ResultCodes::AUTH_VERIFICATION_INVALID
    end
  end

  def response_hash
    webservice_id = webservice.respond_to?(:webservice_id) ? webservice.webservice_id : nil
    result_code   = determine_result_code
    hash          = ActiveSupport::OrderedHash.new
    if result_code == Aselect::ResultCodes::SUCCESS
      hash[:uid                   ] = uid
      hash[:betrouwbaarheidsniveau] = session.betrouwbaarheidsniveau
      hash[:rid                   ] = session.rid
      hash[:organization          ] = Aselect.organization
      hash[:app_id                ] = session.webservice.app_id
      hash[:result_code           ] = Aselect::ResultCodes::SUCCESS
      hash[:a_select_server       ] = Aselect.default_server
      ActiveSupport::Notifications.instrument("digid.#{log_mapping_success[aselect_request.aselect_type.downcase.to_sym]}",
                                              webservice_id: webservice_id, hidden: true)
    else
      ActiveSupport::Notifications.instrument("digid.#{log_mapping_failure[aselect_request.aselect_type.downcase.to_sym]}",
                                              code: result_code,
                                              webservice_id: webservice_id,
                                              hidden: true
                                              )
      hash[:a_select_server] = Aselect.default_server
      hash[:result_code]     = result_code
    end
    session.destroy if session
    hash
  end

  def response(response_hash_value = nil)
    response_hash_value = response_hash if response_hash_value.nil?
    Aselect::Response.for_aselect_type(response_hash_value, aselect_request.aselect_type, aselect_request.soap_namespace)
  end

  private

  def uid
    if aselect_request.aselect_type == "WSDL"
      {Sectorcode: session.sector_code,
       SectoraalNummer: session.sectoraal_nummer}
    else
      session.sectoraal_nummer
    end
  end

  def log_mapping_success
    { cgi: "1516",
      "soap-1-1": "1518",
      "soap-1-2": "1520",
      wsdl: "1522" }
  end

  def log_mapping_failure
    { cgi: "1517",
      "soap-1-1": "1519",
      "soap-1-2": "1521",
      wsdl: "1523" }
  end
end
