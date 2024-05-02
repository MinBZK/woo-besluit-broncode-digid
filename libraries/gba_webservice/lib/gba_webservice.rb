
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

# The GBA webservice facilitates requests to receive the required user data
# for you application. GBA is the Municipal Administration data from
# individuals of the Netherlands.

require 'active_support'
require 'active_support/core_ext/object/blank'
require 'savon'
require 'nokogiri'
require 'json'
require 'builder'
require 'ostruct'

require 'gba_webservice/version'

class GbaWebservice
  attr_reader :xml_doc, :client, :options

  CHANGE_PASSWORD_WSDL = 'config/wsdl/LRDAdministrationSOAP0_4.wsdl'

  # return the following Gba rubrieken:
  FIELD_MAPPINGS = {
    # 01 persoon
    '010110' => :a_nummer,
    '010120' => :bsn,
    '010210' => :voornamen,
    '010220' => :adellijke_titel,
    '010230' => :voorvoegsel_geslachtsnaam,
    '010240' => :geslachtsnaam,
    '010310' => :geboortedatum,
    '010410' => :geslachtsaanduiding,
    '016110' => :aanduiding_naamgebruik,
    '018310' => :aanduiding_gegevens_in_onderzoek_algemeen,
    '018330' => :datum_einde_onderzoek_algemeen,
    # 04 nationaliteit
    '040510' => :nationaliteit,
    '048310' => :aanduiding_gegevens_in_onderzoek_nationaliteit,
    '048330' => :datum_einde_onderzoek_nationaliteit,
    # 05 huwelijk / geregistreerd partnerschap
    '050230' => :voorvoegsel_geslachtsnaam_partner,
    '050240' => :geslachtsnaam_partner,
    '050610' => :datum_huwelijk,
    '050710' => :datum_ontbinding_huwelijk,
    # 07 inschrijving
    '077010' => :indicatie_geheim,
    '076710' => :datum_opschorting_bijhouding,
    '076720' => :omschrijving_reden_opschorting_bijhouding,
    # 08 verblijfplaats
    '080910' => :gemeente_van_inschrijving,
    '081110' => :straatnaam,
    '081120' => :huisnummer,
    '081130' => :huisletter,
    '081140' => :huisnummertoevoeging,
    '081150' => :aanduiding_bij_huisnummer,
    '081160' => :postcode,
    '081170' => :woonplaats,
    '081210' => :locatieomschrijving,
    '088310' => :aanduiding_gegevens_in_onderzoek_adres,
    '088330' => :datum_einde_onderzoek_adres,
    # 12 document
    '123510' => :soort_reisdocument,
    '123520' => :nummer_reisdocument,
    '123550' => :datum_einde_geldigheid_reisdocument,
    '123560' => :datum_inhouding_vermissing_reisdocument,
    '123570' => :aanduiding_inhouding_vermissing_reisdocument,
    '128310' => :aanduiding_gegevens_in_onderzoek_reisdocument,
    '128330' => :datum_einde_onderzoek_reisdocument,
  }.freeze

  # GBA Productie returns this information if present, but returns authorization error if requested
  FORBIDDEN_ATTRIBUTES = %w(018310 018320 018330 048310 048320 048330 076710 076720 088310 088320 088330 128310 128320 128330).to_set.freeze

  CODE_STATUS_MAPPING = {
    'A' => 'valid',
    'G' => 'not_found',
    'M' => 'ministerial_decree',
    'O' => 'deceased',
    'E' => 'emigrated',
    'R' => 'rni',
    'F' => 'suspended_error',
    '.' => 'suspended_unknown'
  }.freeze

  GBA_NAMESPACE = { 'ver' => 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS' }
  CATEGORIE_HUWELIJK = 5
  CATEGORIE_NATIONALITEIT = 4
  CATEGORIE_REISDOCUMENT = 12
  SECTIE_NATIONALITEIT = "040510"

  def initialize(options)
    @options = options
  end

  # signature sucks, but remains here for backwards compatibility
  def self.get_gba_data(gba_url, search, ssl=nil)
    enrich(new(ssl: ssl, wsdl_endpoint: gba_url, search: search).search)
  end

  def self.version
    VERSION
  end

  def search
    if response_xml = get_response
      @xml_doc = Nokogiri response_xml
    end
    xml_doc ? Data.new(parse_xml) : nil
  end

  private

  def client
    unless @client
      # TODO fix, Savon is not used as intended.
      # It is only used to format request with soap headers, not for
      ssl = options.fetch(:ssl, nil)

      @client = Savon.client(:log => false) do |globals|
        globals.ssl_cert_key_file ssl['ssl_cert_key_file'].to_s if ssl
        globals.ssl_cert_file ssl['ssl_cert_file'].to_s if ssl
        globals.ssl_cert_key_password ssl['ssl_cert_key_password'].to_s if ssl
        globals.ssl_ca_cert_file ssl['ssl_ca_cert_file'].to_s if ssl
        globals.basic_auth [ssl['username'].to_s, ssl['password'].to_s] if ssl
        globals.ssl_verify_mode :client_once

        globals.document options[:wsdl_document] if options[:wsdl_document]
        globals.endpoint options[:wsdl_endpoint] if options[:wsdl_endpoint]
        globals.namespace "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"

        globals.logger GbaWebservice.logger
      end
    end
    @client
  end

  def get_response
    response = client.call(:vraag, request_args)
  rescue Savon::HTTPError => e
    GbaWebservice.logger.error "HTTP error when calling GBA: #{e.message}"
    nil
  else
    response.to_xml
  end

  # This method is just used for testing purposes.
  # TODO: Please integrate this method with the method 'get_response'
  def build_request
    client.build_request(:vraag, request_args)
  end

  def request_args
    {xml: soap_body, soap_action: nil}
  end

  def soap_body
    xml_markup = Builder::XmlMarkup.new
    # since Savon 2 we lost this argument, but apparantly is doen not matter:
    #SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS

    xml = xml_markup.tag!("ver:in0") do |xml|
      xml.tag! "ver:indicatieAdresvraag", "0"
      xml.tag! "ver:indicatieZoekenInHistorie", "0"
      xml.tag! "ver:masker" do |item|
        FIELD_MAPPINGS.each_key do |section|
          next if FORBIDDEN_ATTRIBUTES.include?(section)
          item.tag! "ver:item", section
        end
      end
      xml.tag! "ver:parameters" do
        options[:search].each do |section, value|
          xml.tag! "ver:item" do
            xml.tag! "ver:rubrieknummer", section
            xml.tag! "ver:zoekwaarde", value
          end
        end
      end
    end

    "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
  end

  def parse_xml
    response = {}
    ontbonden_huwelijken, actief_huwelijk = [], []
    nationaliteiten = []
    reisdocumenten = []

    xml_doc.xpath("//ver:item/ver:categorievoorkomens/ver:item", GBA_NAMESPACE).each do |categorievoorkomens_xml|
      categorienummer = categorievoorkomens_xml.xpath("ver:categorienummer", GBA_NAMESPACE).text.strip.to_i
      nationaliteit = {}
      reisdocument = {}
      categorievoorkomens_xml.xpath("ver:elementen/ver:item", GBA_NAMESPACE).each do |element_xml|
        section, section_value = parse_element_xml(element_xml, categorienummer)
        if categorienummer == CATEGORIE_HUWELIJK
          actief_huwelijk      << categorievoorkomens_xml if section == '050610'
          ontbonden_huwelijken << categorievoorkomens_xml if section == '050710'
        elsif categorienummer == CATEGORIE_NATIONALITEIT
          nationaliteit[section] = (section == SECTIE_NATIONALITEIT ? section_value.rjust(4, '0') : section_value)
        elsif categorienummer == CATEGORIE_REISDOCUMENT
          reisdocument[section] = section_value
        elsif categorienummer > 0
          response[section] = section_value
        end
      end
      nationaliteiten << Data.new(nationaliteit) unless nationaliteit.empty?
      reisdocumenten << Data.new(reisdocument) unless reisdocument.empty?
    end

    if (actief_huwelijk + ontbonden_huwelijken).count > 0
      laatste_huwelijk(actief_huwelijk, ontbonden_huwelijken).xpath("ver:elementen/ver:item", GBA_NAMESPACE).each do |element_xml|
        section, section_value = parse_element_xml(element_xml, CATEGORIE_HUWELIJK)
        response[section]      = section_value
      end
    end
    response.merge('nationaliteiten' => nationaliteiten, 'reisdocumenten' => reisdocumenten, 'status' => determine_status(response))
  end

  def laatste_huwelijk actief_huwelijk, ontbonden_huwelijken
    return actief_huwelijk.first if actief_huwelijk.count > 0

    laatste_ontbonden_huwelijk = nil
    laatste_datum = 0
    ontbonden_huwelijken.each do |scheiding|
      scheiding.xpath("ver:elementen/ver:item", GBA_NAMESPACE).each do |element_xml|
        section, section_value = parse_element_xml element_xml, CATEGORIE_HUWELIJK
        if section == "050710"
          datum = section_value.to_i
        end
        if datum && (datum >= laatste_datum)
          laatste_datum = datum
          laatste_ontbonden_huwelijk = scheiding
        end
      end
    end
    laatste_ontbonden_huwelijk
  end

  def parse_element_xml(element_xml, categorienummer)
    categorienummer   = categorienummer.to_s.strip.rjust(2, '0')
    section_value     = element_xml.xpath('ver:waarde', GBA_NAMESPACE).text.strip
    section_number    = element_xml.xpath('ver:nummer', GBA_NAMESPACE).text.strip.rjust(4, '0')
    section           = "#{categorienummer}#{section_number}"
    [section, section_value]
  end

  def determine_status(response)
    response_code = if response['076720'].to_s.strip.present?
      response['076720'].strip # Set status to section code
    else
      code = xml_doc.xpath("//ns1:letter", "ns1" => "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS").text.strip
      code.present? ? code : nil
    end
    CODE_STATUS_MAPPING.fetch(response_code, 'error')
  end

  def self.enrich(data)
    return unless data
    data.set('onderzoek_algemeen?', data['018310'].to_s.strip.present? && data['018330'].to_s.strip.blank?)
    data.set('onderzoek_geboortedatum?', data.onderzoek_algemeen? && %w(010310 010300).include?(data['018310']))
    data.set('onderzoek_adres?', data['088310'].to_s.strip.present? && data['088330'].to_s.strip.blank?)
    data.nationaliteiten.each do |nationaliteit|
      nationaliteit.set('onderzoek?', nationaliteit['048310'].to_s.strip.present? && nationaliteit['048330'].to_s.strip.blank?)
    end
    data.reisdocumenten.each do |reisdocument|
      reisdocument.set('vervaldatum', parse_date(reisdocument['123550'])) if reisdocument['123550']
      reisdocument.set('vermist?', reisdocument['123570'].to_s.strip.present?)
      reisdocument.set('onderzoek?', reisdocument['128310'].to_s.strip.present? && reisdocument['128330'].to_s.strip.blank?)
    end
    data
  end

  def self.parse_date(date)
    return nil unless date && /^\d{8}$/ =~ date
    Date.parse(date.sub(/0000$/, '0101').sub(/00$/, '01'))
  rescue ArgumentError
    nil
  end

  def self.logger
    if defined?(Rails)
      Rails.logger
    else
      Logger.new('log/gba.log')
    end
  end

  class Data < OpenStruct
    alias_method :get, :[]
    alias_method :set, :[]=

    def initialize(attributes=nil)
      @attributes = attributes || {}
      super humanize_attributes(@attributes)
    end

    def [](key)
      @attributes[key]
    end

    def []=(key, value)
      @attributes[key] = value
    end

    def has_key?(key)
      @attributes.include?(key)
    end

    def to_json(state=nil)
      @attributes.to_json
    end

    def as_json(options=nil)
      each_pair.to_h
    end

    private

    def humanize_code(code)
      if attr_name = FIELD_MAPPINGS[code]
        attr_name
      elsif code.to_s =~ /\D/
        code
      else
        GbaWebservice.logger.warn "Unsupported gba field: #{code}"
        nil
      end
    end

    def humanize_attributes(attributes)
      attributes.each_with_object({}) do |(code,value), result|
        key = humanize_code(code)
        result[key] = value if key
      end
    end
  end
end
