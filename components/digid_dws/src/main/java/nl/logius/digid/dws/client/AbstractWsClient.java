
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

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

public abstract class AbstractWsClient {

    private static Logger logger = LoggerFactory.getLogger(AbstractWsClient.class);

    private int connectionTimeout;
    protected int receiveTimeout;
    protected String clientTlsPassphrase;
    protected String clientTlsKeystore;

    public AbstractWsClient(int connectionTimeout, int receiveTimeout, String clientTlsPassphrase, String clientTlsKeystore) {
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
        this.clientTlsPassphrase = clientTlsPassphrase;
        this.clientTlsKeystore = clientTlsKeystore;
    }

    protected void setTimeouts (Client client) {
        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(connectionTimeout);
        httpClientPolicy.setReceiveTimeout(receiveTimeout);
        http.setClient(httpClientPolicy);
    }

    protected void setupTLS(Client client) {
        try {
            HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
            TLSClientParameters tlsCP = new TLSClientParameters();

            InputStream is = new ByteArrayInputStream(Base64.decode(clientTlsKeystore));
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, clientTlsPassphrase.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(getDefaultAlgorithm());
            kmf.init(keyStore, clientTlsPassphrase.toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);
            tlsCP.setKeyManagers(keyManagers);

            httpConduit.setTlsClientParameters(tlsCP);
        } catch (IOException | GeneralSecurityException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
