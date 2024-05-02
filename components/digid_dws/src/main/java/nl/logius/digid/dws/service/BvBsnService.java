
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

import com.google.common.collect.ImmutableMap;
import nl.ictu.bsn.VerificatieIdenDocumentenBU;
import nl.ictu.bsn.VerificatieIdentiteitsDocument;
import nl.logius.digid.dws.client.BvBsnClient;
import nl.logius.digid.dws.exception.BvBsnException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.TravelDocumentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.dws.util.VerificatieIdenDocumentenBIBuilder.verificatieIdenDocumentenBIBuilder;

@Service
public class BvBsnService {

    @Value("${ws.client.bvbsn_sender}")
    private String Sender;

    @Value("${ws.client.bvbsn_indication_end_user}")
    private String IndicationEndUser;

    private final Map<String, String> statusOK = ImmutableMap.of("status", "OK");

    @Autowired
    private BvBsnClient client;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Map<String, String> verifyTravelDocument(TravelDocumentRequest request) throws SoapValidationException, BvBsnException {
        final Map<String, String> response = new HashMap<>();

        final VerificatieIdentiteitsDocument bvBsnRequest = new VerificatieIdentiteitsDocument();
        bvBsnRequest.setBerichtIn(verificatieIdenDocumentenBIBuilder(DocumentType.valueOf(request.getDocumentType()), request.getDocumentNumber(), Sender, IndicationEndUser));

        final VerificatieIdenDocumentenBU bvBsnResponse = client.postVerifyTravelDocument(bvBsnRequest);
        logger.info("The SOAP BV BSN request was successful");
        var resultCode = bvBsnResponse.getIdenDocumentenResultaat().getVerificatieIdenDocumentenResultaatDE().get(0).getResultaatCode();
        if ( 24002 != resultCode) {
            String errorMessage = "Connection with BV BSN was successful, but the response description was not equal to 24002: ";
            logger.error(errorMessage + resultCode);
            throw new BvBsnException("Response other then 24002");
        }

        response.putAll(statusOK);

        return response;
    }
}
