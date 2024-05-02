
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

import static nl.logius.digid.saml.domain.artifact.BvdStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtifactControllerTest {
    private MockHttpServletRequest httpServletRequestMock;
    private MockHttpServletResponse httpServletResponseMock;
    private ArtifactController artifactController;

    @Mock
    private ArtifactResolveService artifactResolveServiceMock;
    @Mock
    private AssertionConsumerServiceUrlService assertionConsumerServiceUrlServiceMock;
    @Mock
    private ArtifactResponseService artifactResponseServiceMock;

    @BeforeEach
    public void setup() {
        httpServletRequestMock = new MockHttpServletRequest();
        httpServletResponseMock = new MockHttpServletResponse();

        artifactController = new ArtifactController(artifactResolveServiceMock, assertionConsumerServiceUrlServiceMock, artifactResponseServiceMock);
    }

    @Test
    void redirectWithArtifactTest() throws SamlSessionException, UnsupportedEncodingException {
        String redirectUrl = "redirectUrl";
        httpServletRequestMock.setRequestedSessionId("sessionId");
        when(assertionConsumerServiceUrlServiceMock.generateRedirectUrl(anyString(), any(), anyString(), any())).thenReturn(redirectUrl);

        RedirectView result = artifactController.redirectWithArtifact("artifact", httpServletRequestMock);

        assertNotNull(result);
        assertEquals(redirectUrl, result.getUrl());
        verify(assertionConsumerServiceUrlServiceMock, times(1)).generateRedirectUrl(anyString(), any(), anyString(), any());
    }

    @Test
    void redirectFromBvdTest() throws SamlSessionException, UnsupportedEncodingException {
        String redirectUrl = "redirectUrl";
        httpServletRequestMock.setRequestedSessionId("sessionId");
        when(assertionConsumerServiceUrlServiceMock.generateRedirectUrl(any(), anyString(), anyString(), any(BvdStatus.class))).thenReturn(redirectUrl);

        RedirectView result = artifactController.redirectFromBvd("transactionId", OK, httpServletRequestMock);

        assertNotNull(result);
        assertEquals(redirectUrl, result.getUrl());
        verify(assertionConsumerServiceUrlServiceMock, times(1)).generateRedirectUrl(any(), anyString(), anyString(), any(BvdStatus.class));
    }

    @Test
    void successfullResolveArtifactTest() throws SamlParseException {
        ArtifactResolveRequest artifactResolveRequest = new ArtifactResolveRequest();
        when(artifactResolveServiceMock.startArtifactResolveProcess(any(HttpServletRequest.class))).thenReturn(artifactResolveRequest);

        artifactController.resolveArtifact(httpServletRequestMock, httpServletResponseMock);

        verify(artifactResolveServiceMock, times(1)).startArtifactResolveProcess(any(HttpServletRequest.class));
        verify(artifactResponseServiceMock, times(1)).generateResponse(any(HttpServletResponse.class), any(ArtifactResolveRequest.class));

    }
    @Test
    void failedResolveArtifactTest() throws SamlParseException {
        when(artifactResolveServiceMock.startArtifactResolveProcess(any(HttpServletRequest.class))).thenThrow(ClassCastException.class);

        ResponseEntity response = artifactController.resolveArtifact(httpServletRequestMock, httpServletResponseMock);
        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);

        verify(artifactResolveServiceMock, times(1)).startArtifactResolveProcess(any(HttpServletRequest.class));
    }
}
