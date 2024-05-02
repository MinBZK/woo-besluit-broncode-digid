
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

import nl.logius.digid.dgl.client.DigidXClient;
import nl.logius.digid.dgl.client.Log;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.dgl.util.TestDglMessagesUtil;
import nl.logius.digid.digilevering.api.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DglResponseServiceTest {

    @InjectMocks
    private DglResponseService classUnderTest;

    @Mock
    private AfnemersberichtRepository afnemersberichtRepository;

    @Mock
    private Afnemersbericht afnemersbericht;

    @Mock
    private DigidXClient digidXClient;

    @Test
    public void testProcessNullMessageAv01(){
        Null testNullMessage = TestDglMessagesUtil.createTestNullMessage();

        when(afnemersbericht.getType()).thenReturn(Afnemersbericht.Type.Av01);

        classUnderTest.processNullMessage(testNullMessage, afnemersbericht);

        verify(afnemersberichtRepository, times(1)).delete(afnemersbericht);
    }

    @Test
    public void testProcessNullMessageNotAv01(){
        Null testNullMessage = TestDglMessagesUtil.createTestNullMessage();

        when(afnemersbericht.getType()).thenReturn(Afnemersbericht.Type.Ap01);

        classUnderTest.processNullMessage(testNullMessage, afnemersbericht);

        verify(afnemersberichtRepository, times(0)).delete(afnemersbericht);
    }

    @Test
    public void testProcessAg01(){
        String testBsn = "SSSSSSSSS";
        Ag01 testAg01 = TestDglMessagesUtil.createTestAg01(testBsn, "A", "SSSSSSSS");

        when(afnemersbericht.getBsn()).thenReturn(testBsn);

        classUnderTest.processAg01(testAg01, afnemersbericht);

        verify(afnemersberichtRepository, times(1)).delete(afnemersbericht);
        verify(digidXClient, times(1)).setANummer(testBsn,"A" + testBsn);
        verify(digidXClient, times(1)).setOpschortingsStatus("A" + testBsn, "A");
    }

    @Test
    public void testProcessAg31NotRelatedToRequest(){
        String testBsn = "SSSSSSSSS";
        Ag31 testAg31 = TestDglMessagesUtil.createTestAg31(testBsn, "O", "SSSSSSSS");

        classUnderTest.processAg31(testAg31, null);

        verify(afnemersberichtRepository, times(0)).delete(afnemersbericht);
        verify(digidXClient, times(1)).setANummer(testBsn,"A" + testBsn);
        verify(digidXClient, times(1)).setOpschortingsStatus("A" + testBsn, "O");
    }

    @Test
    public void testProcessAg31StatusA(){
        String testBsn = "SSSSSSSSS";
        Ag31 testAg31 = TestDglMessagesUtil.createTestAg31(testBsn, "A", "SSSSSSSS");

        classUnderTest.processAg31(testAg31, afnemersbericht);

        verify(digidXClient, times(1)).setANummer(testBsn,"A" + testBsn);
        verify(digidXClient, times(1)).setOpschortingsStatus("A" + testBsn, "A");
    }

    @Test
    public void testProcessAg31RelatedToRequest(){
        String testBsn = "SSSSSSSSS";
        Ag31 testAg31 = TestDglMessagesUtil.createTestAg31(testBsn,"O", "SSSSSSSS");

        classUnderTest.processAg31(testAg31, afnemersbericht);

        verify(afnemersberichtRepository, times(1)).delete(afnemersbericht);
        verify(digidXClient, times(1)).setANummer(testBsn,"A" + testBsn);
        verify(digidXClient, times(1)).setOpschortingsStatus("A" + testBsn, "O");
    }

    @Test
    public void testProcessAf01(){
        String testBsn = "SSSSSSSSS";
        Af01 testAf01 = TestDglMessagesUtil.createTestAf01(testBsn);

        classUnderTest.processAf01(testAf01, afnemersbericht);

        verify(afnemersberichtRepository, times(1)).delete(afnemersbericht);
    }

    @Test
    public void testProcessAf11(){
        String testANummer = "SSSSSSSSSS";
        Af11 testAf11 = TestDglMessagesUtil.createTestAf11(testANummer);

        classUnderTest.processAf11(testAf11, afnemersbericht);

        verify(afnemersberichtRepository, times(1)).save(afnemersbericht);
    }

    @Test
    public void testProcessGv01() {
        String testAnummer = "SSSSSSSSSS";
        String testBsnOud = "SSSSSSSSS";
        String testBsnNieuw = "SSSSSSSSS";
        Gv01 testGv01 = TestDglMessagesUtil.createTestGv01(testAnummer, "O", testBsnOud, testBsnNieuw);

        classUnderTest.processGv01(testGv01);

        verify(digidXClient, times(1)).setOpschortingsStatus(testAnummer, "O");
        verify(digidXClient, times(1)).remoteLogSpontaneVerstrekking(Log.BSN_CHANGED, "Gv01", testAnummer, testBsnOud);
    }

    @Test
    public void testProcessNg01() {
        String testAnummer = "SSSSSSSSSS";
        String testDatumOpschorting = "SSSSSSSS";
        Ng01 testNg01 = TestDglMessagesUtil.createTestNg01(testAnummer, "F", testDatumOpschorting);

        classUnderTest.processNg01(testNg01);

        verify(digidXClient, times(1)).setOpschortingsStatus(testAnummer, "F");
    }

    @Test
    public void testProcessWa11() {
        String testAnummer = "SSSSSSSSSS";
        String testNieuwAnummer = "SSSSSSSSSS";
        String datumGeldigheid = "SSSSSSSS";
        Wa11 testWa11 = TestDglMessagesUtil.createTestWa11(testAnummer, testNieuwAnummer, datumGeldigheid);

        classUnderTest.processWa11(testWa11);

        verify(digidXClient, times(1)).updateANummer(testAnummer, testNieuwAnummer);

        classUnderTest.processWa11(testWa11);

        verify(digidXClient, times(2)).updateANummer(testAnummer, testNieuwAnummer);
    }
}
