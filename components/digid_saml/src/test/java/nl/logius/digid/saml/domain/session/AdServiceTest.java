
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

package nl.logius.digid.saml.domain.session;

import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.artifact.BvdStatus;
import nl.logius.digid.saml.domain.authentication.AdAuthenticationMapper;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.saml.exception.AdValidationException;
import nl.logius.digid.saml.exception.BvdException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static nl.logius.digid.saml.domain.session.AdAuthenticationStatus.STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
public class AdServiceTest {
    private AdAuthenticationMapper adAuthenticationMapper = Mappers.getMapper(AdAuthenticationMapper.class);

    private AdService adService;

    @Mock
    private BvdClient bvdClientMock;
    @Mock
    private AdSessionRepository adSessionRepositoryMock;
    @Mock
    private AssertionConsumerServiceUrlService assertionConsumerServiceUrlServiceMock;

    @BeforeEach
    public void setup() {
        adService = new AdService(adSessionRepositoryMock, bvdClientMock, assertionConsumerServiceUrlServiceMock);
    }

    @Test
    public void startAuthenticationSessionSuccessful() {
        when(adSessionRepositoryMock.findBySessionId(null)).thenReturn(Optional.of(new AdSession()));
        AdSession adSession = adAuthenticationMapper.authenticationRequestToAdSession("callbackUrl", authenticationRequest(), new ArrayList<>());

        adService.createAuthenticationSession(adSession);

        verify(adSessionRepositoryMock, times(1)).findBySessionId(null);
        verify(adSessionRepositoryMock, times(1)).save(any(AdSession.class));
    }

    @Test
    public void createNewAdSession() {
        when(adSessionRepositoryMock.findBySessionId(null)).thenReturn(Optional.empty());
        AdSession adSession = adAuthenticationMapper.authenticationRequestToAdSession("callbackUrl", authenticationRequest(), new ArrayList<>());

        adService.createAuthenticationSession(adSession);

        verify(adSessionRepositoryMock, times(1)).findBySessionId(null);
        verify(adSessionRepositoryMock, times(1)).save(MockitoHamcrest.argThat(
                Matchers.allOf(
                        Matchers.hasProperty("legacyWebserviceId", Matchers.equalTo(1L)),
                        Matchers.hasProperty("requiredLevel", Matchers.equalTo(10)),
                        Matchers.hasProperty("entityId", Matchers.equalTo("entity-id")),
                        Matchers.hasProperty("ssoSession", Matchers.equalTo(false)),
                        Matchers.hasProperty("ssoLevel", Matchers.equalTo(20)),
                        Matchers.hasProperty("permissionQuestion", Matchers.equalTo("Name from PermissionQuestion"))
                )
        ));
    }

    @Test
    public void resolveAuthenticationResultFailed() {
        Exception result = assertThrows(AdException.class,
                () -> adService.resolveAuthenticationResult("httpSessionId"));
        assertEquals(result.getMessage(), "no adSession found");
    }

    @Test
    public void updateAdSession() throws AdValidationException {
        HashMap<String, Object> body = new HashMap<>();
        body.put("authentication_level", 10);
        body.put("authentication_status", AdAuthenticationStatus.STATUS_SUCCESS.label);
        body.put("bsn", "PPPPPPPPP");

        AdSession result = adService.updateAdSession(new AdSession(), body);

        assertNotNull(result);
        assertEquals(result.getAuthenticationLevel(), 10);
        assertEquals(result.getAuthenticationStatus(), "success");
        assertEquals(result.getBsn(), "PPPPPPPPP");
    }

    @Test
    public void updateAdSessionInvalid() {
        HashMap<String, Object> body = new HashMap<>();
        body.put("authentication_level", 1);
        body.put("authentication_status", "Pending");
        body.put("bsn", "PPPPPPP");

        AdValidationException exception = assertThrows(AdValidationException.class, () -> adService.updateAdSession(new AdSession(), body));

        assertEquals("AdSession validation error", exception.getLocalizedMessage());
        assertEquals(3, exception.getDetails().getErrorCount());
    }

    @Test
    public void getAdSession() {
        when(adSessionRepositoryMock.findBySessionId(anyString())).thenReturn(Optional.empty());

        Exception result = assertThrows(AdException.class,
                () -> adService.getAdSession("httpSessionId"));
        assertEquals(result.getMessage(), "no adSession found");
    }

    @Test
    public void checkAuthenticationStatusSuccessTest() throws BvdException, AdException, SamlSessionException, UnsupportedEncodingException {
        AdSession adSession = new AdSession();
        adSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_SUCCESS.label);
        adSession.setBsn("1234");
        adSession.setAuthenticationLevel(10);

        SamlSession samlSession = new SamlSession(1L);
        samlSession.setServiceEntityId("serviceEntityId");
        samlSession.setTransactionId("transactionId");
        samlSession.setServiceUuid("serviceUuid");

        String artifact = "artifact";


        adService.checkAuthenticationStatus(adSession, samlSession, artifact);

        verify(bvdClientMock, times(1)).startBvdSession(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

    }

    @Test
    public void checkAuthenticationStatusFailedTest() {
        AdSession adSession = new AdSession();
        adSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_FAILED.label);

        SamlSession samlSession1 = new SamlSession(1L);

        String artifact = "artifact";

        Exception result = assertThrows(AdException.class,
                () -> adService.checkAuthenticationStatus(adSession, samlSession1, artifact));

        assertEquals(result.getMessage(), "No successful authentication");
    }

    @Test
    public void checkAuthenticationStatusCanceledTest() throws BvdException, AdException, SamlSessionException, UnsupportedEncodingException {
        AdSession adSession = new AdSession();
        adSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_CANCELED.label);

        SamlSession samlSession = new SamlSession(1L);
        samlSession.setTransactionId("transactionId");
        samlSession.setHttpSessionId("HttpSessionId");

        String artifact = "artifact";

        adService.checkAuthenticationStatus(adSession, samlSession, artifact);

        verify(assertionConsumerServiceUrlServiceMock, times(1)).generateRedirectUrl(anyString(), anyString(), anyString(), any(BvdStatus.class));
    }

    @Test
    public void updateAuthenticationStatusTest() {
        AdSession adSession = adAuthenticationMapper.authenticationRequestToAdSession("callbackUrl", new AuthenticationRequest(), new ArrayList<>());

        adService.updateAuthenticationStatus(adSession, STATUS_INVALID);

        verify(adSessionRepositoryMock, times(1)).save(any(AdSession.class));
    }

    private AuthenticationRequest authenticationRequest() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setLegacyWebserviceId(1L);
        request.setMinimumRequestedAuthLevel(10);
        request.setServiceEntityId("entity-id");
        request.setSsoAuthLevel(20);
        request.setValidSsoSession(false);
        request.setPermissionQuestion("Name from PermissionQuestion");

        return request;
    }
}
