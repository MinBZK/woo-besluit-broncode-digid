
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

package nl.logius.digid.dgl.controller;

import nl.logius.digid.digilevering.api.model.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AfnemersberichtAanDGLFactory {

    protected final String digidOIN;
    protected final String digileveringOIN;

    public AfnemersberichtAanDGLFactory(@Value("${digilevering.oin.self}") String digidOIN, @Value("${digilevering.oin.other}") String digileveringOIN) {
        this.digidOIN = digidOIN;
        this.digileveringOIN = digileveringOIN;
    }

    public AfnemersberichtAanDGL createAfnemersberichtAanDGL() {
        AfnemersberichtAanDGL result = new AfnemersberichtAanDGL();
        BerichtHeaderType berichtHeader = new BerichtHeaderType();
        berichtHeader.setOntvangerId(digileveringOIN);
        berichtHeader.setVerstrekkerId(digidOIN);
        berichtHeader.setDatumtijdstempelVerstrekker(getCurrentTime());
        berichtHeader.setKenmerkVerstrekker(UUID.randomUUID().toString() + "@digid.nl");
        berichtHeader.setBerichtversie("1.0");
        result.setBerichtHeader(berichtHeader);
        AfnemersberichtAanDGL.Stuurgegevens stuurgegevens = new AfnemersberichtAanDGL.Stuurgegevens();
        VersiebeheerType berichtsoort = new VersiebeheerType();
        stuurgegevens.setBerichtsoort(berichtsoort);
        stuurgegevens.setVersieBerichttype("3.10");
        result.setStuurgegevens(stuurgegevens);
        result.setInhoud(new AfnemersInhoudType());
        return result;
    }

    public AfnemersberichtAanDGL createAfnemersberichtAanDGL(Ap01 ap01) {
        AfnemersberichtAanDGL result = createAfnemersberichtAanDGL();
        result.getStuurgegevens().getBerichtsoort().setNaam("Ap01");
        result.getStuurgegevens().getBerichtsoort().setVersie("1.0");
        result.getInhoud().setAp01(ap01);
        return result;
    }

    public AfnemersberichtAanDGL createAfnemersberichtAanDGL(Av01 av01) {
        AfnemersberichtAanDGL result = createAfnemersberichtAanDGL();
        result.getStuurgegevens().getBerichtsoort().setNaam("Av01");
        result.getStuurgegevens().getBerichtsoort().setVersie("1.0");
        result.getInhoud().setAv01(av01);
        return result;
    }

    public XMLGregorianCalendar getCurrentTime() {
        ZonedDateTime localDateTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        try {
            XMLGregorianCalendar xmlGregorianCalendar =
                    DatatypeFactory.newInstance().newXMLGregorianCalendar(formatter.format(localDateTime));
            return xmlGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
