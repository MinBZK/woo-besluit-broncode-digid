
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

package nl.logius.digid.dws.service;

import nl.logius.digid.dws.client.DigidXClient;
import nl.logius.digid.dws.client.RdwClient;
import nl.logius.digid.dws.client.SharedServiceClient;
import nl.logius.digid.dws.exception.PukRequestException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.model.PukRequest;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.rdw.eid_wus_crb._1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class PukRequestServiceTest {

    // test data
    private static final LocalDateTime TEST_TIME = LocalDateTime.of(2019, 1, 22, 12, 34);
    private PukRequest request;
    private PenRequestStatus status;

    @Mock
    private PenRequestRepository mockRepository;

    @Mock
    private SharedServiceClient ssMock;

    private Clock clock = Clock.fixed((TEST_TIME)
        .toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    @Mock
    RdwClient mockRdwClient;

    @Mock
    DigidXClient mockDigidXClient;

    @InjectMocks
    private PukRequestService service;

    @BeforeEach
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(service,"clock", clock);
        status = new PenRequestStatus();
        request = new PukRequest();
        ReflectionTestUtils.setField(request, "bsn", "PPPPPPPPP");
        ReflectionTestUtils.setField(request, "docType", DocumentType.DRIVING_LICENCE);
        ReflectionTestUtils.setField(request, "sequenceNo", "PPPPPPPPPPPP");
        ReflectionTestUtils.setField(service, "validDaysPen", 21);
        Mockito.when(mockRepository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(status);
    }

    @Test
    public void rdwResponseContainsWrongSequenceNoThrowsError() throws PukRequestException, SoapValidationException {
        // set valid date of penrequest in repo
        status.setPinResetValidDate(LocalDateTime.of(2019, 1, 2, 12, 33));

        OpvragenPUKCodeEIDResponse rdwResponse = buildRdwResponse("SSSSSSSSSSSS");

        Mockito.when(mockRdwClient.pukRequest(Mockito.any(OpvragenPUKCodeEIDRequest.class))).thenReturn(rdwResponse);

        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.requestPuk(request);
        });
        assertEquals("DWS8", exception.getMessage());
    }

    @Test
    public void pukRequestAllowedIsAllowedWithin21Days() throws PukRequestException {

        // set valid date of penrequest in repo
        status.setPinResetValidDate(LocalDateTime.of(2019, 1, 2, 12, 33));

        Map<String, String> result = service.pukRequestAllowed(request);
        assertEquals("OK", result.get("status"));
    }

    @Test
    public void pukRequestAllowedIsNotAllowedAfter22Days() throws PukRequestException {
        status.setPinResetValidDate(LocalDateTime.of(2019, 1, 1, 12, 34));

        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.pukRequestAllowed(request);
        });
        assertEquals("DWS5", exception.getMessage());
    }

    @Test
    public void pukRequestAllowedOnlyPossibleWHenPenRequestHasValidDate() throws PukRequestException {
        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.pukRequestAllowed(request);
        });
        assertEquals("DWS7", exception.getMessage());
    }

    @Test
    public void pukRequestAllowedOnlyPossibleAfterPenRequest() throws PukRequestException {
        Mockito.when(mockRepository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(null);

        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.pukRequestAllowed(request);
        });
        assertEquals("DWS6", exception.getMessage());
    }

    @Test
    public void pukRequestIsAllowedWithin21Days() throws PukRequestException, SoapValidationException {

        // set valid date of penrequest in repo
        status.setPinResetValidDate(LocalDateTime.of(2019, 1, 2, 12, 33));

        OpvragenPUKCodeEIDResponse rdwResponse = buildRdwResponse("PPPPPPPPPPPP");

        Mockito.when(mockRdwClient.pukRequest(Mockito.any(OpvragenPUKCodeEIDRequest.class))).thenReturn(rdwResponse);

        Map<String, String> result = service.requestPuk(request);
        assertEquals("OK", result.get("status"));
    }

    @Test
    public void pukRequestIsNotAllowedAfter22Days(){
        status.setPinResetValidDate(LocalDateTime.of(2019, 1, 1, 12, 34));

        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.requestPuk(request);
        });
        assertEquals("DWS5", exception.getMessage());
    }

    @Test
    public void pukRequestOnlyPossibleWHenPenRequestHasValidDate(){
        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.requestPuk(request);
        });
        assertEquals("DWS7", exception.getMessage());
    }

    @Test
    public void pukRequestOnlyPossibleAfterPenRequest(){
        Mockito.when(mockRepository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(null);

        Exception exception = assertThrows(PukRequestException.class, () -> {
            service.requestPuk(request);
        });
        assertEquals("DWS6", exception.getMessage());
    }

    private OpvragenPUKCodeEIDResponse buildRdwResponse(String sequenceNo) {
        EIDSTATUSGEG eidstatusgeg = new EIDSTATUSGEG();
        eidstatusgeg.setEIDVOLGNR(new BigInteger(sequenceNo));
        byte[] byteArray = new byte[2];
        eidstatusgeg.setCRYPTRDEPUK(byteArray);
        EIDSTATUSTAB eidstatustab = new EIDSTATUSTAB();
        List<EIDSTATUSGEG> eidstatusgegs = eidstatustab.getEIDSTATUSGEG();
        eidstatusgegs.add(eidstatusgeg);
        OpvragenPUKCodeEIDResponse rdwResponse = new OpvragenPUKCodeEIDResponse();
        rdwResponse.setEIDSTATINFO(new EIDSTATINFO());
        rdwResponse.getEIDSTATINFO().setEIDSTATUSTAB(eidstatustab);
        return rdwResponse;
    }
}
