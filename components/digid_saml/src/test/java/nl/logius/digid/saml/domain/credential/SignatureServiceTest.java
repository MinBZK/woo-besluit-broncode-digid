
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

package nl.logius.digid.saml.domain.credential;

import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.metadata.EntranceMetadataService;
import nl.logius.digid.saml.domain.metadata.IdpMetadataService;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, InitializationExtension.class})
@Disabled("Work in progress, DD-4002")
public class SignatureServiceTest {
    @InjectMocks
    private SignatureService signatureService;

    @Mock
    private IdpMetadataService idpMetadataServiceMock;
    @Mock
    private BvdMetadataService bvdMetadataServiceMock;
    @Mock
    private EntranceMetadataService entranceMetadataServiceMock;
    @Mock
    private MockHttpServletRequest  httpServletRequestMock;
    @Mock
    private Endpoint endpoint;

    @BeforeEach
    public void setup() {
        when(idpMetadataServiceMock.getIDPEndpoint()).thenReturn(endpoint);

        EntityDescriptor entityDescriptor = OpenSAMLUtils.buildSAMLObject(EntityDescriptor.class);

        SamlRequest samlRequest = new AuthenticationRequest();
        samlRequest.setConnectionEntity(entityDescriptor);
        samlRequest.setConnectionEntityId("connectionEntityId");
    }

    @Test
    public void verifyRequestSignatureValid() throws SamlValidationException {
        assertDoesNotThrow(() -> signatureService.validateSamlRequest(null, null));
    }
}
