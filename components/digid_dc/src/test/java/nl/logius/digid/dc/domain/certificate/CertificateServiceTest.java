
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

import nl.logius.digid.dc.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepositoryMock;

    @InjectMocks
    private CertificateService certificateServiceMock;

    @Test
    public void getCertificate() {
        Certificate certificate = new Certificate();
        certificate.setId(1L);
        Optional<Certificate> certificateOptional = Optional.of(certificate);
        when(certificateRepositoryMock.findById(anyLong())).thenReturn(certificateOptional);

        Certificate result = certificateServiceMock.getCertificate(anyLong());

        verify(certificateRepositoryMock, times(1)).findById(anyLong());
        assertEquals(certificateOptional.get().getId(), result.getId());
    }

    @Test
    public void certificateNotFound() {
        Optional<Certificate> certificateOptional = Optional.empty();
        when(certificateRepositoryMock.findById(anyLong())).thenReturn(certificateOptional);

        assertThrows(NotFoundException.class, () -> {
            certificateServiceMock.getCertificate(anyLong());
        });
    }

    @Test
    public void getAllCertificates() {
        when(certificateRepositoryMock.findAll(PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "activeUntil")))).thenReturn(getPageCertificates());

        Page<Certificate> result = certificateServiceMock.getAllCertificates(1, 10);

        verify(certificateRepositoryMock, times(1)).findAll(PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "activeUntil")));
        assertNotNull(result);
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getContent().size());
    }

    @Test
    public void searchAll()  {
        CertSearchRequest csr = new CertSearchRequest();
        when(certificateRepositoryMock.searchAll(csr, PageRequest.of(1, 10))).thenReturn(getPageCertificates());

        Page<Certificate> result = certificateServiceMock.searchAll(csr, 1, 10);

        assertNotNull(result);
    }

    private Page<Certificate> getPageCertificates() {
        ZonedDateTime currentDate = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam"));

        Certificate certificate1 = new Certificate();
        certificate1.setId(1L);
        certificate1.setActiveUntil(currentDate.plusDays(5));

        Certificate certificate2 = new Certificate();
        certificate2.setId(2L);
        certificate2.setActiveUntil(currentDate.plusDays(2));

        List<Certificate> certificateList = new ArrayList<>();
        certificateList.add(certificate1);
        certificateList.add(certificate2);

        return new PageImpl<>(certificateList);
    }
}
