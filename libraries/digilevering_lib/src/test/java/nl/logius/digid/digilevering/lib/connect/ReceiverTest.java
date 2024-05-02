
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

package nl.logius.digid.digilevering.lib.connect;

import nl.logius.digid.digilevering.api.model.AfnemersberichtAanDGL;
import nl.logius.digid.digilevering.api.model.Ap01;
import nl.logius.digid.digilevering.api.model.Gv01;
import nl.logius.digid.digilevering.api.model.VerstrekkingAanAfnemer;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessageHeaders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static nl.logius.digid.digilevering.lib.TestUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ReceiverTest {

    @Test
    public void testUnmarshallingAfnemersberichtAanDGL() throws IOException {
        List<AfnemersberichtAanDGL> captureList = new ArrayList<>();
        Receiver<AfnemersberichtAanDGL> afnemersberichtAanDGLReceiver = new Receiver<>(new AfnemersberichtAanDGL()) {
            @Override
            protected void processMessage(AfnemersberichtAanDGL message, MessageHeaders headers) {
                captureList.add(message);
            }
        };

        Path xmlFile = Paths.get("src","test","resources", "afnemersberichtAanDGL.xml");
        String xmlMessage = Files.readString(xmlFile);
        afnemersberichtAanDGLReceiver.receiveMessage(xmlMessage, new MessageHeaders(new HashMap<>()));

        assertThat(captureList.size(), is(1));
        AfnemersberichtAanDGL afnemersberichtAanDGL = captureList.get(0);
        assertThat(afnemersberichtAanDGL.getBerichtHeader().getOntvangerId(), is("55555555555555555555"));
        assertThat(afnemersberichtAanDGL.getBerichtHeader().getVerstrekkerId(), is("77777777777777777777"));
        assertThat(afnemersberichtAanDGL.getBerichtHeader().getDatumtijdstempelVerstrekker(), is(parseTime("2020-08-16T11:12:42.566+02:00")));
        assertThat(afnemersberichtAanDGL.getBerichtHeader().getKenmerkVerstrekker(), is("kenmerk.verstrekker.1"));
        assertThat(afnemersberichtAanDGL.getBerichtHeader().getBerichtversie(), is("1.0"));
        assertThat(afnemersberichtAanDGL.getStuurgegevens().getVersieBerichttype(), is("3.10"));
        assertThat(afnemersberichtAanDGL.getStuurgegevens().getBerichtsoort().getNaam(), is("Ap01"));
        assertThat(afnemersberichtAanDGL.getStuurgegevens().getBerichtsoort().getVersie(), is("1.0"));

        assertThat(afnemersberichtAanDGL.getInhoud().getAv01(), nullValue());
        assertThat(afnemersberichtAanDGL.getInhoud().getAp01(), notNullValue());
        Ap01 ap01 = afnemersberichtAanDGL.getInhoud().getAp01();
        assertThat(ap01.getHerhaling(), is(0));
        assertThat(ap01.getRandomKey(), is("00000000"));
        assertThat(ap01.getCategorie().size(), is(1));
        assertThat(ap01.getCategorie().get(0).getNummer(), is("01"));
        assertThat(ap01.getCategorie().get(0).getElement().size(), is(1));
        assertThat(ap01.getCategorie().get(0).getElement().get(0).getNummer(), is("0120"));
        assertThat(ap01.getCategorie().get(0).getElement().get(0).getValue(), is("PPPPPPPPP"));
    }

    @Test
    public void testUnmarshallingVerstrekkingAanAfnemer() throws IOException {
        List<VerstrekkingAanAfnemer> captureList = new ArrayList<>();
        Receiver<VerstrekkingAanAfnemer> verstrekkingAanAfnemerReceiver = new Receiver<>(new VerstrekkingAanAfnemer()) {
            @Override
            protected void processMessage(VerstrekkingAanAfnemer message, MessageHeaders headers) {
                captureList.add(message);
            }
        };

        Path xmlFile = Paths.get("src","test","resources", "verstrekkingAanAfnemer.xml");
        String xmlMessage = Files.readString(xmlFile);
        verstrekkingAanAfnemerReceiver.receiveMessage(xmlMessage, new MessageHeaders(new HashMap<>()));

        assertThat(captureList.size(), is(1));
        VerstrekkingAanAfnemer verstrekkingAanAfnemer = captureList.get(0);
        assertThat(verstrekkingAanAfnemer.getDatumtijdstempelDigilevering().toString(), is("2018-02-02T11:59:04.170+01:00"));
        assertThat(verstrekkingAanAfnemer.getDatumtijdstempelLV().toString(), is("2017-11-27T14:33:05.010+01:00"));
        assertThat(verstrekkingAanAfnemer.getKenmerkDigilevering(), is("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertThat(verstrekkingAanAfnemer.getKenmerkLV(), is("SSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
        assertThat(verstrekkingAanAfnemer.getVersieBerichttype(), is("3.10"));
        assertThat(verstrekkingAanAfnemer.getAbonnement().getNaam(), is(""));
        assertThat(verstrekkingAanAfnemer.getAbonnement().getVersie(), is(""));
        assertThat(verstrekkingAanAfnemer.getBasisregistratie(), is("BRP"));
        assertThat(verstrekkingAanAfnemer.getGebeurtenissoort().getNaam(), is("Gv01"));
        assertThat(verstrekkingAanAfnemer.getGebeurtenissoort().getVersie(), is("1.0"));

        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAf01(), nullValue());
        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAf11(), nullValue());
        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAg01(), nullValue());
        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getNg01(), nullValue());
        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getNull(), nullValue());
        assertThat(verstrekkingAanAfnemer.getGebeurtenisinhoud().getGv01(), notNullValue());
        Gv01 gv01 = verstrekkingAanAfnemer.getGebeurtenisinhoud().getGv01();
        assertThat(gv01.getANummer(), is("SSSSSSSSSS"));
        assertThat(gv01.getRandomKey(), is("00000000"));
        assertThat(gv01.getCategorie().size(), is(2));
        assertThat(gv01.getCategorie().get(0).getNummer(), is("08"));
        assertThat(gv01.getCategorie().get(0).getElement().size(), is(3));
        assertThat(gv01.getCategorie().get(0).getElement().get(0).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(0).getElement().get(0).getValue(), is("PPPPPPPPPPPPPPPPPPPP"));
        assertThat(gv01.getCategorie().get(0).getElement().get(1).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(0).getElement().get(1).getValue(), is("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP"));
        assertThat(gv01.getCategorie().get(0).getElement().get(2).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(0).getElement().get(2).getValue(), is(""));
        assertThat(gv01.getCategorie().get(1).getNummer(), is("58"));
        assertThat(gv01.getCategorie().get(1).getElement().size(), is(3));
        assertThat(gv01.getCategorie().get(1).getElement().get(0).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(1).getElement().get(0).getValue(), is("PPPPPPPPPPPPPPPPPPPPP"));
        assertThat(gv01.getCategorie().get(1).getElement().get(1).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(1).getElement().get(1).getValue(), is("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP"));
        assertThat(gv01.getCategorie().get(1).getElement().get(2).getNummer(), is("PPPP"));
        assertThat(gv01.getCategorie().get(1).getElement().get(2).getValue(), is("O"));
    }

}
