
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

class Aselect::InitiateRequest < Aselect::Base
  attributes :app_url, :app_id, :shared_secret, :a_select_server, :aselect_request, :distinguished_name, :fingerprint

  validates :app_url,
            :app_id,
            :a_select_server,
            :shared_secret,
            presence: true

  def self.hash_contains_required_keys?(hash)
    (["request", "app_id", "app_url", "shared_secret", "a_select_server"] - hash.keys).empty? && hash[:request] == "authenticate"
  end

  #Determine the result code
  def determine_result_code
    check_webservice ||
        check_a_select_server ||
        check_app_url ||
        Aselect::ResultCodes::SUCCESS
  end

  # Check the webservice
  def check_webservice
    if webservice.blank? || webservice.app_id != app_id
      Aselect::ResultCodes::WEBSERVICE_UNAUTHORIZED
    elsif webservice.inactive?
      Aselect::ResultCodes::WEBSERVICE_INACTIVE
    end
  end

  # Check the application url
  def check_app_url
    unless webservice.app_url_valid?(app_url)
      Aselect::ResultCodes::INVALID_APP_URL
    end
  end

  def response
    Aselect::Response.for_aselect_type(response_hash, aselect_request.aselect_type, aselect_request.soap_namespace)
  end

  def response_hash
    result_code   = determine_result_code
    webservice_id = @aselect_session && @aselect_session.webservice.respond_to?(:webservice) ? @aselect_session.webservice.webservice.try(:id) : nil
    hash          = ActiveSupport::OrderedHash.new
    if result_code == Aselect::ResultCodes::SUCCESS
      # FIXME: This is where Aselect::Sessions can be made without a webservice association.
      # We should probably return an invalid request status code if the Webservice can't be found!
      @aselect_session = aselect_request.build_session(app_url: app_url, app_id: app_id, webservice: webservice)
      aselect_request.save!
      hash[:a_select_server] = Aselect.default_server
      hash[:rid]             = @aselect_session.rid
      hash[:as_url]          = Aselect.default_as_url
      hash[:result_code]     = Aselect::ResultCodes::SUCCESS

      logcode = {"soap-1-1" => 100, "soap-1-2" => 100, "cgi" => 98, "wsdl" => 102}[aselect_request.aselect_type.downcase]
      ActiveSupport::Notifications.instrument("digid.#{logcode}",
                                              webservice_id: webservice.try(:webservice_id), hidden: true)
    else
      logcode = {"soap-1-1" => 101, "soap-1-2" => 101, "cgi" => 99, "wsdl" => 103}[aselect_request.aselect_type.downcase]
      ActiveSupport::Notifications.instrument("digid.#{logcode}",
                                              code: result_code,
                                              webservice_id: webservice.try(:webservice_id), hidden: true)
      hash[:a_select_server] = Aselect.default_server
      hash[:result_code]     = result_code
    end
    hash
  end
end
