
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
import nl.logius.digid.dws.exception.PukRequestException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.PenRequestStatus;
import nl.logius.digid.dws.model.PukRequest;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.rdw.eid_wus_crb._1.EIDSTATUSGEG;
import nl.rdw.eid_wus_crb._1.OpvragenPUKCodeEIDRequest;
import nl.rdw.eid_wus_crb._1.OpvragenPUKCodeEIDResponse;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.logius.digid.dws.util.EidStatInfoBuilder.eidstatinfoBuilder;

@Service
public class PukRequestService {

    @Autowired
    private PenRequestRepository repository;

    @Autowired
    private RdwClient rdwClient;

    @Autowired
    private DigidXClient digidXClient;

    // use this class to decouple time (now) from SystemTime and be able to set it in tests
    private Clock clock = Clock.systemUTC();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${shared-service.valid-days-pen}")
    private int validDaysPen;

    private final Map<String, String> statusOK = new HashMap<String, String>() {
        {
            put("status", "OK");
        }
    };

    public Map<String, String> pukRequestAllowed(PukRequest request) throws PukRequestException {
        final PenRequestStatus result = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBsn(), request.getDocType(), request.getSequenceNo());
        checkExpirationDatePen(result);

        return statusOK;
    }

    public Map<String, String> requestPuk(PukRequest request) throws PukRequestException, SoapValidationException {
        final PenRequestStatus result = repository.findFirstByBsnAndDocTypeAndSequenceNoOrderByRequestDatetimeDesc(request.getBsn(), request.getDocType(), request.getSequenceNo());
        checkExpirationDatePen(result);

        final Map<String, String> response = new HashMap<>();

        final OpvragenPUKCodeEIDRequest rdwRequest = new OpvragenPUKCodeEIDRequest();

        rdwRequest.setEIDSTATINFO(eidstatinfoBuilder(request.getBsn(), request.getSequenceNo()));

        final OpvragenPUKCodeEIDResponse rdwResponse = rdwClient.pukRequest(rdwRequest);
        final List<EIDSTATUSGEG> eidstatusgegs = rdwResponse.getEIDSTATINFO().getEIDSTATUSTAB().getEIDSTATUSGEG();
        if (eidstatusgegs.size() == 1 && eidstatusgegs.get(0).getEIDVOLGNR().equals(rdwRequest.getEIDSTATINFO().getEIDSTATAGEG().getEIDVOLGNRA()) && eidstatusgegs.get(0).getCRYPTRDEPUK() != null) {
            final String base64Puk = Base64.encodeBase64String(eidstatusgegs.get(0).getCRYPTRDEPUK());
            response.put("status", "OK");
            response.put("vpuk", base64Puk);
        } else {
            final String errormessage = "multiple EIDSTATUSGEG entries in rdw response or sequenceNo does not match";
            logger.error(errormessage);
            throw new PukRequestException("DWS8", errormessage);
        }

        return response;
    }

    public Map<String, String> pinResetCompleted(PukRequest request) throws PukRequestException {
        final List<PenRequestStatus> result = repository.findByBsnAndDocTypeAndSequenceNo(request.getBsn(), request.getDocType(), request.getSequenceNo());

        if (result.size() == 0) {
            final String errorMessage = "No Penrequest found";
            logger.info(errorMessage);
            throw new PukRequestException("DWS6", errorMessage);
        } else {
            try {
                repository.deleteAll(result);
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new PukRequestException("DWS9", e);
            }
        }

        return statusOK;
    }


    private void checkExpirationDatePen(PenRequestStatus status) throws PukRequestException {

        if (status == null) {
            final String errorMessage = "No Penrequest found";
            logger.info(errorMessage);
            throw new PukRequestException("DWS6", errorMessage);
        } else if (status.getPinResetValidDate() == null) {
            final String errorMessage = "Penrequest has no valid date";
            logger.info(errorMessage);
            throw new PukRequestException("DWS7", errorMessage);
        } else if (!(LocalDateTime.now(clock).minus(validDaysPen, ChronoUnit.DAYS).compareTo(status.getPinResetValidDate()) < 0)) {
            final String errorMessage = "Pen expired";
            digidXClient.remoteLog("digid_hoog.request_pin.dws.requested_pen_expired", buildPayload("hidden", "true", "wid_type", "rijbewijs")); // Log 1286
            logger.info(errorMessage);
            throw new PukRequestException("DWS5", errorMessage);
        }
    }

    private Map<String, String> buildPayload(String key, String value, String key2, String value2) {
        final Map<String, String> payload = new HashMap<>();
        payload.put(key, value);
        payload.put(key2, value2);
        return payload;
    }
}


