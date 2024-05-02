
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

package nl.logius.digid.dgl.service;

import nl.logius.digid.digilevering.api.model.Ap01;
import nl.logius.digid.digilevering.api.model.Av01;
import nl.logius.digid.digilevering.api.model.Container;
import nl.logius.digid.digilevering.api.model.Element;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import org.springframework.stereotype.Component;

@Component
public class DglMessageFactory {

    public Ap01 createAp01(String bsn) {
        Ap01 ap01 = new Ap01();
        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element bsnElement = new Element();
        bsnElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        bsnElement.setValue(bsn);

        container.getElement().add(bsnElement);
        ap01.getCategorie().add(container);
        ap01.setHerhaling(0);
        ap01.setRandomKey("SSSSSSSS");

        return ap01;
    }

    public Av01 createAv01(String aNummer) {
        Av01 av01 = new Av01();
        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue(aNummer);

        container.getElement().add(aNummerElement);
        av01.getCategorie().add(container);
        av01.setHerhaling(0);
        av01.setRandomKey("SSSSSSSS");

        return av01;
    }
}
