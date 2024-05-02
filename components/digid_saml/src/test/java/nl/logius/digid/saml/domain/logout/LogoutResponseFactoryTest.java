
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

package nl.logius.digid.saml.domain.logout;

import nl.logius.digid.saml.Application;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.helpers.MetadataParser;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@ExtendWith({MockitoExtension.class, SpringExtension.class, InitializationExtension.class})
@SpringBootTest(classes = {Application.class})
public class LogoutResponseFactoryTest {
    private LogoutResponseFactory factory;
    @Autowired
    private ApplicationContext ctx;
    @Mock
    private LogoutRequestModel logoutRequestModelMock;
    @Mock
    private LogoutRequest logoutRequestMock;

    @MockBean(name = "signatureService")
    private SignatureService signatureServiceMock;
    @Value("classpath:saml/test-ca-metadata.xml")
    private Resource stubsMetadataFile;

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getSuccessLogoutResponseTestData() {
        return Stream.of(
                Arguments.of(false, "urn:nl-eid-gdi:1.0:AD:0000000273813120000:entities:0000"),
                Arguments.of(true, "urn:nl-eid-gdi:1.0:TD:00000004183317817000:entities:9000")
        );
    }

    @BeforeEach
    public void setup() {

        when(logoutRequestMock.getID()).thenReturn("ID-from-logout-request");
        when(logoutRequestModelMock.getLogoutRequest()).thenReturn(logoutRequestMock);
        when(logoutRequestModelMock.getConnectionEntity()).thenReturn(MetadataParser.readMetadata(stubsMetadataFile, "urn:nl-eid-gdi:1:0:entities:00000008888888888000"));

        final NameID nameId = OpenSAMLUtils.buildSAMLObject(NameID.class);
        nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        nameId.setValue("test");
        SamlSession mockSamlSession = new SamlSession(1L);
        //mockSamlSession.setEntranceSession(FALSE);
        mockSamlSession.setProtocolType(ProtocolType.SAML_ROUTERINGSDIENST);
        factory = new LogoutResponseFactory(logoutRequestModelMock, ctx);
    }

    @ParameterizedTest
    @MethodSource("getSuccessLogoutResponseTestData")
    public void successLogoutResponseTest(boolean isEntranceSession, String issuer) throws SamlValidationException {
        when(logoutRequestModelMock.isEntranceSession()).thenReturn(isEntranceSession);
        doNothing().when(signatureServiceMock).signSAMLObject(any(SignableSAMLObject.class));

        LogoutResponse logoutResponse = factory.getLogoutResponse();

        assertNotNull(logoutResponse.getID());
        assertEquals(SAMLVersion.VERSION_20, logoutResponse.getVersion());
        assertNotNull(logoutResponse.getIssueInstant());
        assertNotNull(logoutResponse.getDestination());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", logoutResponse.getDestination());
        assertEquals("ID-from-logout-request", logoutResponse.getInResponseTo());
        assertNotNull(logoutResponse.getStatus().getStatusCode());
        assertEquals(StatusCode.SUCCESS, logoutResponse.getStatus().getStatusCode().getValue());
    }
}
