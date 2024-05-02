
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

package nl.logius.digid.dc.domain.metadata;

import nl.logius.digid.dc.exception.CollectSamlMetadataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetadataControllerTest {

    @Mock
    private MetadataRetrieverService metadataRetrieverServiceMock;
    @Mock
    private MetadataProcessorService metadataProcessorServiceMock;
    @InjectMocks
    private MetadataController controllerMock;

    @Test
    public void findAllByConnectionId() {
        when(metadataRetrieverServiceMock.getAllSamlMetadataById(anyLong(), anyInt(), anyInt())).thenReturn(MetadataProcessHelper.getPageSamlMetadataProcessResult());

        Page<SamlMetadataProcessResult> result = controllerMock.findAllByConnectionId(1, 10, 1L);

        verify(metadataRetrieverServiceMock, times(1)).getAllSamlMetadataById(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
    }

    @Test
    public void findBySamlMetadataProcessResultId() {
        when(metadataRetrieverServiceMock.getSamlMetadataById(anyLong(), anyInt(), anyInt())).thenReturn(MetadataProcessHelper.getPageSamlMetadataProcessError());

        Page<SamlMetadataProcessError> result = controllerMock.findBySamlMetadataProcessResultId(1, 10, 1L);

        verify(metadataRetrieverServiceMock, times(1)).getSamlMetadataById(anyLong(), anyInt(), anyInt());
        assertNotNull(result);
    }

    @Test
    public void getProcessedMetadata() {
        when(metadataRetrieverServiceMock.getProcessedMetadata(anyLong())).thenReturn("metadata");

        String result = controllerMock.getProcessedMetadata(1L);

        verify(metadataRetrieverServiceMock, times(1)).getProcessedMetadata(anyLong());
        assertNotNull(result);
    }

    @Test
    public void resolveSamlMetadata() {
        SamlMetadataRequest request = new SamlMetadataRequest();
        SamlMetadataResponse response = new SamlMetadataResponse();
        response.setRequestStatus("OK");
        when(metadataRetrieverServiceMock.resolveSamlMetadata(any(SamlMetadataRequest.class))).thenReturn(response);

        SamlMetadataResponse result = controllerMock.resolveMetadata(request);

        verify(metadataRetrieverServiceMock, times(1)).resolveSamlMetadata(any(SamlMetadataRequest.class));
        assertEquals(result.getRequestStatus(), response.getRequestStatus());
    }

    @Test
    public void collectMetadata() throws CollectSamlMetadataException {
        Map<String, String> collectMetadata = new HashMap<>();
        collectMetadata.put("count", "1");
        when(metadataProcessorServiceMock.collectSamlMetadata(anyString())).thenReturn(collectMetadata);

        Map<String, String> result = controllerMock.collectMetadata("id");

        verify(metadataProcessorServiceMock, times(1)).collectSamlMetadata(anyString());
        assertEquals(result.size(), collectMetadata.size());
        assertNotNull(result);
    }

    @Test
    public void failedCollectingMetadata() throws CollectSamlMetadataException {
        when(metadataProcessorServiceMock.collectSamlMetadata(anyString())).thenThrow(CollectSamlMetadataException.class);
        assertThrows(CollectSamlMetadataException.class, () -> {
            controllerMock.collectMetadata(anyString());
        });
    }
}
