
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

package nl.logius.digid.dc.domain.organization;

import nl.logius.digid.dc.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {
    @Mock
    private OrganizationRepository repositoryMock;
    @InjectMocks
    private OrganizationService organizationServiceMock;

    @Test
    public void getOrganizationByName() {
        Optional<Organization> organizationOptional = Optional.of(newOrganization());
        when(repositoryMock.findByName(anyString())).thenReturn(organizationOptional);

        Organization result = organizationServiceMock.getOrganizationByName("organization");

        verify(repositoryMock, times(1)).findByName(anyString());
        assertEquals(organizationOptional.get().getName(), result.getName());
    }

    @Test
    public void getOrganizationById() {
        Optional<Organization> organizationOptional = Optional.of(newOrganization());
        when(repositoryMock.findById(anyLong())).thenReturn(organizationOptional);

        Organization result = organizationServiceMock.getOrganizationById(1L);

        verify(repositoryMock, times(1)).findById(anyLong());
        assertEquals(organizationOptional.get().getId(), result.getId());
    }

    @Test
    public void organizationNotFound() {
        Optional<Organization> organizationOptional = Optional.empty();
        when(repositoryMock.findByName(anyString())).thenReturn(organizationOptional);

        assertThrows(NotFoundException.class, () -> {
            organizationServiceMock.getOrganizationByName("organization");
        });
    }

    @Test
    public void getAllOrganizations() {
        when(repositoryMock.findAll(any(Pageable.class))).thenReturn(getPageOrganizations());

        Page<Organization> result = organizationServiceMock.getAllOrganizations(1, 10);

        verify(repositoryMock, times(1)).findAll(any(Pageable.class));
        assertNotNull(result);
    }

    @Test
    public void searchAllOrganizations() {
        when(repositoryMock.searchAll(any(Organization.class), any(OrganizationRole.class), any(Pageable.class))).thenReturn(getPageOrganizations());

        Page<Organization> result = organizationServiceMock.searchAllOrganizations(newOrganization(), 1, 10);

        verify(repositoryMock, times(1)).searchAll(any(Organization.class), any(OrganizationRole.class), any(Pageable.class));
        assertNotNull(result);
    }

    @Test
    public void saveOrganization() {
        when(repositoryMock.saveAndFlush(any(Organization.class))).thenReturn(newOrganization());

        organizationServiceMock.saveOrganization(newOrganization());

        verify(repositoryMock, times(1)).saveAndFlush(any(Organization.class));
    }

    @Test
    public void deleteOrganization() {
        doNothing().when(repositoryMock).delete(any(Organization.class));

        organizationServiceMock.deleteOrganization(newOrganization());

        verify(repositoryMock, times(1)).delete(any(Organization.class));
    }


    private Organization newOrganization() {
        Organization organization = new Organization();
        organization.setId(1L);
        organization.setName("name");
        return organization;
    }

    private Page<Organization> getPageOrganizations() {
        Organization organization = new Organization();
        List<Organization> organizationList = new ArrayList<>();
        organizationList.add(organization);
        return new PageImpl<>(organizationList);

    }

}
