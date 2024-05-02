
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

class AselectController < ApplicationController
  include LogConcern

  before_action :check_offline
  skip_before_action :verify_authenticity_token
  # GET aselect/handle_request
  def handle_cgi
    handle_with_rescue do
      aselect_hash = params.except(:action, :controller, :host)
      aselect_hash[:a_select_server] = aselect_hash.delete(:'a-select-server') if aselect_hash[:'a-select-server']
      if !aselect_hash.empty?
        @aselect_request         = Aselect::Request.create(aselect_type: "CGI")
        session[:authentication] = {return_to: aselect_session_path}
        ActiveRecord::Base.transaction do
          render handle_request(aselect_hash)
        end
      else
        raise ActionController::RoutingError.new("Not Found")
      end
    end
  end

  # POST aselect/handle_request for soap calls
  # ContentType is checked for soap version
  # Namespace is checked for valid soap message
  def handle_soap
    handle_with_rescue do
      soap_namespace = "#{Aselect.protocol}://#{request.url.gsub(/https?:\/\//, "")}"
      aselect_type     = request.content_type == "application/soap+xml" ? "SOAP-1-2" : "SOAP-1-1"
      @aselect_request = Aselect::Request.create(aselect_type: aselect_type,
                                                 soap_namespace: soap_namespace)
      xml_hash         = Hash.from_xml(request.raw_post)
      aselect_hash     = HashWithIndifferentAccess.new(xml_hash["Envelope"]["Body"]["ASelect"])
      raise Aselect::InvalidRequestError unless aselect_hash.keys.exclude?("xmlns:m") || aselect_hash["xmlns:m"] =~ /#{soap_namespace}/
      ActiveRecord::Base.transaction do
        render handle_request(aselect_hash)
      end
    end
  end

  private

  def offline?
    false
  end

  def check_offline
    if offline?
      handle_with_rescue do
        raise Aselect::TemporaryOfflineError
      end
    end
  end

  def handle_with_rescue
    yield
  rescue Aselect::TemporaryOfflineError
    render_failed_response(Aselect::ResultCodes::TEMPORARY_OFFLINE)
  rescue Aselect::InvalidRequestError
    render_failed_response(Aselect::ResultCodes::REQUEST_INVALID)
  rescue ActionController::RoutingError => e
    Log.instrument("1500")
    raise e
  end

  # GET || POST to handle all request when temporary offline
  def render_failed_response(result_code)
    aselect_type = if request.get?
      "CGI"
    else
      request.content_type == "application/soap+xml" ? "SOAP-1-2" : "SOAP-1-1"
    end

    logcode = {"soap-1-1" => 101, "soap-1-2" => 101, "cgi" => 99, "wsdl" => 103}[aselect_type.downcase]

    ActiveSupport::Notifications.instrument("digid.#{logcode}", code: result_code, hidden: true)

    response_hash = { a_select_server: Aselect.default_server, result_code: result_code }
    render Aselect::Response.for_aselect_type(response_hash, aselect_type, request.url)
  end

  # Handle an aselect_request
  # Check request type and required parameters
  def handle_request(aselect_hash)
    if Aselect::InitiateRequest.hash_contains_required_keys?(aselect_hash)
      initiation(aselect_hash)
    elsif Aselect::VerificationRequest.hash_contains_required_keys?(aselect_hash)
      verification(aselect_hash)
    else
      raise Aselect::InvalidRequestError
    end
  end

  # Initiation
  def initiation(args = {})
    @initiate_request = Aselect::InitiateRequest.new(app_id: args[:app_id],
                                                     app_url: CGI.unescape(args[:app_url]),
                                                     shared_secret: args[:shared_secret],
                                                     a_select_server: args[:a_select_server],
                                                     aselect_request: @aselect_request)
    webservice = @initiate_request.webservice.respond_to?(:webservice) ? @initiate_request.webservice.webservice : @initiate_request.webservice
    @initiate_request.response
  end

  # Verification
  def verification(args = {})
    @verfication_request = Aselect::VerificationRequest.new(rid: args[:rid],
                                                            aselect_credentials: args[:aselect_credentials],
                                                            shared_secret: args[:shared_secret],
                                                            a_select_server: args[:a_select_server],
                                                            aselect_request: @aselect_request)
    aselect = Aselect::Session.find_by_rid(args[:rid])

    if aselect.betrouwbaarheidsniveau
      response_hash = @verfication_request.response_hash
      if [Aselect::ResultCodes::SUCCESS, Aselect::ResultCodes::AUTHENTICATION_CANCELED].include?(response_hash[:result_code])
        log_webservice_authentication_succeed(aselect: aselect)
      else
        log_webservice_authentication_error(aselect: aselect)
      end
    end

    @verfication_request.response(response_hash)
  end
end
