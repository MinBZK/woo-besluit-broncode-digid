
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

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import nl.logius.digid.oidc.client.AdClient;
import nl.logius.digid.oidc.client.AppClient;
import nl.logius.digid.oidc.client.DienstenCatalogusClient;
import nl.logius.digid.oidc.exception.AuthenticationLevelTooLowException;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.exception.OpenIdSessionNotFoundException;
import nl.logius.digid.oidc.model.AccessTokenRequest;
import nl.logius.digid.oidc.model.AccessTokenResponse;
import nl.logius.digid.oidc.model.AuthenticateRequest;
import nl.logius.digid.oidc.model.DcMetadataResponse;
import nl.logius.digid.oidc.model.LevelOfAssurance;
import nl.logius.digid.oidc.model.OpenIdSession;
import nl.logius.digid.oidc.model.StatusResponse;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.ValidationException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OpenIdService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenIdRepository openIdRepository;
    private final AppClient appClient;
    private final AdClient adClient;
    private final DienstenCatalogusClient dcClient;
    private final Provider provider;

    @Value("${hosts.app}")
    private String appHost;

    @Value("${hosts.digid}")
    private String digidHost;

    @Value("${protocol}")
    private String protocol;

    @Value("${protocol}://${hosts.ad}/openid-connect/v1")
    private String issuer;

    @Autowired
    public OpenIdService(OpenIdRepository openIdRepository, AppClient appClient, AdClient adClient, DienstenCatalogusClient dcClient, Provider provider) {
        this.openIdRepository = openIdRepository;
        this.appClient = appClient;
        this.adClient = adClient;
        this.dcClient = dcClient;
        this.provider = provider;
    }

    public String redirectWithSession(AuthenticateRequest params) throws IOException, ParseException, JOSEException, InvalidSignatureException, DienstencatalogusException {
        var session = startSessionFromApp(params);

        return protocol + "://" + appHost+ "/digid-app?data=" + URLEncoder.encode("digid-app-auth://app_session_id=" + session.get("app_session_id") + "&host=" + digidHost + "&browser=safari", StandardCharsets.UTF_8);
    }

    public OpenIdSession startSession(@Valid AuthenticateRequest params, String jwksUri, Long legacyWebserviceId, String serviceName) {
        OpenIdSession session = new OpenIdSession();
        session.setJwksUri(jwksUri);
        session.setClientId(params.getClientId());
        session.setResponseType(params.getResponseType());
        session.setScope(params.getScope());
        session.setRedirectUri(params.getRedirectUri());
        session.setState(params.getState());
        session.setNonce(params.getNonce());
        session.setCodeChallenge(params.getCodeChallenge());
        session.setCodeChallengeMethod(params.getCodeChallengeMethod());
        session.setCode(UUID.randomUUID().toString());
        session.setLegacyWebserviceId(legacyWebserviceId);
        session.setServiceName(serviceName);

        return session;
    }

    public String getClientReturnId(String sessionId) {
        Optional<OpenIdSession> session = openIdRepository.findById(sessionId);

        if (session.isEmpty()) return null;

        OpenIdSession openIdSession = session.get();

        var returnUrl = openIdSession.getRedirectUri() + "?state=" + openIdSession.getState();
        if (!"success".equals(openIdSession.getAuthenticationState())) {
            return returnUrl + "&error=CANCELLED";
        }

        return returnUrl + "&code=" + openIdSession.getCode();
    }

    public AccessTokenResponse createAccesToken(AccessTokenRequest request) throws NoSuchAlgorithmException, DienstencatalogusException {
        var openIdSession = openIdRepository.findByCode(request.getCode()).orElseThrow(()
                -> new OpenIdSessionNotFoundException("OpenIdSession not found"));

        var metadata = dcClient.retrieveMetadataFromDc(request.getClientId());

        validateCodeChallenge(request.getCodeVerifier(), openIdSession);
        validateMimumAuthenticationLevel(openIdSession, metadata);

        AccessTokenResponse response = new AccessTokenResponse();
        response.setTokenType("Bearer");
        response.setAccessToken(getAccessToken(openIdSession));
        response.setIdToken(getIdToken(openIdSession));
        response.setState(openIdSession.getState());

        var logCode = Map.of("20", "743", "25", "1124").get(openIdSession.getAuthenticationLevel());
        adClient.remoteLog(logCode, Map.of("account_id", openIdSession.getAccountId(), "webservice_id", openIdSession.getLegacyWebserviceId(), "webservice_name", openIdSession.getServiceName()));

        openIdRepository.delete(openIdSession);

        return response;
    }

    public Map<String, String> startSessionFromApp(@Valid AuthenticateRequest params) throws IOException, ParseException, JOSEException, InvalidSignatureException, DienstencatalogusException {
        var response = dcClient.retrieveMetadataFromDc(params.getClientId());
        validateSignature(response, params);

        adClient.remoteLog("1121", Map.of("webservice_id", response.getLegacyWebserviceId()));

        var oidcSession = startSession(params, response.getMetadataUrl(), response.getLegacyWebserviceId(), response.getServiceName());
        openIdRepository.save(oidcSession);

        var appSession = appClient.startAppSession(
                issuer + "/return?sessionId=" + oidcSession.getId(),
                response.getServiceName(),
                response.getLegacyWebserviceId(),
                response.getMinimumReliabilityLevel(),
                response.getIconUri(),
                oidcSession
        );

        return Map.of("app_session_id", appSession.get("id"));
    }

    public StatusResponse userLogin(OpenIdSession session, Long accountId, String authenticationLevel, String authenticationStatus){
        var bsnResponse = adClient.getBsn(accountId);

        session.setAuthenticationLevel(authenticationLevel);
        session.setAuthenticationState(authenticationStatus);
        session.setAccountId(accountId);
        session.setBsn(bsnResponse.get("bsn"));

        openIdRepository.save(session);

        return new StatusResponse("OK");
    }

    public String redirectWithError(AuthenticateRequest params) throws DienstencatalogusException, InvalidSignatureException, IOException, ParseException, JOSEException {
        var response = dcClient.retrieveMetadataFromDc(params.getClientId());
        validateSignature(response, params);

        return params.getRedirectUri() + "?state=" + params.getState() + "?error=invalid_request_object";
    }

    private void validateSignature(DcMetadataResponse response, AuthenticateRequest params) throws DienstencatalogusException, JOSEException, InvalidSignatureException, IOException, ParseException {
        if (response == null || !response.getRequestStatus().equals("STATUS_OK")) {
            throw new DienstencatalogusException("Metadata from dc not found or not active: " + response.getErrorDescription());
        }

        if (params.getRedirectUri() == null || !params.getRedirectUri().startsWith(response.getAppReturnUrl())) {
            throw new ValidationException("redirect_uri does not match metadata");
        }

        provider.verifySignature(response.getMetadataUrl(), params.getSignedJwt());
    }

    private void validateMimumAuthenticationLevel(OpenIdSession openIdSession, DcMetadataResponse metadata) {
        if (Integer.parseInt(metadata.getMinimumReliabilityLevel()) > Integer.parseInt(openIdSession.getAuthenticationLevel())) {
            throw new AuthenticationLevelTooLowException("minimumLevel is too low");
        }
    }

    private String getAccessToken(OpenIdSession openIdSession) {
        JsonObject claims = new JsonObject();
        claims.addProperty("iss", issuer); //issuer
        claims.addProperty("azp", openIdSession.getClientId());
        claims.addProperty("exp", Instant.now().plusSeconds(2 * 60).getEpochSecond()); //expiration
        claims.addProperty("jti", UUID.randomUUID().toString());

        return provider.generateJWT(claims.toString());
    }

    private String getIdToken(OpenIdSession openIdSession) {
        JsonObject claims = new JsonObject();
        claims.addProperty("iss", issuer); //issuer
        claims.addProperty("aud", issuer + "/token");
        claims.addProperty("sub", openIdSession.getBsn());
        claims.addProperty("sub_id_type", "urn:nl-eid-gdi:1.0:id:legacy-BSN");
        claims.addProperty("nonce", openIdSession.getNonce());
        claims.addProperty("exp", Instant.now().plusSeconds(2 * 60).getEpochSecond()); //expiration
        claims.addProperty("iat", Instant.now().getEpochSecond());//issued at
        claims.addProperty("nbf", Instant.now().plusSeconds( 2 * 60).getEpochSecond()); //notbefore

        claims.addProperty("acr", LevelOfAssurance.map(Integer.valueOf(openIdSession.getAuthenticationLevel()))); //
        claims.addProperty("jti", UUID.randomUUID().toString());

        return provider.generateJWE(provider.generateJWT(claims.toString()), openIdSession.getJwksUri());
    }

    private void validateCodeChallenge(String codeVerifier, OpenIdSession openIdSession) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(codeVerifier.getBytes());

        byte[] digestBytes = digest.digest();
        String encodeCodeVerifier = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digestBytes);

        if (!encodeCodeVerifier.equals(openIdSession.getCodeChallenge())) {
            throw new ValidationException("CodeVerifier does not match");
        }
    }
}


