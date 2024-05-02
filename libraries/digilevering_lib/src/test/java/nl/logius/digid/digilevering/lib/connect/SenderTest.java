
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

import nl.logius.digid.digilevering.api.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.messaging.MessageHeaders;

import javax.jms.JMSException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.digilevering.lib.TestUtil.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class SenderTest {

    @Mock
    JmsTemplate jmsTemplateMock;

    @InjectMocks
    private Sender<AfnemersberichtAanDGL> afnemersberichtAanDGLSender = new Sender<>(){};

    @InjectMocks
    private Sender<VerstrekkingAanAfnemer> verstrekkingAanAfnemerSender = new Sender<>(){};

    @Test
    public void testMarshallingAfnemersberichtAanDGL() throws IOException {
        AfnemersberichtAanDGL afnemersberichtAanDGL = new AfnemersberichtAanDGL();

        BerichtHeaderType berichtheader = new BerichtHeaderType();
        berichtheader.setOntvangerId("55555555555555555555");
        berichtheader.setVerstrekkerId("77777777777777777777");
        berichtheader.setDatumtijdstempelVerstrekker(parseTime("2020-08-16T11:12:42.566+02:00"));
        berichtheader.setKenmerkVerstrekker("kenmerk.verstrekker.1");
        berichtheader.setBerichtversie("1.0");
        afnemersberichtAanDGL.setBerichtHeader(berichtheader);
        AfnemersberichtAanDGL.Stuurgegevens stuurgegevens = new AfnemersberichtAanDGL.Stuurgegevens();
        stuurgegevens.setVersieBerichttype("3.10");
        VersiebeheerType berichtsoort = new VersiebeheerType();
        berichtsoort.setNaam("Ap01");
        berichtsoort.setVersie("1.0");
        stuurgegevens.setBerichtsoort(berichtsoort);
        afnemersberichtAanDGL.setStuurgegevens(stuurgegevens);
        AfnemersInhoudType inhoud = new AfnemersInhoudType();
        Ap01 ap01 = new Ap01();
        ap01.setRandomKey("00000000");
        Container categorie = new Container();
        categorie.setNummer("01");
        Element element = new Element();
        element.setNummer("0120");
        element.setValue("PPPPPPPPP");
        categorie.getElement().add(element);
        ap01.getCategorie().add(categorie);
        inhoud.setAp01(ap01);
        afnemersberichtAanDGL.setInhoud(inhoud);

        Path xmlFile = Paths.get("src","test","resources", "afnemersberichtAanDGL.xml");
        String xmlMessage = Files.readString(xmlFile);

        afnemersberichtAanDGLSender.sendMessage(afnemersberichtAanDGL, new MessageHeaders(new HashMap<>()));
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsTemplateMock).convertAndSend(nullable(String.class), stringArgumentCaptor.capture(), any(MessagePostProcessor.class));
        String sentXML = stringArgumentCaptor.getValue();

        assertThat(sentXML, is(xmlMessage));
    }

    @Test
    public void testMarshallingVerstrekkingAanAfnemer() throws IOException {
        VerstrekkingAanAfnemer verstrekkingAanAfnemer = new VerstrekkingAanAfnemer();

        verstrekkingAanAfnemer.setDatumtijdstempelDigilevering(parseTime("2018-02-02T11:59:04.170+01:00"));
        verstrekkingAanAfnemer.setDatumtijdstempelLV(parseTime("2017-11-27T14:33:05.010+01:00"));
        verstrekkingAanAfnemer.setKenmerkDigilevering("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        verstrekkingAanAfnemer.setKenmerkLV("SSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        verstrekkingAanAfnemer.setVersieBerichttype("3.10");
        GeversioneerdType abonnement = new GeversioneerdType();
        abonnement.setNaam("");
        abonnement.setVersie("");
        verstrekkingAanAfnemer.setAbonnement(abonnement);
        verstrekkingAanAfnemer.setBasisregistratie("BRP");
        GeversioneerdType gebeurtenissoort = new GeversioneerdType();
        gebeurtenissoort.setNaam("Gv01");
        gebeurtenissoort.setVersie("1.0");
        verstrekkingAanAfnemer.setGebeurtenissoort(gebeurtenissoort);

        VerstrekkingInhoudType inhoud = new VerstrekkingInhoudType();

        Gv01 gv01 = new Gv01();
        gv01.setANummer("SSSSSSSSSS");
        gv01.setRandomKey("00000000");

        Container categorie08 = new Container();
        categorie08.setNummer("08");
        Element element081110 = new Element();
        element081110.setNummer("PPPP");
        element081110.setValue("PPPPPPPPPPPPPPPPPPPP");
        categorie08.getElement().add(element081110);
        Element element081115 = new Element();
        element081115.setNummer("PPPP");
        element081115.setValue("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        categorie08.getElement().add(element081115);
        Element element088410 = new Element();
        element088410.setNummer("PPPP");
        element088410.setValue("");
        categorie08.getElement().add(element088410);
        gv01.getCategorie().add(categorie08);

        Container categorie58 = new Container();
        categorie58.setNummer("58");
        Element element581110 = new Element();
        element581110.setNummer("PPPP");
        element581110.setValue("PPPPPPPPPPPPPPPPPPPPP");
        categorie58.getElement().add(element581110);
        Element element581115 = new Element();
        element581115.setNummer("PPPP");
        element581115.setValue("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        categorie58.getElement().add(element581115);
        Element element588410 = new Element();
        element588410.setNummer("PPPP");
        element588410.setValue("O");
        categorie58.getElement().add(element588410);
        gv01.getCategorie().add(categorie58);

        inhoud.setGv01(gv01);
        verstrekkingAanAfnemer.setGebeurtenisinhoud(inhoud);

        Path xmlFile = Paths.get("src","test","resources", "verstrekkingAanAfnemer.xml");
        String xmlMessage = Files.readString(xmlFile);

        verstrekkingAanAfnemerSender.sendMessage(verstrekkingAanAfnemer, new MessageHeaders(new HashMap<>()));
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsTemplateMock).convertAndSend(nullable(String.class), stringArgumentCaptor.capture(), any(MessagePostProcessor.class));
        String sentXML = stringArgumentCaptor.getValue();

        assertThat(sentXML, is(xmlMessage));
    }

    @Test
    public void testMessagePostProcessingHeaders() throws JMSException {
        AfnemersberichtAanDGL afnemersberichtAanDGL = new AfnemersberichtAanDGL();

        Map<String, Object> headers = new HashMap<>();
        headers.put("header1", "header1 value");
        headers.put("header2", "header2 value");
        MessageHeaders messageHeaders = new MessageHeaders(headers);

        afnemersberichtAanDGLSender.sendMessage(afnemersberichtAanDGL, messageHeaders);

        ArgumentCaptor<MessagePostProcessor> messagePostProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        Mockito.verify(jmsTemplateMock).convertAndSend(nullable(String.class), nullable(String.class), messagePostProcessorCaptor.capture());

        MessagePostProcessor value = messagePostProcessorCaptor.getValue();
        javax.jms.Message mockMessage = Mockito.mock(javax.jms.Message.class);
        value.postProcessMessage(mockMessage);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockMessage, times(4)).setStringProperty(keyCaptor.capture(), valueCaptor.capture());

        assertThat(keyCaptor.getAllValues().get(1), is("header1"));
        assertThat(valueCaptor.getAllValues().get(1), is("header1 value"));
        assertThat(keyCaptor.getAllValues().get(0), is("header2"));
        assertThat(valueCaptor.getAllValues().get(0), is("header2 value"));

    }
}
