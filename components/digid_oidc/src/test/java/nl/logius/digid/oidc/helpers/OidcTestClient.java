
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

package nl.logius.digid.oidc.helpers;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import nl.logius.digid.oidc.service.JWTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

@Service
public class OidcTestClient extends JWTable {
    public final String CLIENT_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    public final String CHALLENGE = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    public final String CHALLENGE_VERIFIER = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    public final String GRANT_TYPE = "authorization_code";
    private JWKSet jwkSet;

    public OidcTestClient(@Value("${oidc.keystore_file}") String keystoreFile, @Value("${oidc.keystore_password}") String keystoreFilePassword) throws NoSuchAlgorithmException {
       super(keystoreFile, keystoreFilePassword);
       jwkSet = new JWKSet(List.of(generateJWK(KeyUse.ENCRYPTION), generateJWK(KeyUse.SIGNATURE)));
    }

    private JWK generateJWK(KeyUse keyUse){
        return new RSAKey.Builder((RSAPublicKey)getPublicKey())
            .privateKey((RSAPrivateKey)getPrivateKey())
            .keyUse(keyUse)
            .keyID(UUID.randomUUID().toString())
            .build();
    }

    public JWKSet getJWKSet() {
        return jwkSet;
    }

    public String generateRequest() {
        return generateRequest("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "openid");
    }

    public String generateRequest(String redirectUri, String scope) {
        JsonObject claims = new JsonObject();
        claims.addProperty("client_id", CLIENT_ID); //issuer
        claims.addProperty("response_type", "code");
        claims.addProperty("scope", scope);
        claims.addProperty("redirect_uri", redirectUri);
        claims.addProperty("state", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        claims.addProperty("nonce", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        claims.addProperty("code_challenge", CHALLENGE);
        claims.addProperty("code_challenge_method", "S256");

        return generateJWT(claims.toString());
    }

    public MultiValueMap generateTokenRequest(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("clientId", CLIENT_ID);
        formData.add("codeVerifier", CHALLENGE_VERIFIER);
        formData.add("grantType", GRANT_TYPE);
        formData.add("code", code);

        return formData;
    }
}
