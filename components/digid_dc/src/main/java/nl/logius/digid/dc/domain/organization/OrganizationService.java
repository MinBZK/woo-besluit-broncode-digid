
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

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.logius.digid.dc.domain.DropdownItem;
import nl.logius.digid.dc.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationCsvService csvService;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository, OrganizationCsvService csvService) {
        this.organizationRepository = organizationRepository;
        this.csvService = csvService;
    }

    public Organization getOrganizationByName(String name) {
        Optional<Organization> conf = organizationRepository.findByName(name);
        if (!conf.isPresent()) {
            throw new NotFoundException("Could not find organization with name: " + name);
        }
        return conf.get();
    }

    public Organization getOrganizationById(Long id) {
        Optional<Organization> conf = organizationRepository.findById(id);
        if (!conf.isPresent()) {
            throw new NotFoundException("Could not find organization with id: " + id);
        }
        return conf.get();
    }

    public Page<Organization> getAllOrganizations(int pageIndex, int pageSize) {
        return organizationRepository.findAll(PageRequest.of(pageIndex, pageSize));
    }

    public List<DropdownItem> retrieveAll() {
        return organizationRepository.retrieveAll();
    }

    public Page<Organization> searchAllOrganizations(Organization org, int pageIndex, int pageSize) {
        OrganizationRole orgRole = org.getOrganizationRoles().isEmpty() ? new OrganizationRole() : org.getOrganizationRoles().get(0);
        return organizationRepository.searchAll(org, orgRole, PageRequest.of(pageIndex, pageSize));
    }

    public void saveOrganization(Organization organization) {
        organizationRepository.saveAndFlush(organization);
    }

    public void deleteOrganization(Organization organization) {
        organizationRepository.delete(organization);
    }

    public Map<String, Object> updateWithCsv(String file, boolean dryRun) throws JsonProcessingException {
        return csvService.processCsvFile(file, dryRun);
    }
}
