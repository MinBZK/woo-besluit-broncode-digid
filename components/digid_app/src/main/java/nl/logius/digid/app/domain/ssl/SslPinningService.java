
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

package nl.logius.digid.app.domain.ssl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.*;

@Service
public class SslPinningService {
    private static final Logger logger = LoggerFactory.getLogger(SslPinningService.class);
    private final SslPinningRepository repository;

    private final String signingKey;

    @Autowired
    public SslPinningService(SslPinningRepository repository, @Value("${ssl.signing_key}") String signingKey) {
        this.repository = repository;
        this.signingKey = signingKey;
    }


    public JsonObject getPayload(List<SslCertificate> certificates) {
        JsonObject claims = new JsonObject();

        Map<String, JsonArray> map = new HashMap<>();

        for (SslCertificate certificate : certificates) {
            for (Object domain : certificate.getSans()) {
                if (map.containsKey(domain)) {
                    map.get(domain).add(certificate.getFingerprint());
                } else {
                    var list = new JsonArray();
                    list.add(certificate.getFingerprint());
                    map.put((String) domain, list);
                }
            }
        }

        map.forEach(claims::add);

        return claims;
    }

    public String getSignedJwtWithPins() {
        var list = repository.findAll();
        var payload = getPayload(list);

        return generateJWT(payload.toString());
    }

    private String generateJWT(String data) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES512).keyID("1").build();
        JWSObject jwsObject = new JWSObject(header, new Payload(data));
        logger.debug("jwt data: {}", data);

        try {
            jwsObject.sign(new ECDSASigner(getPrivateKey()));
        } catch (JOSEException e) {
            return null;
        }

        return jwsObject.serialize();
    }

    private ECPrivateKey getPrivateKey() {
        try {
            final PEMParser pemParser = new PEMParser(new StringReader(new String(Base64.getDecoder().decode(signingKey))));
            final Object parsedPem = pemParser.readObject();
            if (!(parsedPem instanceof PEMKeyPair)) {
                throw new IOException("Attempted to parse PEM string as a keypair, but it's actually a " + parsedPem.getClass());
            }

            Security.addProvider(new BouncyCastleProvider());

            final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            return (ECPrivateKey) converter.getKeyPair((PEMKeyPair) parsedPem).getPrivate();
        } catch (IOException e) {
            logger.error("Can't read private key", e);
            return null;
        }
    }

}
