
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

import nl.logius.digid.dws.client.RdwClient;
import nl.logius.digid.dws.client.SharedServiceClient;
import nl.logius.digid.dws.exception.PenRequestException;
import nl.logius.digid.dws.exception.SharedServiceClientException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.PenRequest;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.rdw.eid_wus_crb._1.PenAanvraagEIDResponse;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class PenRequestServiceTest {

    // test data
    private List<PenRequestStatus> mockStatusList = new ArrayList<>();
    private static final LocalDateTime TEST_TIME = LocalDateTime.of(2019, 1, 15, 12, 34);
    private String speedRequest;
    private String numberOfRequests;
    private PenRequest request;
    private PenRequestStatus status;

    @Mock
    private PenRequestRepository mockRepository;

    @Mock
    private SharedServiceClient ssMock;

    @Mock
    private PenAanvraagEIDResponse rdwResponseMock;

    private Clock clock = Clock.fixed((TEST_TIME)
        .toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    @Mock
    private RdwClient rdwClientMock;

    @InjectMocks
    private PenRequestService service;

    @BeforeEach
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(ssMock.getSSConfigInt(speedRequest)).thenReturn(1);
        Mockito.when(ssMock.getSSConfigInt(numberOfRequests)).thenReturn(3);
        ReflectionTestUtils.setField(service,"clock", clock);
        status = new PenRequestStatus();
        request = new PenRequest();
        ReflectionTestUtils.setField(request, "bsn", "PPPPPPPPP");
        ReflectionTestUtils.setField(request, "docType", DocumentType.DRIVING_LICENCE);
        ReflectionTestUtils.setField(request, "sequenceNo", "PPPPPPPPPPPP");
    }

    @Test
    public void firstPinRequestIsAllowed() throws PenRequestException, SharedServiceClientException {
        Map<String, String> result = service.penRequestAllowed(request);
        assertEquals("OK", result.get("status"));
    }

    @Test
    public void pinRequestTooSoonThrowsDWS1Exception() throws PenRequestException, SharedServiceClientException {
        // create a pinRequestStatus with a RequestDateTime in the repo
        status.setRequestDatetime(LocalDateTime.now(clock));
        mockStatusList.add(status);

        // return arraylist with one dummy pinrequest
        Mockito.when(mockRepository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(mockStatusList);

        Exception exception = assertThrows(PenRequestException.class, () -> {
            service.penRequestAllowed(request);
        });
        assertEquals("DWS1", exception.getMessage());
    }

    @Test
    public void penRequest23HoursAfterLastRequestThrowsDWS1Exception() throws PenRequestException, SharedServiceClientException{
        // create a previous penRequest with a RequestDateTime
        status.setRequestDatetime(TEST_TIME.minusHours(23));
        mockStatusList.add(status);

        // return arraylist with one dummy penrequest
        Mockito.when(mockRepository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(mockStatusList);

        Exception exception = assertThrows(PenRequestException.class, () -> {
            service.penRequestAllowed(request);
        });
        assertEquals("DWS1", exception.getMessage());
    }

    @Test
    public void penRequestTooOftenThrowsDWS2Exception() throws PenRequestException, SharedServiceClientException {
        // create three penRequests with a RequestDateTime, 24 hours apart
        PenRequestStatus firstStatus = new PenRequestStatus();
        firstStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 1, 00, 01));
        mockStatusList.add(firstStatus);

        PenRequestStatus secondStatus= new PenRequestStatus();
        secondStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 2, 00, 01));
        mockStatusList.add(secondStatus);

        PenRequestStatus thirdStatus= new PenRequestStatus();
        thirdStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 3, 00, 01));
        mockStatusList.add(thirdStatus);

        // return arraylist with one dummy penrequest
        Mockito.when(mockRepository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(mockStatusList);

        Exception exception = assertThrows(PenRequestException.class, () -> {
            service.penRequestAllowed(request);
        });
        assertEquals("DWS2", exception.getMessage());
    }

    @Test
    public void penRequestAllowedWithValidPreviousRequests() throws PenRequestException, SharedServiceClientException {

        // create three penRequests with a RequestDateTime, 24 hours apart
        PenRequestStatus firstStatus = new PenRequestStatus();
        firstStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 1, 00, 01));
        mockStatusList.add(firstStatus);

        PenRequestStatus secondStatus = new PenRequestStatus();
        secondStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 2, 00, 01));
        mockStatusList.add(secondStatus);

        PenRequestStatus thirdStatus = new PenRequestStatus();
        thirdStatus.setRequestDatetime(LocalDateTime.of(2018, 1, 3, 00, 01));
        mockStatusList.add(thirdStatus);

        // return arraylist with one dummy penrequest
        Mockito.when(mockRepository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(mockStatusList);

        Map<String, String> result = service.penRequestAllowed(request);
        Map<String, String> expectedMap  = new HashMap<String, String>() {{
            put("status", "OK");
        }};
        assertEquals(expectedMap, result);
    }

    @Test
    public void penRequestWithConfigurationsErrorThrowsDWS3Exception() throws PenRequestException, SharedServiceClientException, SoapValidationException {
        Mockito.when(ssMock.getSSConfigInt(speedRequest)).thenThrow(new SharedServiceClientException("DWS3"));
        mockStatusList.add(status);
        Mockito.when(mockRepository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo())).thenReturn(mockStatusList);

        Exception exception = assertThrows(SharedServiceClientException.class, () -> {
            service.requestPenReset(request);
        });
        assertEquals("DWS3", exception.getMessage());
    }
}
