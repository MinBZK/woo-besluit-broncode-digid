
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

import nl.logius.digid.oidc.Application;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.model.AccessTokenRequest;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import nl.logius.digid.oidc.service.OpenIdService;
import nl.logius.digid.oidc.service.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class})
@ActiveProfiles({ "default", "unit-test" })
class BackchannelControllerTest {
    @Mock
    private OpenIdRepository repository;
    @Mock
    private OpenIdService service;
    @Mock
    private HttpServletRequest httpServletRequest;

    private Provider provider;
    private BackchannelController controller;

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
        controller = new BackchannelController(service);
    }

    @Test
    void access() throws NoSuchAlgorithmException, DienstencatalogusException {
        controller.access(accessTokenRequest());
    }

    private AccessTokenRequest accessTokenRequest() {
        var request = new AccessTokenRequest();

        return request;
    }
}
