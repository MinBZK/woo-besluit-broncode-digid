
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

package nl.logius.digid.dc.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.dc.client.DigidAdminClient;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTest {

    @InjectMocks
    private ServiceService serviceServiceMock;

    @Mock
    private ServiceRepository serviceRepositoryMock;
    @Mock
    private DigidAdminClient digidAdminClientMock;

    @Test
    public void findAllowedService() {
        when(serviceRepositoryMock
            .findAllowedByConnectionAndEntityIdAndServiceUUID(any(Connection.class), anyString(), anyString()))
            .thenReturn(newService());

        Service result = serviceServiceMock.findAllowedService(new Connection(), "entityId", "serviceUUID");

        verify(serviceRepositoryMock, times(1)).findAllowedByConnectionAndEntityIdAndServiceUUID(any(Connection.class), anyString(), anyString());
        assertNotNull(result);
    }

    @Test
    public void findAllowedServiceById() {
        when(serviceRepositoryMock.findByConnectionIdAndEntityId(anyLong(), anyString())).thenReturn(newService());

        Service result = serviceServiceMock.findAllowedServiceById(1L, "serviceEntityId");

        verify(serviceRepositoryMock, times(1)).findByConnectionIdAndEntityId(anyLong(), anyString());
        assertNotNull(result);
    }

    @Test
    public void updateService() {
        when(serviceRepositoryMock.saveAndFlush(any(Service.class))).thenReturn(newService());

        serviceServiceMock.updateService(newService());

        verify(serviceRepositoryMock, times(1)).saveAndFlush(any(Service.class));
    }

    @Test
    public void getServiceByName() {
        Optional<Service> serviceOptional = Optional.of(newService());
        when(serviceRepositoryMock.findByName(anyString())).thenReturn(serviceOptional);

        Service result = serviceServiceMock.getServiceByName("service");

        verify(serviceRepositoryMock, times(1)).findByName(anyString());
        assertEquals(serviceOptional.get().getName(), result.getName());
    }

    @Test
    public void getServiceById() {
        Optional<Service> serviceOptional = Optional.of(newService());
        when(serviceRepositoryMock.findById(anyLong())).thenReturn(serviceOptional);

        Service result = serviceServiceMock.getServiceById(1L);

        verify(serviceRepositoryMock, times(1)).findById(anyLong());
        assertEquals(serviceOptional.get().getId(), result.getId());
    }

    @Test
    public void serviceNotFound() {
        Optional<Service> serviceOptional = Optional.empty();
        when(serviceRepositoryMock.findByName(anyString())).thenReturn(serviceOptional);

        assertThrows(NotFoundException.class, () -> {
            serviceServiceMock.getServiceByName("service");
        });
    }

    @Test
    public void getAllServices() {
        when(serviceRepositoryMock.findAll(any(Pageable.class))).thenReturn(getPageServices());

        Page<Service> result = serviceServiceMock.getAllServices(1, 10);

        verify(serviceRepositoryMock, times(1)).findAll(any(Pageable.class));
        assertNotNull(result);
    }


    @Test
    public void searchAllServices() {
        ServiceSearchRequest ssr = new ServiceSearchRequest();
        doReturn(getPageServices()).when(serviceRepositoryMock).searchAll(ssr, PageRequest.of(1, 10));

        Page<Service> result = serviceServiceMock.searchAllServices(ssr, 1, 10);

        verify(serviceRepositoryMock, times(1)).searchAll(any(ServiceSearchRequest.class), any(Pageable.class));
        assertNotNull(result);
    }

    @Test
    public void saveService() throws JsonProcessingException {
        mockAdmin();
        when(serviceRepositoryMock.saveAndFlush(any(Service.class))).thenReturn(newService());

        serviceServiceMock.createNewService(newService());

        verify(serviceRepositoryMock, times(1)).saveAndFlush(any(Service.class));
    }

    @Test
    public void deleteService() {
        doNothing().when(serviceRepositoryMock).delete(any(Service.class));

        serviceServiceMock.deleteService(newService());

        verify(serviceRepositoryMock, times(1)).delete(any(Service.class));
    }

    @Test
    public void findLegacyIdsById() {
        when(serviceRepositoryMock.findLegacyIdsById(anyLong())).thenReturn(new ArrayList<>());

        Map<String, Object> result = serviceServiceMock.findLegacyIdsById(1L);

        verify(serviceRepositoryMock, times(1)).findLegacyIdsById(anyLong());
        assertNotNull(result);
    }

    private Service newService() {
        Service service = new Service();
        service.setName("SSSSSSSSSSSSSSSS");
        service.setConnectionId(1L);
        return service;
    }

    private Page<Service> getPageServices() {
        Service service = new Service();
        List<Service> serviceList = new ArrayList<>();
        serviceList.add(service);

        return new PageImpl<>(serviceList, PageRequest.of(1,10),10 );

    }

    private void mockAdmin() throws JsonProcessingException {
        String jsonString = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);
        final ObjectNode responseAdmin = ((ObjectNode) actualObj);
        when(digidAdminClientMock.retrieveLegacyServiceIds(Mockito.anyList())).thenReturn(responseAdmin);
    }
}

