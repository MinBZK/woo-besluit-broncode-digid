
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

package nl.logius.digid.oidc.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import nl.logius.digid.oidc.Application;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.model.AuthenticateRequest;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import nl.logius.digid.oidc.service.OpenIdService;
import nl.logius.digid.oidc.service.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class})
@ActiveProfiles({ "default", "unit-test" })
class FrontchannelControllerTest {
    @Mock
    private OpenIdRepository repository;
    @Mock
    private OpenIdService service;
    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServerHttpResponse serverHttpResponse;

    private Provider provider;
    private FrontchannelController controller;

    @Value("${oidc.keystore_file}")
    private String keystoreFile;

    @Value("${oidc.keystore_password}")
    private String keystoreFilePassword;

    @Value("${proxy:}")
    private String configuredProxyUrl;

    @Value("${protocol}://${hosts.ad}/openid-connect/v1")
    private String issuer;

    @Value("${protocol}://${hosts.api}/openid-connect/v1")
    private String backchannel;

    @BeforeEach
    void beforeEach() {
        provider = new Provider(keystoreFile, keystoreFilePassword, configuredProxyUrl, issuer, backchannel);
        controller = new FrontchannelController(service, provider);
    }

    @Test
    void authenticate() throws InvalidSignatureException, IOException, ParseException, DienstencatalogusException, JOSEException {
        controller.authenticate(authenticateRequest(), null);
    }

    @Test
    void returnFomAd() {
        controller.returnFomAd("sessionId");
    }

    @Test
    void configuration() throws JsonProcessingException {
        var response = controller.configuration();

        assertEquals(List.of("code"), response.get("response_types_supported"));
        assertEquals(List.of("authorization_code"), response.get("grant_types_supported"));
        assertEquals(List.of("openid"), response.get("scopes_supported"));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", response.get("issuer"));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", response.get("authorization_endpoint"));
        assertEquals(List.of("urn:nl-eid-gdi:1.0:id:legacy-BSN"), response.get("sub_id_types_supported"));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", response.get("jwks_uri"));
        assertEquals(List.of("RS256"), response.get("id_token_signing_alg_values_supported"));
    }

    @Test
    void jwks() {
        var response = controller.jwks();

        List list = (List) response.get("keys");
        Map<String, String> key = (Map<String,String>) list.get(0);

        assertEquals(1, list.size());
        assertEquals("RSA", key.get("kty"));
        assertEquals("sig", key.get("use"));
        assertEquals("AQAB", key.get("e"));
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", key.get("n"));
    }

    private AuthenticateRequest authenticateRequest() {
        var request = new AuthenticateRequest();

        return request;
    }
}
