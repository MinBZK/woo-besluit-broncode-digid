
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

package nl.logius.digid.digilevering.lib.config;

import org.bouncycastle.util.encoders.Base64;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import static javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm;

public interface JmsConfigUtil {

    public static KeyManager[] readKeystore(String keyStoreBase64, String keyStorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        if(keyStoreBase64 != null) {
            InputStream ksIs = new ByteArrayInputStream(Base64.decode(keyStoreBase64));
            keyStore.load(ksIs, keyStorePassword.toCharArray());
        } else {
            keyStore.load(null);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(getDefaultAlgorithm());

        kmf.init(keyStore, keyStorePassword.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();

        return keyManagers;
    }

    public static TrustManager[] readTruststore(String trustStoreBase64, String trustStorePassword) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        if(trustStoreBase64 != null) {
            InputStream tsIs = new ByteArrayInputStream(Base64.decode(trustStoreBase64));
            trustStore.load(tsIs, trustStorePassword.toCharArray());
        } else {
            trustStore.load(null);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        return trustManagers;
    }
}
