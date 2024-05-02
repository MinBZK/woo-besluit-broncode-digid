
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

# frozen_string_literal: false

# webregistration exposes soap entry points for webdienst services
# soap xmls directed to /svb/aanvragen/nieuw will be consumed
# (currently) there are 3 endpoints in the wsdl:
# -1- AanvragenSectorAccount
# -2- AanvragenSectorAccountOnderwater
# -3- RevocerenAccount
# The first is processed/validated in model WebRegistration
# The last two endpoints (2,3) in model AccountAdmin

class WebRegistrationsController < ApplicationController
  skip_before_action :verify_authenticity_token, only: [:svb]

  before_action :dispatcher,            only: [:svb]
  before_action :aanvraag_authorized?,  only: [:svb]
  before_action :aanvraag_present?,     only: [:svb]
  before_action :aanvraag_valid?,       only: [:svb]

  # Render the wsdl file and XSD
  def schema
    respond_to do |format|
      format.html { render template: "web_registrations/wsdl/WSDigiDSectoradministratie_200806", formats: [:wsdl], content_type: :xml, layout: false }
      format.xsd  { render template: "web_registrations/wsdl/WSDigiDSectoradministratie_200806", formats: [:xsd],  content_type: :xml, layout: false }
      format.wsdl { render template: "web_registrations/wsdl/WSDigiDSectoradministratie_200806", formats: [:wsdl], content_type: :xml, layout: false }
    end
  end

  # entry point for svb webdienst (aanvraag)
  # Sectorcode: 8 cijfers. [00000000 BSN, 00000001 SOFI, 00000002 Anummer, 00000100 OEB]
  def svb
    begin
      webreg = WebRegistration.soap_to_registration(@soap_request, @aanvraag)
      if webreg.save
        # create a normal registration so we can follow through with the process normally
        registration = Registration.create_fake_aanvraag(@aanvraag[:sectoraal_nummer])

        if Sector.get(@aanvraag[:sectorcode]).eql?(Sector.get("sofi"))
          if registration.retrieve_brp!("webdienst", webreg.id)
            state, registration = wait_for_gba_status(registration)
            content = gba_request_handler(state, registration)
            Log.instrument("274")
          else
            # overbelast
            Log.instrument("275", reason: I18n.t("transactiecode.tc-0002", locale: :nl).downcase)
            content = ActiveSupport::OrderedHash.new
            content[:Transactiegegevens] = create_content "0002", I18n.t("transactiecode.tc-0002")
          end
        else
          Log.instrument("274")
          content = ActiveSupport::OrderedHash.new
          create_letter_content(content, registration)
        end
      else
        log_reason = webreg.errors.messages.values.flatten.uniq.map { |r| ActionController::Base.helpers.strip_tags(r) }.join(", ")
        Log.instrument("275", reason: log_reason)
      end
    rescue
      content = "" # sends a syntax error
    end
    render xml: to_soap("AanvragenSectorAccountResponse", content), content_type: "text/xml"
  end

  private

  # check the type of request and dispatch accordingly
  def dispatcher
    fingerprint && distinguished_name

    if soap_request[:Envelope][:Body][:AanvragenSectorAccountOnderwaterRequest].present?
      handle_onderwater
    elsif soap_request[:Envelope][:Body][:RevocerenAccountRequest].present?
      handle_revoceren
    end
    # else proceed with aanvraag_authorized?
  end

  # AanvragenSectorAccountOnderwaterRequest:
  # log start en result
  # check if authorization is ok
  # validate account details -> create test account if ok
  # render soap response
  def handle_onderwater
    @soap_request = soap_request[:Envelope][:Body][:AanvragenSectorAccountOnderwaterRequest]
    content = ActiveSupport::OrderedHash.new
    content[:BerichtVersie] = bericht_versie
    if authorized?(:test)
      msg_code = AccountAdmin.onderwater(@soap_request)
      content[:Transactiegegevens] = create_content msg_code, I18n.t("transactiecode.tc-#{msg_code}")
    else
      # Geen autorisatie voor deze operatie
      Log.instrument("273", reason: I18n.t("transactiecode.tc-0004", locale: :nl), webservice_id: @soap_request[:webservice_id])
      content[:Transactiegegevens] = create_content "0004", I18n.t("transactiecode.tc-0004")
    end
    render xml: to_soap("AanvragenSectorAccountOnderwaterResponse", content), content_type: "text/xml"
  end

  # RevocerenAccountRequest:
  # log start en result
  # check if authorization is ok
  # validate account details -> create test account if ok
  # render soap response
  def handle_revoceren
    @soap_request = soap_request[:Envelope][:Body][:RevocerenAccountRequest]
    content = ActiveSupport::OrderedHash.new
    content[:BerichtVersie] = bericht_versie
    if authorized?(:revocation)
      msg_code = AccountAdmin.revoceren(@soap_request)
      content[:Transactiegegevens] = create_content msg_code, I18n.t("transactiecode.tc-#{msg_code}")
      if msg_code != "0000"
        Log.instrument("278", webservice_id: @soap_request[:webservice_id])
      end
    else
      # Geen autorisatie voor deze operatie
      Log.instrument("278", webservice_id: @soap_request[:webservice_id])
      content[:Transactiegegevens] = create_content "0004", I18n.t("transactiecode.tc-0004")
    end
    render xml: to_soap("RevocerenAccountResponse", content), content_type: "text/xml"
  end

  # generic method to check the certificate and app_id for all endpoints of the webservice
  def authorized?(operation)
    check_webdienst_authorization = ::Configuration.get_int("check_webdienst_authorization")
    if check_webdienst_authorization == 1
      authorize_with_certificate(operation)
    else
      authorize_without_cert # no certificate check, but find out which webservice requested
    end
  end

  # let's see of the webservice is authorized for the requested service for the requested sector
  def authorize_with_certificate(operation)
    certificate = if APP_CONFIG["authentication_method"] == "fingerprint"
      return false if fingerprint.blank?
      Certificate.find_by(fingerprint: fingerprint)
    else
      return false if distinguished_name.blank?
      Certificate.find_by(distinguished_name: distinguished_name)
    end

    return false unless certificate
    webservice = certificate.webservice
    @soap_request[:webservice_id] = webservice.id
    # return false if webservice_name != @soap_request[:AppId] unless operation.eql?(:registration) # not all xml's have app_id
    return false unless webservice.active?
    webservice.authorized_for?(sector: @soap_request[:Sectorgegevens][:Sectorcode], operation: operation)
  end

  # there is no way to fetch a webservice, so we use the url for svb tests, and the AppId for onderwater and revoke
  def authorize_without_cert
    @soap_request[:webservice_id] = if @soap_request[:Aanvraagvoorkeuren]
                                      Webservice.find_by(website_url: @soap_request[:Aanvraagvoorkeuren][:URLBI]).id
                                    else
                                      Webservice.find_by(name: @soap_request[:AppId]).id
                                    end
    true
  end

  # not used until further info about app_id in the xml
  # before_action for method svb
  # check if the requester is authorized for this endpoint
  def aanvraag_authorized?
    @soap_request = soap_request[:Envelope][:Body][:AanvragenSectorAccountRequest]
    return if authorized?(:registration)

    # Geen autorisatie voor deze operatie
    content = ActiveSupport::OrderedHash.new
    content[:Transactiegegevens] = create_content "0004", I18n.t("transactiecode.tc-0004")
    Log.instrument("275", reason: I18n.t("transactiecode.tc-0004", locale: :nl).downcase)
    render xml: to_soap("AanvragenSectorAccountResponse", content), content_type: "text/xml"
  end

  # before_action for method svb
  # checks if there is any data
  def aanvraag_present?
    return if @soap_request.present?

    render xml: to_soap("AanvragenSectorAccountResponse", ""), content_type: "text/xml"
  end

  # before_action for method svb
  # checks if data is valid
  # Bij ontbreken Activeringstermijn wordt deze bepaald door de Sectorcode
  def aanvraag_valid?
    @aanvraag = {}
    # if any of these is not available we throw an error and catch it with rescue -> syntax error
    @aanvraag[:sectorcode] = @soap_request[:Sectorgegevens][:Sectorcode]
    @aanvraag[:sectoraal_nummer] = @soap_request[:Sectorgegevens][:SectoraalNummer]
    @aanvraag[:urlbi] = @soap_request[:Aanvraagvoorkeuren][:URLBI]
    @aanvraag[:activeringstermijn] = @soap_request[:Aanvraagvoorkeuren][:Activeringstermijn]
    @aanvraag[:adres_gegevens] = @soap_request[:Adresgegevens]
    @aanvraag[:naam_gegevens] = @soap_request[:Naamgegevens]
    @aanvraag[:rid] = SecureRandom.hex(10)

    if @aanvraag[:activeringstermijn].blank?
      sector = Sector.find_by(number_name: @aanvraag[:sectorcode])
      @aanvraag[:activeringstermijn] = sector ? sector.expiration_time : raise
    end

  rescue
    Log.instrument("275", reason: I18n.t("transactiecode.tc-0036", locale: :nl).downcase)
    render xml: to_soap("AanvragenSectorAccountResponse", ""), content_type: "text/xml"
  end

  # let's take a look at the positive cases
  def gba_request_handler(state, registration)
    content = ActiveSupport::OrderedHash.new
    if aanvraag_allowed?(registration)
      create_letter_content(content, registration)
    elsif state || (registration.gba_status.eql? "error")
      content[:Transactiegegevens] = create_content "0003", I18n.t("transactiecode.tc-0003")
    elsif registration.gba_status.eql? "deceased"
      content[:Transactiegegevens] = create_content "0022", I18n.t("transactiecode.tc-0022")
    elsif registration.gba_status.eql? "valid"
      content[:Transactiegegevens] = create_content "0024", I18n.t("transactiecode.tc-0024")
    end
    content
  end

  def create_letter_content(content, registration)
    content[:Aanvraagnummer] = @aanvraag[:rid]
    content[:Transactiegegevens] = create_content "0000", I18n.t("transactiecode.tc-0000")
    letter_content = {}
    letter_content["type_bericht"] = "webdienst"
    letter_content["adres_gegevens"] = @aanvraag[:adres_gegevens]
    letter_content["naam_gegevens"] = @aanvraag[:naam_gegevens]
    letter_content["sectorcode"] = @aanvraag[:sectorcode]
    registration.create_letter(letter_content, @aanvraag[:activeringstermijn])
  end

  # als Sectorcode == SOFI
  #  gebruiker mag niet in GBA voorkomen, tenzij status == 'Emigratie' (dan verander SOFI naar BSN)
  def aanvraag_allowed?(registration)
    if Sector.get(@aanvraag[:sectorcode]).eql?(Sector.get("sofi"))
      if registration.status_not_found_or_emigrated_or_rni_or_ministerial_decree?
        if registration.status_emigrated_or_rni_or_ministerial_decree? && (web_reg = WebRegistration.find_by(aanvraagnummer: @aanvraag[:rid]))
          # we need to change sector
          web_reg.sector_id = Sector.get("bsn")
          web_reg.save!
        end
        true # the aanvraag is allowed
      else
        false
      end
    else
      true # not sofi, so it must be something else, allow
    end
  end

  # creates a ordered (because of sequence in xsd) hash which will be xmlized and returned as a soap response
  def create_content(code, description)
    ordered_hash                          = ActiveSupport::OrderedHash.new
    ordered_hash[:Transactiecode]         = code
    ordered_hash[:Transactiebeschrijving] = description
    ordered_hash
  end

  # creates an ordered (because of sequence in xsd) hash which will be xmlized and returned as a soap response
  def bericht_versie
    ordered_hash                      = ActiveSupport::OrderedHash.new
    ordered_hash[:BerichtMajorversie] = 1
    ordered_hash[:BerichtMinorversie] = 0
    ordered_hash
  end

  # wraps a soap envelope around the content provided
  # if no content is provided a 0005 Syntax Error is build
  def to_soap(soap_response, content)
    if content.blank?
      content = ActiveSupport::OrderedHash.new
      content[:Transactiegegevens] = create_content "0005", I18n.t("transactiecode.tc-0005")
    end
    xml_markup = Builder::XmlMarkup.new
    xml_markup.instruct! :xml, version: "1.0", encoding: "UTF-8"
    xml_markup.tag!(:"soapenv:Envelope",
                    'xmlns:soapenv': "http://schemas.xmlsoap.org/soap/envelope/",
                    'xmlns:xsd': "http://www.w3.org/2001/XMLSchema",
                    'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance") do |xml|
      xml.soapenv :Body do |body|
        body.tag!(soap_response, "xmlns" => "http://digid.nl/WSDigiDSectoradministratie/200806") do |xmlns|
          to_xml(xmlns, content) # content.to_xml(:skip_instruct => true)
        end
      end
    end
  end

  def to_xml(xml, content)
    content.each do |key, value|
      if value.is_a?(Hash)
        xml.tag!(key, "xmlns" => "") do |xmlns|
          to_xml_no_xmlns(xmlns, value)
        end
      else
        xml.tag!(key, value, "xmlns" => "")
      end
    end
  end

  def to_xml_no_xmlns(xml, content)
    content.each do |key, value|
      if value.is_a?(Hash)
        xml.tag!(key) do |xmlns|
          to_xml_no_xmlns(xmlns, value)
        end
      else
        xml.tag!(key, value)
      end
    end
  end

  # check the database to see if delayed job is finished
  def wait_for_gba_status(reg)
    max_time = Time.zone.now + ::Configuration.get_int("gba_timeout")

    state = true
    while state && Time.zone.now < max_time
      ActiveRecord::Base.connection.uncached do
        reg = Registration.find(reg.id)
      end
      if reg.gba_status != "request"
        state = false
      else
        sleep 1
      end
    end
    # delayed job finished
    logger.info "delayed job finished"
    logger.info state
    [state, reg]
  end

  def soap_request
    @_soap_request ||= Hash.from_xml(request.body.read).with_indifferent_access
  end

  def distinguished_name
    @distinguished_name ||= request.env["SSL_CLIENT_S_DN"] || request.env["HTTP_DIGID_SSL_CLIENT_S_DN"]
  end

  def fingerprint
    @fingerprint ||= request.env["HTTP_DIGID_SSL_CLIENT_FINGERPRINT"]
  end
end
