
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

package nl.logius.digid.dws.integration;

import https.digid_nl.schema.mu_pin_reset.DocTypeType;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetRequest;
import nl.logius.digid.dws.TestPinResetApplication;
import nl.logius.digid.dws.client.RdwClient;
import nl.logius.digid.dws.client.SharedServiceClient;
import nl.logius.digid.dws.exception.PenRequestException;
import nl.logius.digid.dws.exception.SharedServiceClientException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.PenRequest;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.logius.digid.dws.service.NotificationService;
import nl.logius.digid.dws.service.PenRequestService;
import nl.rdw.eid_wus_crb._1.PenAanvraagEIDRequest;
import nl.rdw.eid_wus_crb._1.PenAanvraagEIDResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={
        TestPinResetApplication.class})
@ActiveProfiles({ "default", "integration-test" })
public class PenRequestServiceIntegrationTest {

    private PenRequest request;

    private RegisterPinResetRequest registerPinResetRequest;

    @Autowired
    protected Flyway flyway;

    @Autowired
    protected PenRequestService service;

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected PenRequestRepository repository;

    @MockBean
    private RdwClient mockRdwClient;

    @MockBean
    private SharedServiceClient mockSsClient;

    @BeforeEach
    public void init() throws Exception {
        flyway.clean();
        flyway.migrate();

        request = new PenRequest();
        ReflectionTestUtils.setField(request, "bsn", "PPPPPPPPP");
        ReflectionTestUtils.setField(request, "docType", DocumentType.DRIVING_LICENCE);
        ReflectionTestUtils.setField(request, "sequenceNo", "PPPPPPPPPPPP");

        registerPinResetRequest = new RegisterPinResetRequest();
        registerPinResetRequest.setBSN("PPPPPPPPP");
        registerPinResetRequest.setDocType(DocTypeType.NL_RIJBEWIJS); // "NL-rijbewijs" ??
        registerPinResetRequest.setSequenceNr("PPPPPPPPPPPP");
        LocalDateTime date = LocalDateTime.of(2019,4,21,8,34);
        GregorianCalendar gcal = GregorianCalendar.from(date.atZone(ZoneId.systemDefault()));
        XMLGregorianCalendar xcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        registerPinResetRequest.setRequestDateTime(xcal);
    }

    @Test
    public void requestPenResetIsSavedToDb() throws PenRequestException, SharedServiceClientException, SoapValidationException {

        PenAanvraagEIDResponse response = new PenAanvraagEIDResponse();
        response.setResponseDescription("OK");
        Mockito.when(mockRdwClient.penRequest(Mockito.any(PenAanvraagEIDRequest.class))).thenReturn(response);

        Mockito.when(mockSsClient.getSSConfigInt("snelheid_aanvragen")).thenReturn(1);
        Mockito.when(mockSsClient.getSSConfigInt("blokkering_aanvragen")).thenReturn(3);

        service.requestPenReset(request);
        repository.findByBsnAndDocTypeAndSequenceNo("PPPPPPPPP", DocumentType.DRIVING_LICENCE, "PPPPPPPPPPPP");
        assertEquals(1, repository.count(), "One record has been created"); // There is a new record created in the DB
    }

    @Test
    public void requestPenResetRdwBadResponse() throws PenRequestException, SharedServiceClientException, SoapValidationException {
        PenAanvraagEIDResponse response = new PenAanvraagEIDResponse();
        response.setResponseDescription("oK");
        Mockito.when(mockRdwClient.penRequest(Mockito.any(PenAanvraagEIDRequest.class))).thenReturn(response);

        Mockito.when(mockSsClient.getSSConfigInt("snelheid_aanvragen")).thenReturn(1);
        Mockito.when(mockSsClient.getSSConfigInt("blokkering_aanvragen")).thenReturn(3);

        Exception exception = assertThrows(PenRequestException.class, () -> {
            service.requestPenReset(request);
        });
        assertEquals("DWS10", exception.getMessage());
    }

    @Test
    public void registerPaidPinResetFromRdw() throws Exception {
        notificationService.updateStatus(registerPinResetRequest);
        PenRequestStatus latestPenRequestStatus = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc("PPPPPPPPP", DocumentType.DRIVING_LICENCE,"PPPPPPPPPPPP" );
        assertEquals(LocalDateTime.of(2019,4,21,8,34), latestPenRequestStatus.getPinResetValidDate());
        assertEquals(1, repository.count(), "One record has been created"); // There is a new record created in the DB
    }

    @Test
    public void registerPinResetFromRdwWithExistingRecords() throws Exception {
        PenRequestStatus existingPenRequestStatus = new PenRequestStatus();
        existingPenRequestStatus.setBsn("PPPPPPPPP");
        existingPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        existingPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        existingPenRequestStatus.setRequestDatetime(LocalDateTime.now());
        repository.save(existingPenRequestStatus);

        notificationService.updateStatus(registerPinResetRequest);
        PenRequestStatus latestPenRequestStatus = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc("PPPPPPPPP", DocumentType.DRIVING_LICENCE,"PPPPPPPPPPPP");
        assertEquals(LocalDateTime.of(2019,4,21,8,34), latestPenRequestStatus.getPinResetValidDate());
        assertEquals(1, repository.count(), "One record has been updated"); // An existing record is updated in the DB
    }

    @Test
    public void registerPinResetFromRdwUpdateLatestRecord() throws Exception {
        // first record
        PenRequestStatus firstPenRequestStatus = new PenRequestStatus();
        firstPenRequestStatus.setBsn("PPPPPPPPP");
        firstPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        firstPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        firstPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 1, 12, 00));
        repository.save(firstPenRequestStatus);

        //second record
        PenRequestStatus secondPenRequestStatus = new PenRequestStatus();
        secondPenRequestStatus.setBsn("PPPPPPPPP");
        secondPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        secondPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        secondPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 2, 12, 00));
        repository.save(secondPenRequestStatus);

        // third record
        PenRequestStatus thirdPenRequestStatus = new PenRequestStatus();
        thirdPenRequestStatus.setBsn("PPPPPPPPP");
        thirdPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        thirdPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        thirdPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 3, 12, 00));
        repository.save(thirdPenRequestStatus);

        notificationService.updateStatus(registerPinResetRequest);
        PenRequestStatus latestPenRequestStatus = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc("PPPPPPPPP", DocumentType.DRIVING_LICENCE,"PPPPPPPPPPPP");
        assertEquals(LocalDateTime.of(2019, 1, 3, 12, 00), latestPenRequestStatus.getRequestDatetime());
        assertEquals(LocalDateTime.of(2019,4,21,8,34), latestPenRequestStatus.getPinResetValidDate());
        assertEquals(3, repository.count(), "One record has been updated"); // An existing record is updated in the DB
    }
}
