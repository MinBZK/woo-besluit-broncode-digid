
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

package nl.logius.digid.saml.domain.metadata;

import nl.logius.digid.saml.exception.MetadataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetadataControllerTest {
    @Mock
    private IdpMetadataService idpMetadataServiceMock;
    @Mock
    private BvdMetadataService bvdMetadataServiceMock;
    @Mock
    private EntranceMetadataService entranceMetadataServiceMock;
    @InjectMocks
    private MetadataController metadataControllerMock;

    @Test
    public void idpMetadataTest() throws MetadataException {
        String idpMetadata = "idpMetadata";
        when(idpMetadataServiceMock.getMetadata()).thenReturn(idpMetadata);

        String result = metadataControllerMock.metadata();

        assertNotNull(result);
        assertEquals(idpMetadata, result);
        verify(idpMetadataServiceMock, times(1)).getMetadata();
    }

    @Test
    public void bvdMetadataTest() throws MetadataException {
        String bvdMetadata = "bvdMetadata";
        when(bvdMetadataServiceMock.getMetadata()).thenReturn(bvdMetadata);

        String result = metadataControllerMock.bvdMetadata();

        assertNotNull(result);
        assertEquals(bvdMetadata, result);
        verify(bvdMetadataServiceMock, times(1)).getMetadata();
    }

    @Test
    public void entranceMetadataTest() throws MetadataException {
        String entranceMetadata = "entranceMetadata";
        when(entranceMetadataServiceMock.getMetadata()).thenReturn(entranceMetadata);

        String result = metadataControllerMock.entranceMetadata();

        assertNotNull(result);
        assertEquals(entranceMetadata, result);
        verify(entranceMetadataServiceMock, times(1)).getMetadata();
    }
}
