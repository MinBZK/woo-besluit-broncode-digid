
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

package nl.logius.digid.dws.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.dws.exception.DwsRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

public class Wss4jStreamInInterceptor extends WSS4JInInterceptor {

    private static Logger logger = LoggerFactory.getLogger(Wss4jStreamInInterceptor.class);

    private final String base64_keystore;
    private final String password;

    public Wss4jStreamInInterceptor(Map<String, Object> props, String base64_keystore, String password) {
        super(props);
        this.base64_keystore = base64_keystore;
        this.password = password;
    }

    @Override
    protected Crypto loadCryptoFromPropertiesFile(String propFilename, RequestData reqData) throws WSSecurityException {
        final Merlin crypto = new Merlin();

        try {
            // We dont use bouncycastle for base64 decoding because the truststore string is
            // too long
            final InputStream is = new ByteArrayInputStream(Base64.decodeBase64(this.base64_keystore));
            // Java isn't able to find the correct Certificates in PKCS12 truststores.
            // So we need a JKS formatted one instead.
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(is, this.password.toCharArray());

            crypto.setTrustStore(keystore);

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.error(e.getMessage());
            throw new DwsRuntimeException(String.format("Could not set truststore: '%s'", e.getClass().getName()));
        }

        return crypto;
    }
}
