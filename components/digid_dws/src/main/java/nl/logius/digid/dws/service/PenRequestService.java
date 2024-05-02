
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
import nl.rdw.eid_wus_crb._1.PenAanvraagEIDRequest;
import nl.rdw.eid_wus_crb._1.PenAanvraagEIDResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static nl.logius.digid.dws.util.EidStatInfoBuilder.eidstatinfoBuilder;

@Service
public class PenRequestService {

    private final Map<String, String> statusOK = new HashMap<String, String>()    {
        {
            put("status", "OK");
        }
    };

    @Autowired
    private PenRequestRepository repository;

    @Autowired
    private SharedServiceClient ssClient;

    @Autowired
    private RdwClient client;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // use this class to decouple time (now) from SystemTime and be able to set it in tests
    private Clock clock = Clock.systemUTC();

    @Value("${shared-service.minimal-time-between-requests}")
    private String minimalTimeBetweenRequests;

    @Value("${shared-service.allowed-requests-per-period}")
    private String allowedRequestsPerPeriod;

    public Map<String, String> penRequestAllowed(PenRequest request) throws PenRequestException, SharedServiceClientException {
        final List<PenRequestStatus> result = repository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo());
        checkIfTooSoonOrTooOften(result);
        return statusOK;
    }

    public Map<String, String> requestPenReset(PenRequest request) throws PenRequestException, SharedServiceClientException, SoapValidationException {
        final List<PenRequestStatus> result = repository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo());
        checkIfTooSoonOrTooOften(result);

        final Map<String, String> response = new HashMap<>();

        final PenAanvraagEIDRequest rdwRequest = new PenAanvraagEIDRequest();

        rdwRequest.setEIDSTATINFO(eidstatinfoBuilder(request.getBsn(), request.getSequenceNo()));

        final PenAanvraagEIDResponse rdwResponse = client.penRequest(rdwRequest);
        logger.info("The SOAP pen request was successful");

        if ( !"OK".equals(rdwResponse.getResponseDescription())) {
            String errorMessage = "Connection with RDW was successful, but the response description was not equal to OK";
            logger.error(errorMessage);
            throw new PenRequestException("DWS10", errorMessage);
        }

        createPenRequestStatus(request.getBsn(), request.getSequenceNo(), DocumentType.DRIVING_LICENCE);

        response.putAll(statusOK);

        return response;
    }

    private void createPenRequestStatus (String bsn, String sequenceNo, DocumentType documentType) {
        final PenRequestStatus penRequestStatus = new PenRequestStatus();
        penRequestStatus.setBsn(bsn);
        penRequestStatus.setSequenceNo(sequenceNo);
        penRequestStatus.setDocType(documentType);
        // Instant is a ZonedDateTime and return UTC
        LocalDateTime now = LocalDateTime.now(clock);
        penRequestStatus.setRequestDatetime(now);
        repository.save(penRequestStatus);
    }

    private void checkIfTooSoonOrTooOften(List<PenRequestStatus> penRequestStatusList) throws PenRequestException, SharedServiceClientException {
        if (penRequestStatusList.isEmpty()) {
            logger.info("Pen request allowed because no previous Pen request found");
            return;
        }

        int counter = 0;
        final int tooSoonConfig = ssClient.getSSConfigInt(minimalTimeBetweenRequests);
        final int tooOftenConfig = ssClient.getSSConfigInt(allowedRequestsPerPeriod);

        LocalDateTime startOfMonth = LocalDate.now(clock).atStartOfDay().with(firstDayOfMonth());

        for (PenRequestStatus penRequestStatus : penRequestStatusList) {

            if (LocalDateTime.now(clock).minus(tooSoonConfig, ChronoUnit.DAYS).compareTo(penRequestStatus.getRequestDatetime()) < 0) {
                //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
                final String errorMessage = "Request is too soon after previous request";
                logger.info(errorMessage);
                throw new PenRequestException("DWS1", errorMessage);
            }

            if (startOfMonth.compareTo(penRequestStatus.getRequestDatetime()) < 0) {
                //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
                counter++;

                if (counter >= tooOftenConfig) {
                    final String errorMessage = String.format("Too many requests (%d) of the allowed (%d) in the configured period ", counter, tooOftenConfig);
                    logger.info(errorMessage);
                    throw new PenRequestException("DWS2", errorMessage);
                }
            }
        }
    }
}
