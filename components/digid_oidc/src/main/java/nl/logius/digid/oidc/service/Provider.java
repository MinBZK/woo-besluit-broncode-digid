
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

package nl.logius.digid.oidc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.model.LevelOfAssurance;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Provider extends JWTable{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Provider.class);

    private final String configuredProxyUrl;
    private final String frontchannel;
    private final String backchannel;

    @Autowired
    public Provider(@Value("${oidc.keystore_file}") String keystoreFile,  @Value("${oidc.keystore_password}") String keystoreFilePassword, @Value("${proxy:}") String configuredProxyUrl, @Value("${protocol}://${hosts.ad}/openid-connect/v1") String frontchannel, @Value("${protocol}://${hosts.api}/openid-connect/v1") String backchannel) {
        super(keystoreFile, keystoreFilePassword);
        this.configuredProxyUrl = configuredProxyUrl;
        this.frontchannel = frontchannel;
        this.backchannel = backchannel;
    }

    public Map<String, Object> metadata() throws JsonProcessingException {
        Map<String, Object> response = new HashMap<>();
        response.put("issuer", frontchannel);
        response.put("authorization_endpoint", frontchannel + "/authorization");
        response.put("jwks_uri", frontchannel + "/jwks");
        response.put("token_endpoint", backchannel + "/token");
        response.put("scopes_supported", List.of("openid"));
        response.put("response_types_supported", List.of("code"));
        response.put("claims_parameter_supported", false);
        response.put("claims_supported", List.of("sub", "acr"));
        response.put("grant_types_supported", List.of("authorization_code"));
        response.put("subject_types_supported", List.of("public"));
        response.put("sub_id_types_supported", List.of("urn:nl-eid-gdi:1.0:id:legacy-BSN"));
        response.put("acr_values_supported", List.of(LevelOfAssurance.MIDDEN, LevelOfAssurance.SUBSTANTIEEL));
        response.put("token_endpoint_auth_methods_supported", List.of("tls_client_auth"));

        response.put("id_token_signing_alg_values_supported", List.of("RS256"));
        response.put("id_token_encryption_alg_values_supported", List.of("RS256"));

        response.put("request_object_signing_alg_values_supported", Arrays.asList("RS256"));
        response.put("request_object_encryption_enc_values_supported", Arrays.asList("RS256"));

        response.put("request_uri_parameter_supported", false);


        response.put("signed_metadata", generateJWT(MAPPER.writeValueAsString(response)));
        return response;
    }

    public String generateJWE(String data, String jwksUri) {
        JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A256GCM);
        JWEObject jwsObject = new JWEObject(header, new Payload(data));
        logger.debug("jwt data: {}", data);

        try {
            var publicEncryptionKey = getPublicEncryptionKey(jwksUri);
            jwsObject.encrypt(new RSAEncrypter(publicEncryptionKey));
        } catch (JOSEException | IOException | ParseException e) {
            return null;
        }

        return jwsObject.serialize();
    }

    private RSAPublicKey getPublicEncryptionKey(String jwksUri) throws IOException, ParseException, JOSEException {
        return ((RSAKey) getPublicKeys(jwksUri).getKeys().stream()
                .filter(k -> k.getKeyUse().equals(KeyUse.ENCRYPTION))
                .findFirst().get()).toRSAPublicKey();
    }

    public boolean verifySignature(String jwksUri, SignedJWT signedJwt) throws JOSEException, InvalidSignatureException, IOException, ParseException {
        var publicKeys = getPublicKeys(jwksUri);
        var kid = signedJwt.getHeader().getKeyID();

        if (kid != null) {
            var key = ((RSAKey) publicKeys.getKeyByKeyId(kid));
            if (key != null) {
                RSASSAVerifier rsaSSAVerifier = new RSASSAVerifier(key.toRSAPublicKey());
                if (signedJwt.verify(rsaSSAVerifier)) return true;
            }
        }

        for (JWK jwk : publicKeys.getKeys()) {
            if (signedJwt.verify(new RSASSAVerifier(((RSAKey) jwk).toRSAPublicKey())))
                return true;
        }

        throw new InvalidSignatureException("Could not validate signature of JWT token");
    }

    @Cacheable(value = "jwks-public", key = "jwksUri")
    public JWKSet getPublicKeys(String jwksUri) throws IOException, ParseException {
        Proxy proxy = null;

        if (StringUtils.isNotBlank(configuredProxyUrl)) {
            URL proxyUrl = new URL(configuredProxyUrl);
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getByName(proxyUrl.getHost()), proxyUrl.getPort()));
        }

        return JWKSet.load(new URL(jwksUri), 5000, 5000, 10240, proxy);
    }
}
