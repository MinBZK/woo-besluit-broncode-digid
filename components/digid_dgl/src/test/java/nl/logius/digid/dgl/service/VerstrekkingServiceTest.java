
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
public class VerstrekkingServiceTest {

    @InjectMocks
    private VerstrekkingService classUnderTest;

    @Mock
    private AfnemersberichtRepository afnemersberichtRepository;

    @Mock
    private Afnemersbericht afnemersbericht;

    @Mock
    private VerstrekkingAanAfnemer verstrekkingAanAfnemer;

    @Mock
    private DglResponseService dglResponseService;

    @Mock
    private DigidXClient digidXClient;

    @Test
    public void testProcessNullMessage(){
        Null testNullMessage = TestDglMessagesUtil.createTestNullMessage();
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setNull(testNullMessage);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Null");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(afnemersbericht);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processNullMessage(testNullMessage, afnemersbericht);
        verify(digidXClient, times(1)).remoteLogWithoutRelatingToAccount(Log.MESSAGE_PROCESSED, "Null");
    }

    @Test
    public void testProcessAg01(){
        String testBsn = "SSSSSSSSS";
        Ag01 testAg01 = TestDglMessagesUtil.createTestAg01(testBsn, "A", "SSSSSSSS");
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAg01(testAg01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Ag01");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(afnemersbericht);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);
        when(afnemersbericht.getBsn()).thenReturn(testBsn);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processAg01(testAg01, afnemersbericht);
        verify(digidXClient, times(1)).remoteLogBericht(Log.MESSAGE_PROCESSED, verstrekkingAanAfnemer, afnemersbericht);
    }

    @Test
    public void testProcessAg01NoMatch(){
        String testBsn = "SSSSSSSSS";
        Ag01 testAg01 = TestDglMessagesUtil.createTestAg01(testBsn, "A", "SSSSSSSS");
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAg01(testAg01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Ag01");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(null);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(digidXClient, times(1)).remoteLogBericht(Log.NO_RELATION_TO_SENT_MESSAGE, verstrekkingAanAfnemer, null);
    }

    @Test
    public void testProcessAg31(){
        String testBsn = "SSSSSSSSS";
        Ag31 testAg31 = TestDglMessagesUtil.createTestAg31(testBsn,"O", "SSSSSSSS");
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAg31(testAg31);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Ag31");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(afnemersbericht);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processAg31(testAg31, afnemersbericht);
    }

    @Test
    public void testProcessAf01(){
        String testBsn = "SSSSSSSSS";
        Af01 testAf01 = TestDglMessagesUtil.createTestAf01(testBsn);
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAf01(testAf01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Af01");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(afnemersbericht);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);
        when(afnemersbericht.getBsn()).thenReturn(testBsn);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processAf01(testAf01, afnemersbericht);
    }

    @Test
    public void testProcessAf11(){
        String testANummer = "SSSSSSSSSS";
        Af11 testAf11 = TestDglMessagesUtil.createTestAf11(testANummer);
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setAf11(testAf11);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Af11");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn("referentieId");
        when(afnemersberichtRepository.findByOnzeReferentie("referentieId")).thenReturn(afnemersbericht);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);
        when(afnemersbericht.getANummer()).thenReturn(testANummer);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processAf11(testAf11, afnemersbericht);
    }

    @Test
    public void testProcessGv01(){
        String testAnummer = "SSSSSSSSSS";
        String testBsnOud = "SSSSSSSSS";
        String testBsnNieuw = "SSSSSSSSS";
        Gv01 testGv01 = TestDglMessagesUtil.createTestGv01(testAnummer, "O", testBsnOud, testBsnNieuw);
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setGv01(testGv01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Gv01");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn(null);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processGv01(testGv01);
    }

    @Test
    public void testProcessNg01(){
        String testAnummer = "SSSSSSSSSS";
        String testDatumOpschorting = "SSSSSSSS";
        Ng01 testNg01 = TestDglMessagesUtil.createTestNg01(testAnummer, "F", testDatumOpschorting);
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setNg01(testNg01);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Ng01");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn(null);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processNg01(testNg01);
        verify(digidXClient, times(1)).remoteLogSpontaneVerstrekking(Log.MESSAGE_PROCESSED, "Ng01", testAnummer, "");
    }

    @Test
    public void testProcessWa11(){
        String testAnummer = "SSSSSSSSSS";
        String testNieuwAnummer = "SSSSSSSSSS";
        String datumGeldigheid = "SSSSSSSS";
        Wa11 testWa11 = TestDglMessagesUtil.createTestWa11(testAnummer, testNieuwAnummer, datumGeldigheid);
        VerstrekkingInhoudType inhoudType = new VerstrekkingInhoudType();
        inhoudType.setWa11(testWa11);
        GeversioneerdType type = new GeversioneerdType();
        type.setNaam("Wa11");

        when(verstrekkingAanAfnemer.getReferentieId()).thenReturn(null);
        when(verstrekkingAanAfnemer.getGebeurtenissoort()).thenReturn(type);
        when(verstrekkingAanAfnemer.getGebeurtenisinhoud()).thenReturn(inhoudType);

        classUnderTest.processVerstrekkingAanAfnemer(verstrekkingAanAfnemer);

        verify(dglResponseService, times(1)).processWa11(testWa11);
    }
}
