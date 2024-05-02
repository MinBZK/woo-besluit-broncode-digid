
/*
  Deze broncode is openbaar gemaakt vanwege een Woo-verzoek zodat deze
  gericht is op transparantie en niet op hergebruik. Hergebruik van 
  de broncode is toegestaan onder de EUPL licentie, met uitzondering 
  van broncode waarvoor een andere licentie is aangegeven.
  
  Het archief waar dit bestand deel van uitmaakt is te vinden op:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Eventuele kwetsbaarheden kunnen worden gemeld bij het NCSC via:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  onder vermelding van "Logius, openbaar gemaakte broncode DigiD" 
  
  Voor overige vragen over dit Woo-besluit kunt u mailen met open@logius.nl
  
  This code has been disclosed in response to a request under the Dutch
  Open Government Act ("Wet open Overheid"). This implies that publication 
  is primarily driven by the need for transparence, not re-use.
  Re-use is permitted under the EUPL-license, with the exception 
  of source files that contain a different license.
  
  The archive that this file originates from can be found at:
    https://github.com/MinBZK/woo-besluit-broncode-digid
  
  Security vulnerabilities may be responsibly disclosed via the Dutch NCSC:
    https://www.ncsc.nl/contact/kwetsbaarheid-melden
  using the reference "Logius, publicly disclosed source code DigiD" 
  
  Other questions regarding this Open Goverment Act decision may be
  directed via email to open@logius.nl
*/

package nl.logius.digid.digilevering.api.util;

import nl.logius.digid.digilevering.api.model.Container;
import nl.logius.digid.digilevering.api.model.Element;

import java.util.List;

public class CategorieUtil {

    public static final String CATEGORIE_IDENTIFICATIENUMMERS = "01";
    public static final String CATEGORIE_IDENTIFICATIENUMMERS_OUDE_WAARDE = "51";
    public static final String ELEMENT_A_NUMMER = "0110";
    public static final String ELEMENT_BURGERSERVICENUMMER = "0120";
    public static final String CATEGORIE_OVERLIJDEN = "06";
    public static final String CATEGORIE_OVERLIJDEN_OUDE_WAARDE = "56";
    public static final String ELEMENT_DATUM_OVERLIJDEN = "0810";
    public static final String CATEGORIE_INSCHRIJVING = "07";
    public static final String CATEGORIE_INSCHRIJVING_OUDE_WAARDE = "57";
    public static final String ELEMENT_DATUM_OPSCHORTING = "6710";
    public static final String ELEMENT_REDEN_OPSCHORTING = "6720";

    public static Container createCategorie(String categorieNummer, String... elementNummerValues) {
        Container categorie = new Container();
        categorie.setNummer(categorieNummer);
        for(int i = 0 ; i < elementNummerValues.length; i+=2) {
            Element element = new Element();
            element.setNummer(elementNummerValues[i]);
            element.setValue(elementNummerValues[i+1]);
            categorie.getElement().add(element);
        }
        return categorie;
    }

    public static String findANummer(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_IDENTIFICATIENUMMERS, ELEMENT_A_NUMMER);
    }

    public static String findBsn(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_IDENTIFICATIENUMMERS, ELEMENT_BURGERSERVICENUMMER);
    }

    public static String findBsnOudeWaarde(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_IDENTIFICATIENUMMERS_OUDE_WAARDE, ELEMENT_BURGERSERVICENUMMER);
    }

    public static String findDatumOverlijden(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_OVERLIJDEN, ELEMENT_DATUM_OVERLIJDEN);
    }

    public static String findDatumOverlijdenOudeWaarde(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_OVERLIJDEN_OUDE_WAARDE, ELEMENT_DATUM_OVERLIJDEN);
    }

    public static String findRedenOpschorting(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_INSCHRIJVING, ELEMENT_REDEN_OPSCHORTING);
    }

    public static String findRedenOpschortingOudeWaarde(List<Container> categorieList){
        return findValue(categorieList, CATEGORIE_INSCHRIJVING_OUDE_WAARDE, ELEMENT_REDEN_OPSCHORTING);
    }

    private static String findValue(List<Container> categorieList, String categorieNummer, String elementNummer){
        String result = categorieList.stream()
                .filter(cat -> cat.getNummer() != null)
                .filter(cat -> cat.getNummer().equals(categorieNummer))
                .findFirst()
                .orElse(new Container())
                .getElement()
                .stream()
                .filter(el -> el.getNummer() != null)
                .filter(el -> el.getNummer().equals(elementNummer))
                .findFirst()
                .orElse(new Element())
                .getValue();
        return result;
    }
}
