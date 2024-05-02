
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

package nl.logius.digid.dgl.util;

import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.digilevering.api.util.CategorieUtil;

public class TestDglMessagesUtil {

    public static Ag01 createTestAg01(String bsn, String status, String datum){
        Ag01 ag01 = new Ag01();
        ag01.setStatus(status);
        ag01.setDatum(datum);

        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element bsnElement = new Element();
        bsnElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        bsnElement.setValue(bsn);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue("A" + bsn);

        container.getElement().add(bsnElement);
        container.getElement().add(aNummerElement);
        ag01.getCategorie().add(container);
        return ag01;
    }

    public static Af01 createTestAf01(String bsn){
        Af01 af01 = new Af01();
        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        aNummerElement.setValue(bsn);

        af01.setFoutreden("I");

        container.getElement().add(aNummerElement);
        af01.getCategorie().add(container);
        return af01;
    }

    public static Ag31 createTestAg31(String bsn, String status, String datum){
        Ag31 ag31 = new Ag31();
        ag31.setStatus(status);
        ag31.setDatum(datum);

        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element bsnElement = new Element();
        bsnElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        bsnElement.setValue(bsn);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue("A" + bsn);

        container.getElement().add(bsnElement);
        container.getElement().add(aNummerElement);
        ag31.getCategorie().add(container);
        return ag31;
    }

    public static Af11 createTestAf11(String aNummer) {
        Af11 af11 = new Af11();
        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue(aNummer);

        af11.setFoutreden("I");

        container.getElement().add(aNummerElement);
        af11.getCategorie().add(container);
        return af11;
    }

    public static Gv01 createTestGv01(String aNummer, String status, String bsnOud, String bsnNieuw) {
        Gv01 gv01 = new Gv01();

        gv01.setANummer(aNummer);

        Container containerIdentificatie = new Container();
        containerIdentificatie.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);
        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue(aNummer);
        containerIdentificatie.getElement().add(aNummerElement);
        Element bsnElement = new Element();
        bsnElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        bsnElement.setValue(bsnNieuw);
        containerIdentificatie.getElement().add(bsnElement);

        Container containerIdentificatieOud = new Container();
        containerIdentificatieOud.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS_OUDE_WAARDE);
        Element bsnOudElement = new Element();
        bsnOudElement.setNummer(CategorieUtil.ELEMENT_BURGERSERVICENUMMER);
        bsnOudElement.setValue(bsnOud);
        containerIdentificatieOud.getElement().add(bsnOudElement);

        Container containerInschrijving = new Container();
        containerInschrijving.setNummer(CategorieUtil.CATEGORIE_INSCHRIJVING);
        Element redenOpschortingElement = new Element();
        redenOpschortingElement.setNummer(CategorieUtil.ELEMENT_REDEN_OPSCHORTING);
        redenOpschortingElement.setValue(status);
        containerInschrijving.getElement().add(redenOpschortingElement);

        gv01.getCategorie().add(containerIdentificatie);
        gv01.getCategorie().add(containerInschrijving);
        gv01.getCategorie().add(containerIdentificatieOud);
        return gv01;
    }

    public static Null createTestNullMessage() {
        Null nullMessage = new Null();

        return nullMessage;
    }

    public static Wa11 createTestWa11(String testAnummer, String testNieuwAnummer, String datumGeldigheid) {
        Wa11 wa11 = new Wa11();
        wa11.setANummer(testNieuwAnummer);
        wa11.setDatumGeldigheid(datumGeldigheid);

        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);

        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue(testAnummer);
        container.getElement().add(aNummerElement);
        wa11.getCategorie().add(container);

        return wa11;
    }

    public static Av01 createTestAv01(String aNummer) {
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

    public static Ap01 createTestAp01(String bsn) {
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

    public static Ng01 createTestNg01(String aNummer, String redenOpschorting, String datumOpschorting) {
        Ng01 ng01 = new Ng01();
        Container containerIdentificatie = new Container();
        containerIdentificatie.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);
        Element aNummerElement = new Element();
        aNummerElement.setNummer(CategorieUtil.ELEMENT_A_NUMMER);
        aNummerElement.setValue(aNummer);
        containerIdentificatie.getElement().add(aNummerElement);

        Container containerInschrijving = new Container();
        containerInschrijving.setNummer(CategorieUtil.CATEGORIE_INSCHRIJVING);
        Element datumOpschortingElement = new Element();
        datumOpschortingElement.setNummer(CategorieUtil.ELEMENT_DATUM_OPSCHORTING);
        datumOpschortingElement.setValue(datumOpschorting);
        containerInschrijving.getElement().add(datumOpschortingElement);
        Element redenOpschortingElement = new Element();
        redenOpschortingElement.setNummer(CategorieUtil.ELEMENT_REDEN_OPSCHORTING);
        redenOpschortingElement.setValue(redenOpschorting);
        containerInschrijving.getElement().add(redenOpschortingElement);

        ng01.getCategorie().add(containerIdentificatie);
        ng01.getCategorie().add(containerInschrijving);
        return ng01;
    }
}
