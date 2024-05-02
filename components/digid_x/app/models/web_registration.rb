
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

# voor het controleren van aanvragen via het administratie koppelvlak
class WebRegistration < ActiveRecord::Base
  belongs_to :sector

  before_validation :parse_json

  validates :sectoraalnummer, presence: true, length: { in: 1..15 }
  validates :webadres, length: { in: 0..256 }, if: -> { webadres.present? }
  validate :valid_sectorcode?
  validate :valid_naam?
  validate :valid_adres?
  validate :valid_aanvraagvoorkeuren?
  validate :valid_activeringstermijn?

  # retrieve the return url if available
  def self.get_webdienst_url(session)
    return unless session[:webdienst]
    return unless (reg = WebRegistration.find_by("id = ?", session[:web_registration_id])).present?
    reg.webadres
  end

  # creates a webregistration against the validations
  def self.soap_to_registration(soap_request, aanvraag)
    WebRegistration.new do |web_reg|
      web_reg.aanvraag          = soap_request.to_json # keep the whole request
      web_reg.webadres          = aanvraag[:urlbi]
      web_reg.sectoraalnummer   = aanvraag[:sectoraal_nummer]
      web_reg.sector_id         = Sector.get(aanvraag[:sectorcode])
      web_reg.geldigheidsdsduur = aanvraag[:activeringstermijn]
      web_reg.aanvraagnummer    = aanvraag[:rid]
      web_reg.webdienst_id = soap_request[:webservice_id]
    end
  end

  # check the expiry date of a web request.
  # return the expiry time (days) if expired
  # return false if ok
  def expired?
    if created_at < (Time.zone.now - geldigheidsdsduur.to_i.days)
      geldigheidsdsduur
    else
      false
    end
  end

  def web_registration_too_soon?
    aantal_dagen_configured = ::Configuration.get_int("snelheid_aanvragen")

    Registration.where(burgerservicenummer: sectoraalnummer, status: Registration::Status::REQUESTED).where("created_at > ?", (Time.zone.now - aantal_dagen_configured.days)).exists?
  end


  def web_registration_too_often?
    this_month = Time.zone.now.beginning_of_month..Time.zone.now.end_of_month
    counter = Registration.where(burgerservicenummer: sectoraalnummer, status: Registration::Status::REQUESTED).where(created_at: this_month).count

    limit = ::Configuration.get_int("blokkering_aanvragen")
    limit.positive? && counter >= limit ? Time.zone.now.next_month.beginning_of_month : false
  end

  private

  # let's parse the json once before all validation rules
  def parse_json
    @aanvraag_str = JSON.parse(aanvraag) if aanvraag.present?
  end

  # checks sectorcode table for inclusion
  def valid_sectorcode?
    find_sector = Sector.get(@aanvraag_str["Sectorgegevens"]["Sectorcode"])
    errors.add(:aanvraag, "unknown sectorcode") unless find_sector
  end

  # valid_naam checks validations for the Naamgegevens of
  # the Soap request aanvragen_sector_account_request
  def valid_naam?
    # occurs 1: Min. 1 en max. 256 tekens
    geslachtsnaam = @aanvraag_str["Naamgegevens"]["Geslachtsnaam"]
    if geslachtsnaam.blank? || geslachtsnaam.length > 256
      errors.add(:aanvraag, "geslachtsnaam not valid")
    end

    check_length(@aanvraag_str["Naamgegevens"]["Voornaam"], 256)
    check_length(@aanvraag_str["Naamgegevens"]["Voorvoegsel"], 16)

    # there is a bug in the XSD since it passes empty fields so:
    # occurs 1: (M|V|O) if empty make it onzijdig (O) (when making letters)
    geslacht = @aanvraag_str["Naamgegevens"]["Geslachtsaanduiding"]
    return if geslacht.blank? || (%w(M V O).include? geslacht)

    errors.add(:aanvraag, "geslacht not valid")
  end

  # helper to check field length
  def check_length(field, length)
    return unless field.present? && field.length > length

    errors.add(:aanvraag, "Naamgegevens not valid")
  end

  # valid_adres checks validations for the Adresgegevens of
  # the Soap request aanvragen_sector_account_request
  def valid_adres?
    # minOccurs=1; 4 cijfers, volgens GBA conventie
    landcode = @aanvraag_str["Adresgegevens"]["Woonlandcode"]
    unless landcode.match(Regexp.only(CharacterClass::COUNTRY_CODE))
      errors.add(:aanvraag, "landcode not valid")
    end

    # Adresregel1; maxlength 64
    adres_regel1 = @aanvraag_str["Adresgegevens"]["Adresregel1"]
    if adres_regel1.blank? || adres_regel1.length > 64
      errors.add(:aanvraag, "adres_regel1 not valid")
    end

    # Adresregel2-4; maxlength 64
    check_adresregel(@aanvraag_str["Adresgegevens"]["Adresregel2"], 2)
    check_adresregel(@aanvraag_str["Adresgegevens"]["Adresregel3"], 3)
    check_adresregel(@aanvraag_str["Adresgegevens"]["Adresregel4"], 4)
  end

  # helper to check adresregels
  def check_adresregel(adres_regel, nr)
    return unless adres_regel.present? && adres_regel.length > 64

    errors.add(:aanvraag, "adres_regel#{nr} not valid")
  end

  # valid_aanvraagvoorkeuren checks validations for the Aanvraagvoorkeuren of
  # the Soap request aanvragen_sector_account_request
  def valid_aanvraagvoorkeuren?
    # minOccurs 0, Max 256 tekens
    instantienaam = @aanvraag_str["Aanvraagvoorkeuren"]["InstantieNaam"]
    if instantienaam.present? && instantienaam.length > 256
      errors.add(:aanvraag, "instantienaam not valid")
    end

    # minOccurs 0, Max 4 tekens
    taalvoorkeur = @aanvraag_str["Aanvraagvoorkeuren"]["Taalvoorkeur"]
    return unless taalvoorkeur.present? && taalvoorkeur.length > 4

    errors.add(:aanvraag, "taalvoorkeur not valid")
  end

  # valid_activeringstermijn checks validations for the Aanvraagvoorkeuren of
  # the Soap request aanvragen_sector_account_request
  def valid_activeringstermijn?
    # minOccurs 0, Max 4 tekens
    activeringstermijn = @aanvraag_str["Aanvraagvoorkeuren"]["Activeringstermijn"]

    sector = Sector.find_by(number_name: @aanvraag_str["Sectorgegevens"]["Sectorcode"])
    max_activeringstermijn = sector ? sector.expiration_time : 45

    ## check activeringstermijn when present: niet langer dan 4 tekens & niet groter dan het maximale (van deze sector)
    return unless activeringstermijn.present? && (activeringstermijn.length > 4 || activeringstermijn.to_i > max_activeringstermijn.to_i)

    errors.add(:aanvraag, "activeringstermijn not valid")
  end
end
