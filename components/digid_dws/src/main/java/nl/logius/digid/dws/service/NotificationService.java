
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

import https.digid_nl.schema.mu_pin_reset.RegisterPinResetRequest;
import nl.logius.digid.dws.client.DigidXClient;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.repository.PenRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private PenRequestRepository repository;

    @Autowired
    private DigidXClient digidXClient;

    public void updateStatus(RegisterPinResetRequest request) throws Exception {
        final PenRequestStatus latestPenRequestStatus = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBSN(), DocumentType.DRIVING_LICENCE, request.getSequenceNr());

        if (latestPenRequestStatus == null) {
            final PenRequestStatus penRequestStatus = new PenRequestStatus();
            penRequestStatus.setDocType(DocumentType.DRIVING_LICENCE);
            penRequestStatus.setBsn(request.getBSN());
            penRequestStatus.setSequenceNo(request.getSequenceNr());
            LocalDateTime now = LocalDateTime.now();
            penRequestStatus.setRequestDatetime(now);
            penRequestStatus.setPinResetValidDate(request.getRequestDateTime().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
            repository.save(penRequestStatus);
            digidXClient.remoteLog("digid_hoog.request_pin.dws.confirmation_letter_has_been_saved", buildPayload("hidden", "true")); // Log 1278
        } else {
            latestPenRequestStatus.setPinResetValidDate(request.getRequestDateTime().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
            repository.save(latestPenRequestStatus);
            digidXClient.remoteLog("digid_hoog.request_pin.dws.confirmation_letter_ignored", buildPayload("hidden", "true")); // Log 1279
        }

        digidXClient.remoteLog("digid_hoog.request_pin.dws.confirmation_letter_received", buildPayload("hidden", "true")); // Log 1276
        digidXClient.sendNotification(request.getBSN(), "NL-Rijbewijs");
    }

    private Map<String, String> buildPayload(String key, String value) {
        final Map<String, String> payload = new HashMap<>();
        payload.put(key, value);
        return payload;
    }
}
