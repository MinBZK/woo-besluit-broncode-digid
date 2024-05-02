
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

package nl.logius.digid.dgl.client;

import nl.logius.digid.dgl.controller.AfnemersberichtAanDGLFactory;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.util.TestDglMessagesUtil;
import nl.logius.digid.digilevering.api.model.*;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DigiDXClientTest {

    private String key = "test";

    @Mock
    private Afnemersbericht afnemersbericht;

    @Mock
    private VerstrekkingAanAfnemer verstrekkingAanAfnemer;

    @Spy
    private DigidXClient classUnderTest = new DigidXClient(HttpUrl.get("SSSSSSSSSSSSSSSSS"), "iapiToken", 100);

    @Test
    void testRemoteLogBerichtWithAfnemersBericht() {
        AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory = new AfnemersberichtAanDGLFactory("oin1", "oin2");
        AfnemersberichtAanDGL afnemersberichtAanDGL = afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(TestDglMessagesUtil.createTestAp01("bsn"));

        classUnderTest.remoteLogBericht(key, afnemersberichtAanDGL);
        verify(classUnderTest, times(1)).remoteLog(isA(String.class), isA(HashMap.class), isA(HashMap.class));
    }

    @Test
    void testRemoteLogBerichtWithAg01() {
        String testBsn = "SSSSSSSSS";
        Ag01 testAg01 = TestDglMessagesUtil.createTestAg01(testBsn, "A", "SSSSSSSS");
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAg01(testAg01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Ag01");
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.remoteLogBericht(key, verstrekkingAanAfnemer, afnemersbericht);

        verify(afnemersbericht, times(0)).getANummer();
        verify(afnemersbericht, times(0)).getBsn();
        verify(classUnderTest, times(1)).remoteLog(isA(String.class), isA(HashMap.class), isA(HashMap.class));
    }

    @Test
    public void testRemoteLogSpontaneVerstrekking() {
        String aNummer = "SSSSSSSSSS";
        String bsn = "SSSSSSSSS";
        classUnderTest.remoteLogSpontaneVerstrekking(key, "Gv01", aNummer, bsn);
        verify(classUnderTest, times(1)).remoteLog(isA(String.class), isA(HashMap.class), isA(HashMap.class));
    }

    @Test
    public void testRemoteLogWithoutRelatingToAccount() {
        String messageType = "Gv01";
        classUnderTest.remoteLogWithoutRelatingToAccount(key, messageType);
        verify(classUnderTest, times(1)).remoteLog(isA(String.class), isA(HashMap.class), isA(HashMap.class));
    }
}
