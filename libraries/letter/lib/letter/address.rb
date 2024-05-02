
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

module Letter
  module Address
    # De straatnaam komt in de eerste adresregel terecht
    # (bijvoorbeeld xml path //Brieven/Brief/ActiveringsBrief/Adresgegevens/Adresregel1/)
    # en wordt in dezelfde adresregel gevolgd door het huisnummer..
    # De straatnaam wordt gevuld met de waarde uit 08.12.10. Als deze niet gevuld is wordt straatnaam opgebouwd uit 08.11.10.
    def straatnaam(gba)
      if gba['081210'].present?
        gba['081210']                # Locatiebeschrijving
      else
        gba['081110']                # Straatnaam
      end
    end

     # Het huisnummer wordt opgebouwd uit 08.11.20 en indien ingevuld 08.11.30 en indien gevuld 08.11.40.
    def huisnummer(gba)
      "#{gba["081120"]}#{gba['081130']} #{gba['081140']} #{gba['081150']}".strip
    end

    # Indien categorie 08.11.70 aanwezig is dient woonplaats deze waarde te krijgen.
    # Anders dient zij gevuld te worden met de waarde uit 08.09.10
    # deze waarde is een code en dient door middel van een
    # vertaaltabel omgezet naar een leesbare gemeentenaam.
    def woonplaats(gba, csv_path='')
      if gba['081170'].present?
        gba['081170']                # Verblijfplaats / woonplaats
      else
        @csv = codes(csv_path) unless csv_path == ''
        @csv.codes(gba['080910']) if gba['080910'].present? # Gemeente van inschrijving (code) -> gemeente
      end
    end

    def codes(csv_path)
      csv = LoadCsv.new(csv_path)
      csv.fetch
      csv
    end
  end
end
