
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.logius.digid.dc.client.DigidAdminClient;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.metadata.CacheService;
import nl.logius.digid.dc.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceMapper serviceMapper;
    private final CacheService cacheService;
    private final DigidAdminClient digidAdminClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ServiceService(ServiceRepository serviceRepository, ServiceMapper serviceMapper, CacheService cacheService, DigidAdminClient digidAdminClient) {
        this.serviceRepository = serviceRepository;
        this.serviceMapper = serviceMapper;
        this.cacheService = cacheService;
        this.digidAdminClient = digidAdminClient;
    }

    public Service serviceExists(Connection connection, String serviceEntityId, String serviceUUID) {
        Optional<Service> service = serviceRepository.findByConnectionAndEntityIdAndServiceUUID(connection, serviceEntityId, serviceUUID);
        return service.orElse(null);
    }

    public Service findAllowedService(Connection connection, String serviceEntityId, String serviceUUID) {
        return serviceRepository.findAllowedByConnectionAndEntityIdAndServiceUUID(connection, serviceEntityId, serviceUUID);
    }

    public Service findAllowedServiceById(Long connectionId, String serviceEntityId) {
        return serviceRepository.findByConnectionIdAndEntityId(connectionId, serviceEntityId);

    }

    public Service updateAttributes(Long id, ServiceDTO serviceDTO) {
        Service updatedService = getServiceById(id);
        return serviceMapper.toUpdatedService(updatedService, serviceDTO);
    }

    public void updateService(Service service) {
        serviceRepository.saveAndFlush(service);

        if (service != null && service.getConnection() != null) {
            cacheService.evictRelatedCacheValues("metadata-response", service.getConnection().getEntityId());
        }
    }

    public Service getServiceByName(String name) {
        Optional<Service> conf = serviceRepository.findByName(name);
        if (conf.isEmpty()) {
            throw new NotFoundException("Could not find service with name: " + name);
        }
        return conf.get();
    }

    public Service getServiceById(Long id) {
        Optional<Service> conf = serviceRepository.findById(id);
        if (conf.isEmpty()) {
            throw new NotFoundException("Could not find service with id: " + id);
        }
        return conf.get();
    }

    public Page<Service> getAllServices(int pageIndex, int pageSize) {
        return serviceRepository.findAll(PageRequest.of(pageIndex, pageSize));
    }

    public Page<Service> searchAllServices(ServiceSearchRequest serviceSearchRequest, int pageIndex, int pageSize) {
        return serviceRepository.searchAll(serviceSearchRequest, PageRequest.of(pageIndex, pageSize));
    }

    public void createNewService(Service service) throws JsonProcessingException {
        service.getCertificates().forEach(cert -> cert.setService(service));
        service.getKeywords().forEach(keyword -> keyword.setService(service));

        var response = digidAdminClient.retrieveLegacyServiceIds(List.of(service.getName())).get("legacy_ids");
        Map<String, Long> serviceLegacyIds = mapper.readValue(mapper.writeValueAsString(response), new TypeReference<>() {});
        service.setLegacyServiceId(serviceLegacyIds.get(service.getName()));

        serviceRepository.saveAndFlush(service);
    }

    public void deleteService(Service service) {
        serviceRepository.delete(service);
    }

    public Map<String, Object> findLegacyIdsById(Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("legacy_service_ids", serviceRepository.findLegacyIdsById(id));

        return response;
    }

    public Service getServiceByClientId(String clientId) {
        return serviceRepository.findFirstByClientId(clientId);
    }
}
