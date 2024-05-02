
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

package nl.logius.digid.dc.domain.certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificateControllerTest {

    @InjectMocks
    private CertificateController controllerMock;

    @Mock
    private CertificateService certificateServiceMock;

    @Test
    public void getCertificateById() {
        when(certificateServiceMock.getCertificate(anyLong())).thenReturn(getCertificate());

        Certificate result = controllerMock.getById(anyLong());

        verify(certificateServiceMock, times(1)).getCertificate(anyLong());
        assertEquals("test", result.getCachedCertificate());
    }

    @Test
    public void getAllCertificates() {
        when(certificateServiceMock.getAllCertificates(anyInt(), anyInt())).thenReturn(getPageCertificates());

        Page<Certificate> result = controllerMock.getAll(anyInt(), anyInt());

        verify(certificateServiceMock, times(1)).getAllCertificates(anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());
    }

    @Test
    public void getAllCertificatesBasedOnConditions() {
        CertSearchRequest request = new CertSearchRequest();
        when(certificateServiceMock.searchAll(request, 1, 10)).thenReturn(getPageCertificates());

        Page<Certificate> result = controllerMock.search(request, 1, 10);

        verify(certificateServiceMock, times(1)).searchAll(any(CertSearchRequest.class), anyInt(), anyInt());
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());
    }

    private Certificate getCertificate() {
        Certificate certificate = new Certificate();
        certificate.setCachedCertificate("test");
        return certificate;
    }

    private Page<Certificate> getPageCertificates() {
        Certificate certificate1 = new Certificate();
        certificate1.setId(1L);

        Certificate certificate2 = new Certificate();
        certificate2.setId(2L);

        List<Certificate> certificateList = new ArrayList<>();
        certificateList.add(certificate1);
        certificateList.add(certificate2);

        return new PageImpl<>(certificateList);
    }

}
