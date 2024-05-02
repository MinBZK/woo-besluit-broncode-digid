
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationServiceMock;
    @InjectMocks
    private OrganizationController controllerMock;

    @Test
    public void getOrganizationByName() {
        when(organizationServiceMock.getOrganizationByName(anyString())).thenReturn(newOrganization());

        Organization result = controllerMock.getByName("test");

        assertEquals(newOrganization().getName(), result.getName());
        verify(organizationServiceMock, times(1)).getOrganizationByName(anyString());
        assertNotNull(result);
    }

    @Test
    public void getOrganizationById() {
        when(organizationServiceMock.getOrganizationById(1L)).thenReturn(newOrganization());

        Organization result = controllerMock.getById(1L);

        assertEquals(newOrganization().getName(), result.getName());
        verify(organizationServiceMock, times(1)).getOrganizationById(anyLong());
        assertNotNull(result);
    }

    @Test
    public void organizationNameNotFound() {
        when(organizationServiceMock.getOrganizationByName(anyString())).thenThrow(NotFoundException.class);

        assertThrows(NotFoundException.class, () -> {
            controllerMock.getByName("test");
        });
    }

    @Test
    public void organizationIdNotFound() {
        when(organizationServiceMock.getOrganizationById(anyLong())).thenThrow(NotFoundException.class);

        assertThrows(NotFoundException.class, () -> {
            controllerMock.getById(1L);
        });
    }

    @Test
    public void getAllOrganizations() {
        when(organizationServiceMock.getAllOrganizations(1, 10)).thenReturn(getPageOrganizations());

        Page<Organization> result = controllerMock.getAll(1, 10);

        verify(organizationServiceMock, times(1)).getAllOrganizations(anyInt(), anyInt());
        assertNotNull(result);
    }

    @Test
    public void createOrganization() {
        doNothing().when(organizationServiceMock).saveOrganization(any(Organization.class));

        Organization result = controllerMock.create(new Organization());

        verify(organizationServiceMock, times(1)).saveOrganization(any(Organization.class));
        assertNotNull(result);
    }

    @Test
    public void updateOrganization() {
        Organization organization = new Organization();
        organization.setId(1L);
        doNothing().when(organizationServiceMock).saveOrganization(any(Organization.class));

        Organization result = controllerMock.update(1L, organization);

        verify(organizationServiceMock, times(1)).saveOrganization(any(Organization.class));
        assertNotNull(result);
    }

    @Test
    public void removeOrganization() {
        when(organizationServiceMock.getOrganizationById(anyLong())).thenReturn(newOrganization());
        doNothing().when(organizationServiceMock).deleteOrganization(any(Organization.class));

        controllerMock.remove(1L);

        verify(organizationServiceMock, times(1)).deleteOrganization(any(Organization.class));
        verify(organizationServiceMock, times(1)).getOrganizationById(anyLong());
    }

    private Organization newOrganization() {
        Organization organization = new Organization();
        organization.setName("test");
        return organization;
    }

    private Page<Organization> getPageOrganizations() {
        Organization organization = new Organization();
        List<Organization> organizationList = new ArrayList<>();

        organizationList.add(organization);
        return new PageImpl<>(organizationList);
    }
}
