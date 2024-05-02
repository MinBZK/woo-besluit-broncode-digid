
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

package nl.logius.digid.dws.client;

import nl.logius.digid.dws.exception.PenRequestException;
import nl.logius.digid.dws.exception.PukRequestException;
import nl.rdw.eid_wus_crb._1.*;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;
import java.util.HashMap;
import java.util.Map;

@Component
public class RdwClient extends AbstractWsClient {

    @Autowired
    public RdwClient(@Value("${ws.client.connection_timeout}") Integer connectionTimeout,
                       @Value("${ws.client.receive_timeout}") Integer receiveTimeout,
                       @Value("${rdw_private_key_password}") String clientTlsPassphrase,
                       @Value("${rdw_client_tls_keystore}") String clientTlsKeystore) {
        super(connectionTimeout, receiveTimeout, clientTlsPassphrase, clientTlsKeystore );
    }

    @Value("${ws.client.rdw_endpoint_url}")
    private String endpoint;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PenAanvraagEIDResponse penRequest(PenAanvraagEIDRequest rdwRequest) throws PenRequestException {
        final IEidOpRijbewijs port = setupClient();

         try {
            final PenAanvraagEIDResponse penAanvraagResponse = port.getPEN(rdwRequest);
            if (penAanvraagResponse.getPenAanvraagEIDFault() != null) {
                throw new PenRequestException(penAanvraagResponse.getPenAanvraagEIDFault());
            }
            return penAanvraagResponse;
        } catch (javax.xml.ws.WebServiceException e) {
            logger.error("During the PEN request call to RDW a javax.xml.ws.WebServiceException has been thrown");
            throw new PenRequestException("DWS4", e);
        }
    }

    public OpvragenPUKCodeEIDResponse pukRequest(OpvragenPUKCodeEIDRequest rdwRequest) throws PukRequestException {
        final IEidOpRijbewijs port = setupClient();

        try {
            final OpvragenPUKCodeEIDResponse opvragenPUKCodeEIDResponse = port.getPUK(rdwRequest);
            if (opvragenPUKCodeEIDResponse.getOpvragenPUKCodeEIDFault() != null) {
                throw new PukRequestException(opvragenPUKCodeEIDResponse.getOpvragenPUKCodeEIDFault());
            }
            return opvragenPUKCodeEIDResponse;
        } catch (javax.xml.ws.WebServiceException e) {
            logger.error("During the PEN request call to RDW a javax.xml.ws.WebServiceException has been thrown");
            throw new PukRequestException("DWS4", e);
        }
    }


    private IEidOpRijbewijs setupClient() {
        final EidOpRijbewijs service = new EidOpRijbewijs();
        final IEidOpRijbewijs port = service.getCustomBindingIEidOpRijbewijs();

        final BindingProvider bp = (BindingProvider)port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://www.w3.org/2005/08/addressing"); // add wsa namespace to envelope
        final Client client = ClientProxy.getClient(port);
        client.getRequestContext().put("soap.env.ns.map", nsMap);

        setTimeouts(client);
        setupTLS(client);
        return port;
    }


}
