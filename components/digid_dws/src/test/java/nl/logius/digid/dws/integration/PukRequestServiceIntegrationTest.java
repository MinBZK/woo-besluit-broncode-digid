
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

import nl.logius.digid.dws.TestPinResetApplication;
import nl.logius.digid.dws.exception.PukRequestException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.model.PukRequest;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.logius.digid.dws.service.PukRequestService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={
        TestPinResetApplication.class})
@ActiveProfiles({ "default", "integration-test" })
public class PukRequestServiceIntegrationTest {

    private PukRequest request;

    @Autowired
    protected Flyway flyway;

    @Autowired
    protected PukRequestService service;

    @Autowired
    protected PenRequestRepository repository;

    @BeforeEach
    public void init() {
        flyway.clean();
        flyway.migrate();

        request = new PukRequest();
        ReflectionTestUtils.setField(request, "bsn", "PPPPPPPPP");
        ReflectionTestUtils.setField(request, "docType", DocumentType.DRIVING_LICENCE);
        ReflectionTestUtils.setField(request, "sequenceNo", "PPPPPPPPPPPP");
    }

    @Test
    public void pinResetCompletedDeletingAllPenRequest() throws PukRequestException {
        //first record
        PenRequestStatus firstPenRequestStatus = new PenRequestStatus();
        firstPenRequestStatus.setBsn("PPPPPPPPP");
        firstPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        firstPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        firstPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 1, 12, 00));

        //second record
        PenRequestStatus secondPenRequestStatus = new PenRequestStatus();
        secondPenRequestStatus.setBsn("PPPPPPPPP");
        secondPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        secondPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        secondPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 2, 12, 00));

        // third record
        PenRequestStatus thirdPenRequestStatus = new PenRequestStatus();
        thirdPenRequestStatus.setBsn("PPPPPPPPP");
        thirdPenRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
        thirdPenRequestStatus.setSequenceNo("PPPPPPPPPPPP");
        thirdPenRequestStatus.setRequestDatetime(LocalDateTime.of(2019, 1, 3, 12, 00));

        List<PenRequestStatus> penRequests = Arrays.asList(firstPenRequestStatus, secondPenRequestStatus, thirdPenRequestStatus);
        repository.saveAll(penRequests);
        assertEquals(repository.count(), 3, "3 records are created ");

        service.pinResetCompleted(request);
        assertEquals(repository.count(), 0, "All records are deleted");
    }

    @Test
    public void pinResetCompletedErrorWhenNoPenrequestFound() throws PukRequestException {
        repository.deleteAll();
        assertThrows(PukRequestException.class, () -> {
            service.pinResetCompleted(request);
        });
    }
}
