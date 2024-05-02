
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

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nl.ictu.bsn.Gebruiker;
import nl.ictu.bsn.GebruikerSoap;
import nl.ictu.bsn.VerificatieIdenDocumentenBU;
import nl.ictu.bsn.VerificatieIdentiteitsDocument;
import nl.logius.digid.dws.exception.BvBsnException;

@Component
public class BvBsnClient extends AbstractWsClient {

    private final AbstractLoggingInterceptor logInInterceptor;
    private final AbstractLoggingInterceptor logOutInterceptor;

    @Value("${ws.client.bvbsn_endpoint_url}")
    private String endpoint;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public BvBsnClient(@Value("${ws.client.connection_timeout}") Integer connectionTimeout,
            @Value("${ws.client.receive_timeout}") Integer receiveTimeout,
            @Value("${bvbsn_private_key_password}") String clientTlsPassphrase,
            @Value("${bvbsn_client_tls_keystore}") String clientTlsKeystore,
            AbstractLoggingInterceptor logInInterceptor,
            AbstractLoggingInterceptor logOutInterceptor) {
        super(connectionTimeout, receiveTimeout, clientTlsPassphrase, clientTlsKeystore);
        this.logInInterceptor = logInInterceptor;
        this.logOutInterceptor = logOutInterceptor;
    }

    public VerificatieIdenDocumentenBU postVerifyTravelDocument(
            VerificatieIdentiteitsDocument verificatieIdentiteitsDocument) throws BvBsnException {
        final GebruikerSoap port = setupClient();

        try {
            final VerificatieIdenDocumentenBU verificatieIdenDocumentenBU = port
                    .verificatieIdentiteitsDocument(verificatieIdentiteitsDocument.getBerichtIn());
            if (verificatieIdenDocumentenBU.getIdenDocumentenResultaat() == null) {
                throw new BvBsnException("There was not result in the response from the BV BSN");
            }
            return verificatieIdenDocumentenBU;
        } catch (javax.xml.ws.WebServiceException e) {
            logger.error("During the call to BV BSN a javax.xml.ws.WebServiceException has been thrown");
            throw new BvBsnException("Webservice exception");
        }
    }

    private GebruikerSoap setupClient() {
        final Gebruiker service = new Gebruiker();
        final GebruikerSoap port = service.getGebruikerSoap();

        final BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://www.w3.org/2005/08/addressing"); // add wsa namespace to envelope
        final Client client = ClientProxy.getClient(port);
        client.getRequestContext().put("soap.env.ns.map", nsMap);
        client.getInInterceptors().add(logInInterceptor);
        client.getOutInterceptors().add(logOutInterceptor);
        setTimeouts(client);
        setupTLS(client);
        return port;
    }
}
