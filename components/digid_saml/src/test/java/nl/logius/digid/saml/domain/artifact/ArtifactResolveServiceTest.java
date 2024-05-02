
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

import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.*;
import nl.logius.digid.saml.exception.AdException;
import nl.logius.digid.saml.exception.SamlParseException;
import nl.logius.digid.saml.exception.SamlSessionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@ActiveProfiles({"default", "integration-test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArtifactResolveServiceTest extends AbstractBaseTest {

    private SamlSession samlSession;
    private AdAuthentication adAuthentication;
    private AdSession adSession;

    @Autowired
    private ArtifactResolveService artifactResolveService;
    @Mock
    private SamlSessionService samlSessionServiceMock;
    @Mock
    private DcMetadataService dcMetadataServiceMock;
    @Mock
    private SignatureService signatureServiceMock;
    @Mock
    private AdService adServiceMock;

    @Value("classpath:requests/artifact-request-valid.xml")
    private Resource artifactResolveValid;
    @Value("classpath:requests/artifact-request-valid-bvd.xml")
    private Resource artifactResolveValidBVD;
    @Value("classpath:requests/artifact-request-no-signature.xml")
    private Resource artifactResolveRequestNoSignature;
    @Value("classpath:requests/artifact-request-invalid-version.xml")
    private Resource artifactResolveRequestInvalidVersion;
    @Value("classpath:requests/artifact-request-no-artifact.xml")
    private Resource artifactResolveRequestNoArtifact;

    @BeforeEach
    public void before() throws AdException {
        ReflectionTestUtils.setField(artifactResolveService, "samlSessionService", samlSessionServiceMock);
        ReflectionTestUtils.setField(artifactResolveService, "dcMetadataService", dcMetadataServiceMock);
        ReflectionTestUtils.setField(artifactResolveService, "adService", adServiceMock);
        ReflectionTestUtils.setField(artifactResolveService, "signatureService", signatureServiceMock);

        samlSession = new SamlSession(1000L);
        samlSession.setConnectionEntityId("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000");
        samlSession.setServiceEntityId("serviceEntityId");
        samlSession.setServiceUuid("serviceUuid");
        samlSession.setAuthenticationStatus("success");
        samlSession.setAuthenticationLevel(10);
        samlSession.setBsn("PPPPPPPP");
        samlSession.setHttpSessionId("httpSessionId");
        samlSession.setProtocolType(ProtocolType.SAML_COMBICONNECT);

        adAuthentication = new AdAuthentication();
        adAuthentication.setLevel(10);

        adSession = new AdSession();
        adSession.setBsn("PPPPPPPP");
        adSession.setAuthenticationLevel(10);
        adSession.setAuthenticationStatus("success");
    }

    @Test
    void parseArtifactResolveSuccessfulForCombConnect() throws Exception {
        when(samlSessionServiceMock.loadSession(anyString())).thenReturn(samlSession);
        when(adServiceMock.resolveAuthenticationResult(anyString())).thenReturn(adAuthentication);

        ArtifactResolveRequest result = artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveValid));

        assertNotNull(result);
        assertEquals(result.getConnectionEntityId(), samlSession.getConnectionEntityId());
    }

    @Test
    void parseArtifactResolveWithWrongConnectionEntityId() throws SamlSessionException {
        samlSession.setConnectionEntityId("wrongConnectionEntityId");
        when(samlSessionServiceMock.loadSession(anyString())).thenReturn(samlSession);

        SamlParseException exception = assertThrows(SamlParseException.class,
                () -> artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveValid))
        );
        assertEquals("ArtifactResolve not valid", exception.getMessage());
    }

    @Test
    void parseArtifactResolveWithoutArtifact() {
        SamlParseException exception = assertThrows(SamlParseException.class,
                () -> artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveRequestNoArtifact)));

        assertEquals("ArtifactResolve not valid", exception.getMessage());
    }

    @Test
    void parseArtifactResolveNoSignature() throws SamlSessionException {
        when(samlSessionServiceMock.loadSession(anyString())).thenReturn(samlSession);

        SamlParseException exception = assertThrows(SamlParseException.class, () ->
                artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveRequestNoSignature))
        );
        assertEquals("ArtifactResolve not valid", exception.getMessage());
    }

    @Test
    void parseArtifactResolveInvalidVersion() throws SamlSessionException {
        when(samlSessionServiceMock.loadSession(anyString())).thenReturn(samlSession);

        SamlParseException exception = assertThrows(SamlParseException.class, () ->
                artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveRequestInvalidVersion)));

        assertEquals("ArtifactResolve not valid", exception.getMessage());
    }

    @Test
    void parseArtifactResolveSuccessfulBVDRequest() throws Exception {
        samlSession.setProtocolType(ProtocolType.SAML_ROUTERINGSDIENST);
        samlSession.setTransactionId("transactionId");

        when(samlSessionServiceMock.loadSession(anyString())).thenReturn(samlSession);
        ArtifactResolveRequest artifactResolveRequest = artifactResolveService.startArtifactResolveProcess(prepareSoapRequest(artifactResolveValidBVD));

        assertEquals("PPPPPPPP", artifactResolveRequest.getAdAuthentication().getBsn());
    }
}
