
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
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

@Service
public class ScheduledTasksService {
    private static final String RE_CHECK_DOCUMENTS = "re_check_documents";

    private final IdCheckDocumentRepository idCheckDocumentRepository;
    private final DwsClient dwsClient;
    private final SharedServiceClient sharedServiceClient;
    private final DigidClient digidClient;

    public ScheduledTasksService(IdCheckDocumentRepository idCheckDocumentRepository, DwsClient dwsClient, SharedServiceClient sharedServiceClient, DigidClient digidClient) {
        this.idCheckDocumentRepository = idCheckDocumentRepository;
        this.dwsClient = dwsClient;
        this.sharedServiceClient = sharedServiceClient;
        this.digidClient = digidClient;
    }

    public void processScheduledTask(String name) throws SharedServiceClientException {
        if(RE_CHECK_DOCUMENTS.equals(name)) {
            var daysAgo = sharedServiceClient.getSSConfigInt("interval_lost_stolen_check");
            for (IdCheckDocument document : idCheckDocumentRepository.findAllWithCreationDateTimeBefore(ZonedDateTime.now().minusDays(daysAgo))) {
                var response = dwsClient.checkBvBsn(document.getDocumentType(), document.getDocumentNumber());

                if (response.get(STATUS).equals(NOK)) {
                    digidClient.remoteLog("1577", Map.of(
                        "document_type", document.getDocumentType(),
                        "document_number", document.getDocumentNumber(),
                        "user_app", document.getUserAppId(),
                        "account_id", document.getAccountId(),
                        "created_at", document.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        HIDDEN, true
                    ));
                }

                idCheckDocumentRepository.delete(document);
            }
        }
    }
}
