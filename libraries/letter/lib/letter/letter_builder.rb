
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

require 'builder'
require 'tempfile'
require 'letter/salutation'
require 'letter/translation'
require 'letter/address'
# Class for building the XML from the processed letter data
# 001 -> Activeringsbrief niveau basis
# 004 -> Activeringsbrief niveau midden
# 005 -> Activeringsbrief uitbreiding niveau midden (exactly the same as 001)
# 006 -> Activeringsbrief uitbreiding niveau midden met app
# 008 -> Wachtwoord herstelbrief
# 009 -> SMS herstelbrief
# 011 -> Aanvraag deblokkeringscode eid
# 012 -> Activeren app one device.
#
# <?xml version="1.0"?>
# <Brieven>
#   <Brief>
#     <BriefStandaardinhoud>
#       <Briefcode/> [ 001 | 004 | 005 | 008 | 009 | 011 ]
#       <Taalcode/> always 0001
#       <Aanvraagdatum/> in dd-mm-yyyy
#PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP
#     </BriefStandaardinhoud>
#     <ActiveringsBrief>
#       <Adresgegevens>
#PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP
#         <Adresregel1/> straatnaam (08.12.10 ||= 08.11.10) huisnummer (08.11.20 + 08.11.30) + 08.11.40)
#         <Adresregel2/> postcode (08.11.60) woonplaats (08.11.70 ||= gemeente['08.09.10'])
#         <Landcode/>  always 6030
#       </Adresgegevens>
#       <ActiveringsBericht>
#         <Activeringscode/>
#         <Vervaldatum/> in dd-mm-yyyy
#       </ActiveringsBericht>
#     </ActiveringsBrief>
#   </Brief>
# </Brieven>
class LetterBuilder
  include Letter::Salutation
  include Letter::Translation
  include Letter::Address

  BRIEFCODE_MAPPING = {
     "activation_aanvraag"                          => "001",
     "uitbreiding_sms"                              => "004",
     "uitbreiding_app"                              => "006",
     "recovery_password"                            => "008",
     "recovery_sms"                                 => "009",
     "aanvraag_deblokkeringscode_rijbewijs"         => "011",
     "activation_app_one_device"                    => "012",
     "balie_aanvraag"                               => "014",
     "aanvraag_deblokkeringscode_identiteitskaart"  => "015",
     "activeringscode_aanvraag_via_digid_app"       => "018",
     "activation_aanvraag_met_sms"                  => "019",
     "app_notification_letter"                      => "020",
  }

  # 1) resets the counter for csv controle
  # 2) loads the gemeentecode conversion table from file
  # 3) creates an empty xml object
  def initialize(csv_path, locale = nil)
    @locale = locale

    @letter_counter = {}

    # load gemeente csv
    @csv = LoadCsv.new(csv_path)
    @csv.fetch
    @xml_file = Tempfile.new 'letter'
    @xml = Builder::XmlMarkup.new(:target => @xml_file)
    @xml.instruct!
  end

  # returns xml
  def xml
    @xml_file.rewind
    @xml_file.read
  end

  def xml_file_path
    @xml_file.path
  end

  # Create XML from letters
  def create_xml_from(letters_proxy)
    @xml.Brieven(:xmlns => 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS') do
      i = 0
      letters_proxy.find_each do |letter|
        i += 1
        GC.start if i % 100 == 0
        build_letter_xml letter
        letter_ids << letter.id
      end
    end
    self
  end

  # returns a string with briefcodes and counts
  # that were created since letter.new
  def csv
    @csv_data ||= @csv.generate(@letter_counter)
  end

  def letter_ids
    @letter_ids ||= []
  end


  private #--------------------------------------------------------------------

  def build_letter_xml(letter)
    @locale = letter.locale
    @aangetekend = letter.aangetekend

    adjust_webdienst? letter.gba
    @xml.Brief do
      @xml.BriefStandaardinhoud do
        briefcode letter.letter_type
        taalcode
        aangetekend
        aanvraagdatum letter.created_at
        aanhef letter.gba
      end
      @xml.ActiveringsBrief do
        adresgegevens letter.gba
        activerings_bericht letter
      end
    end
  end

  # fills the gba rubrieken in case the letter is of type "webdienst"
  # TODO refactor all rubirke nummers to method calls on gba
  def adjust_webdienst?(gba)
    if gba["type_bericht"].eql? "webdienst"
      gba["010220"] = ""
      gba["016110"] = "E"
      gba["010410"] = gba["naam_gegevens"]["Geslachtsaanduiding"]
      gba["010210"] = gba["naam_gegevens"]["Voornaam"]
      gba["010230"] = gba["naam_gegevens"]["Voorvoegsel"]
      gba["010240"] = gba["naam_gegevens"]["Geslachtsnaam"]
      gba["081110"] = gba["adres_gegevens"]["Adresregel1"]
      gba["081160"] = gba["adres_gegevens"]["Adresregel2"]
      gba["woonlandcode"] = gba["adres_gegevens"]["Woonlandcode"]
    else
      gba["woonlandcode"] = "PPPP"
    end
  end

  # -------------------------------------

  # next to building the xml element, it also counts the letters
  def briefcode(letter_type)
    code = briefcode_from(letter_type)
    @letter_counter.key?(code) ? @letter_counter[code]+=1 : @letter_counter[code] = 1
    @xml.Briefcode code
  end

  def taalcode
    @xml.Taalcode({
      "nl" => "0001",
      "en" => "0002"
    }[@locale])
  end

  def aangetekend
    @xml.Aangetekend({
      "true" => "Ja",
      "false" => "Nee"
    }[@aangetekend.to_s] || "Nee")
  end

  # the date the letter was created
  def aanvraagdatum(datum)
    @xml.Aanvraagdatum to_date(datum)
  end

  # build the Aanhef xml content
  def aanhef(gba)
    @xml.Aanhef naam_aanhef(gba)
  end

  # -----------------------------------

  # creates the xml for the name + adres
  def adresgegevens(gba)
    @xml.Adresgegevens do
      # TODO fix truncate below does NOTHING
      naamregel_1(gba)
      adresregel_1(gba)
      adresregel_2(gba)
      adresregel_3(gba)
      adresregel_4(gba)
      landcode(gba)
    end
  end

  # returns the first so_many characters
  # TODO does not seem to be used
  def truncate(str)
    str.to_s[0..249]
  end

  # the name + adellijk
  def naamregel_1(gba)
    @xml.Naamregel1 truncate(naam(gba))
  end

  # straatnaam + huisnummer
  def adresregel_1(gba)
    @xml.Adresregel1 truncate("#{straatnaam(gba)} #{huisnummer(gba)}".strip)
  end

  # postcode + woonplaats
  def adresregel_2(gba)
    @xml.Adresregel2 truncate("#{gba["081160"]} #{woonplaats(gba)}".strip)
  end

  # optional for webdienst
  def adresregel_3(gba)
    regel = gba["adres_gegevens"] && gba["adres_gegevens"]["Adresregel3"]
    @xml.Adresregel3 truncate(regel) if regel
  end

  # optional for webdienst
  def adresregel_4(gba)
    regel = gba["adres_gegevens"] && gba["adres_gegevens"]["Adresregel4"]
    @xml.Adresregel4 truncate(regel) if regel
  end

  # 6030 for The Netherlands
  def landcode(gba)
    @xml.Landcode truncate(gba["woonlandcode"])
  end

  #-------------------------------------

  # creates xml for the activation part (code + expiry date)
  def activerings_bericht(letter)
    @xml.ActiveringsBericht do
      activeringscode letter.controle_code
      vervaldatum letter.created_at, letter.geldigheidsduur
    end
  end

  # activation code
  def activeringscode(code)
    @xml.Activeringscode code
  end

  # expiry date
  def vervaldatum(aanmaakdatum, geldigheidsduur)
    @xml.Vervaldatum to_date(Date.parse(aanmaakdatum.to_s) + geldigheidsduur.to_i)
  end

  # formats a datum (String or Date) to the 31-12-2012 format
  def to_date(datum)
    if datum.class.eql? String
      begin
        # TODO do we ever get here?
        return_date = Date.parse(datum)
      rescue
        return_date = Time.now
      end
    else
      return_date = datum
    end
    return_date.strftime("%d-%m-%Y")
  end

  # transforms the letter_type string to a number format
  def briefcode_from(letter_type)
    BRIEFCODE_MAPPING[letter_type]
  end
end
