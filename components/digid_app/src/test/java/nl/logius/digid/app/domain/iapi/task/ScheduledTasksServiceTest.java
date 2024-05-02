
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

package nl.logius.digid.app.domain.iapi.task;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.iapi.rda.IdCheckDocument;
import nl.logius.digid.app.domain.iapi.rda.IdCheckDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "integration-test" })
class ScheduledTasksServiceTest {

    @MockBean
    private IdCheckDocumentRepository idCheckDocumentRepository;

    @MockBean
    private DwsClient dwsClient;

    @MockBean
    private SharedServiceClient sharedServiceClient;

    @MockBean
    private DigidClient digidClient;

    private ScheduledTasksService service;
    private IdCheckDocument idCheckDocument;

    @BeforeEach
    public void setup(){
        service = new ScheduledTasksService(idCheckDocumentRepository, dwsClient,sharedServiceClient, digidClient);

        idCheckDocument = new IdCheckDocument();
        idCheckDocument.setUserAppId("user_app_id");
        idCheckDocument.setDocumentNumber("document_number");
        idCheckDocument.setDocumentType("document_type");
        idCheckDocument.setAccountId(1L);
        idCheckDocument.setCreatedAt(ZonedDateTime.parse("2022-10-07T13:59:30.018540+02:00[Europe/Amsterdam]"));

        when(idCheckDocumentRepository.findAllWithCreationDateTimeBefore(any())).thenReturn(List.of(idCheckDocument));
    }

    @Test
    void processesScheduledTaskStolenDocument() throws SharedServiceClientException {
        when(dwsClient.checkBvBsn("document_type", "document_number")).thenReturn(Map.of("status", "NOK"));
        when(sharedServiceClient.getSSConfigInt("interval_lost_stolen_check")).thenReturn(7);

        service.processScheduledTask("re_check_documents");

        verify(digidClient, times(1)).remoteLog("1577", Map.of("document_type", "document_type", "document_number", "document_number","user_app", "user_app_id", "account_id", 1L, "created_at", "2022-10-07", "hidden", true));
    }

    @Test
    void processesScheduledTaskValidDocument() throws SharedServiceClientException {
        when(dwsClient.checkBvBsn("document_type", "document_number")).thenReturn(Map.of("status", "OK"));
        when(sharedServiceClient.getSSConfigInt("interval_lost_stolen_check")).thenReturn(7);

        service.processScheduledTask("re_check_documents");

        verify(digidClient, times(0)).remoteLog("1577", Map.of("document_type", "document_type", "document_number", "document_number","user_app", "user_app_id", "account_id", 1L, "created_at", "2022-10-07", "hidden", true));
    }
}
