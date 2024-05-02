
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

import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.CsvImportBase;
import nl.logius.digid.dc.domain.service.Service;
import nl.logius.digid.dc.domain.service.ServiceOrganizationRole;
import nl.logius.digid.dc.domain.service.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class OrganizationCsvService extends CsvImportBase {

    private static final Map<String, Integer> UNIQUE_IDENTIFIERS = Map.of("Organisatie OIN",0);
    private static final String SERVICE_FAILED_PREFIX = "Dienst met UUID ";
    private static final Logger logger = LoggerFactory.getLogger(OrganizationCsvService.class);

    private final ServiceRepository serviceRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;

    private boolean organizationExists;
    private boolean serviceOrgRoleValid;

    private Map<String, Map<String, String>> servicesMap;
    private boolean dryRun;


    @Autowired
    public OrganizationCsvService(ServiceRepository serviceRepository, OrganizationRepository organizationRepository, OrganizationRoleRepository organizationRoleRepository ) {
        this.serviceRepository = serviceRepository;
        this.organizationRepository = organizationRepository;
        this.organizationRoleRepository = organizationRoleRepository;
        this.columnSize = 11;
        this.defaultCharSize =  255;
    }

    @Override
    protected void processCsvLine(String[] line, boolean dryRun){
        this.dryRun = dryRun;
        this.servicesMap = new HashMap<>();

        try {
            String oin = extractMandatoryValue(line[0], "OIN Organisatie");  // Mandatory and Unique
            checkOIN(oin, "OIN Organisatie");

            String name = extractMandatoryValue(line[1], "Naam"); // Mandatory
            String description = extractString(line[2], "Omschrijving", false, 255); //Optional
            Boolean active = extractBoolean(line[3], "Organisatie: Actief"); // Mandatory
            ZonedDateTime activeFrom = extractZonedDateTime(line[4], "Organisatie: Datum ingang", true); // Optional
            ZonedDateTime activeUntil = extractZonedDateTime(line[5], "Organisatie: Datum einde", false); // Optional
            OrganizationRoleType organizationRoleType = convertRoleType(line[6]); // Mandatory
            Boolean activeRole = extractBoolean(line[7], "Organisatie Rol: Actief"); // Mandatory
            ZonedDateTime activeFromRole = extractZonedDateTime(line[8], "Organisatie Rol: Datum ingang", true); // Optional
            ZonedDateTime activeUntilRole = extractZonedDateTime(line[9], "Organisatie Rol: Datum einde", false); // Optional
            extractMandatoryValue(line[10], "Diensten", 4096); //Mandatory

            if (!line[10].isEmpty())
                servicesMap = organizeServices(line[10]);

            if (validRow) {
                Organization updateOrganization = checkOrganizationExists(oin);
                processOrganization(updateOrganization, oin, name, description, active, activeFrom, activeUntil);
                processServices(updateOrganization, organizationRoleType, activeRole, activeFromRole, activeUntilRole);
            }
        } catch (Exception e) {
            addResult(FAILED, e.getMessage());
            logger.error("An error has occurred parsing column: {}", e.getMessage());
        }
    }

    private void updateOrganizationRole (String serviceUuid, Service service, String oin, Organization organization, OrganizationRole updateOrganizationRole, ServiceOrganizationRole updateServiceOrganizationRole) {
        if (updateServiceOrganizationRole != null) {
            processServiceOrganizationRole(updateServiceOrganizationRole, serviceUuid, false);
            saveRecord(organization, updateOrganizationRole, service);
            addResult(SUCCEEDED, buildOrganizationRoleMessage(oin, updateOrganizationRole.getType(), serviceUuid, 0));
        } else {
            createServiceOrganizationRole(serviceUuid, service, organization, updateOrganizationRole);
            addResult(SUCCEEDED, buildOrganizationRoleMessage(oin, updateOrganizationRole.getType(), serviceUuid, 1));
        }
    }

    private void createOrganizationRole(String serviceUuid, Service service, String oin, Organization organization, OrganizationRole newOrganizationRole) {
        createServiceOrganizationRole(serviceUuid, service, organization, newOrganizationRole);

        if (!serviceOrgRoleValid) {
            addResult(FAILED, SERVICE_FAILED_PREFIX + serviceUuid + " met indicatie DigiD is al gekoppeld aan een organisatie");
        } else if (organizationExists) {
            addResult(SUCCEEDED, buildOrganizationRoleMessage(oin, newOrganizationRole.getType(), serviceUuid, 2));
        } else {
            addResult(SUCCEEDED, buildOrganizationRoleMessage(oin, newOrganizationRole.getType(), serviceUuid, 3));
        }
    }

    private String buildOrganizationRoleMessage(String oin, OrganizationRoleType organizationRoleType, String serviceUuid, int created) {
        String oinString = "Organisatie OIN: " + oin;
        String orgRoleTypeString = "Organisatie/Rol: " + organizationRoleType;
        String serviceRelationString = "relatie Rol/Dienst voor dienst UUID: " + serviceUuid;

        String message;
        if (created == 0 || created == 3) {
            message = oinString + ", " + orgRoleTypeString + " en de " + serviceRelationString + (created == 0 ? " overschreven" : " aangemaakt");
        } else {
            message = oinString + (created == 2 ? " overschreven, " : " en ") + orgRoleTypeString + (created == 1 ? " overschreven, " : " en de ") + serviceRelationString + " aangemaakt";
        }

        return message;
    }

    private void createServiceOrganizationRole (String serviceUuid, Service service, Organization organization, OrganizationRole organizationRole ) {
        serviceOrgRoleValid = true;

        if (service.getDigid() != null && service.getDigid() && !service.getServiceOrganizationRoles().isEmpty()) {
            serviceOrgRoleValid = false;
        } else {
            ServiceOrganizationRole newServiceOrganizationRole = new ServiceOrganizationRole();
            processServiceOrganizationRole(newServiceOrganizationRole, serviceUuid, true);
            newServiceOrganizationRole.setOrganizationRole(organizationRole);
            newServiceOrganizationRole.setService(service);

            service.addServiceOrganizationRole(newServiceOrganizationRole);
            saveRecord(organization, organizationRole, service);
        }
    }

    private void saveRecord (Organization organisation, OrganizationRole organizationRole, Service service ) {
        if (dryRun) return;

        //TODO Letting organisation save its role automatically (as child object) leads to two OrganizationRole entries in the database for unknown reasons -> save manually for now
        organizationRoleRepository.save(organizationRole);

        organisation.addOrganizationRole(organizationRole);
        organizationRepository.save(organisation);
        serviceRepository.save(service);
    }

    private Organization checkOrganizationExists (String oin) {
        organizationExists = false;
        Optional<Organization> organization = organizationRepository.findByOin(oin);
        Organization updateOrganization = organization.orElseGet(Organization::new);
        organizationExists = organization.isPresent();
        return updateOrganization;
    }

    private OrganizationRole checkOrganizationRoleExists (Organization organization, OrganizationRoleType organizationRoleType) {
        OrganizationRole updateOrganizationRole = null;
        List<OrganizationRole> belongingOrgRoles =  organization.getOrganizationRoles();
        for (OrganizationRole organizationRole : belongingOrgRoles) {
            if( organizationRole.getType() == organizationRoleType ) {
                updateOrganizationRole = organizationRole;
                break;
            }
        }
        return updateOrganizationRole;
    }

    private ServiceOrganizationRole checkServiceOrganizationRoleExists (Service service, OrganizationRole updateOrganizationRole) {
        serviceOrgRoleValid = true;

        List<ServiceOrganizationRole> serviceOrgRoles = service.getServiceOrganizationRoles();
        for (ServiceOrganizationRole serviceOrgRole : serviceOrgRoles) {
            if (serviceOrgRole.getOrganizationRole() == updateOrganizationRole) {
                return serviceOrgRole;
            } else if (serviceOrgRole.getOrganizationRole() != null && service.getDigid()) {
                serviceOrgRoleValid = false;
                return null;
            }
        }
        return null;
    }

    private void processOrganization(Organization organization, String oin, String name, String description, Boolean active, ZonedDateTime activeFrom, ZonedDateTime activeUntil) {
        organization.setOin(oin);
        organization.setName(name);
        organization.setDescription(description);
        if (organizationExists) {
            buildStatus(organization.getStatus(), active, activeFrom, activeUntil);
        } else {
            organization.setStatus(buildStatus(new Status(), active, activeFrom, activeUntil));
        }
    }

    private void processServices(Organization updateOrganization, OrganizationRoleType organizationRoleType, Boolean activeRole, ZonedDateTime activeFromRole, ZonedDateTime activeUntilRole) {
        OrganizationRole updateOrganizationRole;
        for (String serviceUuid : servicesMap.keySet()) {
            Optional<Service> service = serviceRepository.findFirstByServiceUuid(serviceUuid);
            updateOrganizationRole = checkOrganizationRoleExists(updateOrganization, organizationRoleType);

            if (service.isPresent()) {
                if (updateOrganizationRole != null) {
                    ServiceOrganizationRole updateServiceOrganizationRole = checkServiceOrganizationRoleExists(service.get(), updateOrganizationRole);
                    if (!serviceOrgRoleValid) {
                        addResult(FAILED, SERVICE_FAILED_PREFIX + serviceUuid + " met indicatie DigiD is al gekoppeld aan een organisatie");
                        continue;
                    }
                    buildStatus(updateOrganizationRole.getStatus(), updateOrganization.getStatus().isActive(), updateOrganization.getStatus().getActiveFrom(), updateOrganization.getStatus().getActiveUntil());
                    updateOrganizationRole(serviceUuid, service.get(), updateOrganization.getOin(), updateOrganization, updateOrganizationRole, updateServiceOrganizationRole);
                } else {
                    OrganizationRole newOrganizationRole = new OrganizationRole();
                    newOrganizationRole.setType(organizationRoleType);
                    newOrganizationRole.setOrganization(updateOrganization);
                    newOrganizationRole.setStatus(buildStatus(new Status(), activeRole, activeFromRole, activeUntilRole));
                    createOrganizationRole(serviceUuid, service.get(), updateOrganization.getOin(), updateOrganization, newOrganizationRole);
                }
            } else {
                addResult(FAILED, SERVICE_FAILED_PREFIX + serviceUuid + " bestaat niet");
            }
        }
    }

    private void processServiceOrganizationRole(ServiceOrganizationRole serviceOrganizationRole, String serviceUuid, boolean newServiceOrganizationRole) {
        Map<String, String> csvServiceOrganizationRole = servicesMap.get(serviceUuid);

        String csvServiceActiveFrom = csvServiceOrganizationRole.getOrDefault("activeFrom", "");
        String csvServiceActiveUntil = csvServiceOrganizationRole.getOrDefault("activeUntil", "");

        if (newServiceOrganizationRole) {
            serviceOrganizationRole.setStatus(new Status());
        }

        buildStatus(
            serviceOrganizationRole.getStatus(),
            extractBoolean(csvServiceOrganizationRole.get("active"),"Rol Dienst: Actief"),
            extractZonedDateTime(csvServiceActiveFrom, "Rol Dienst: Datum ingang", true),
            extractZonedDateTime(csvServiceActiveUntil, "Rol Dienst: Datum einde", false)
        );
    }

    private Map<String, Map<String, String>> organizeServices (String servicesString) {
        for (String service : servicesString.split(",")) {
            String[] fields = service.split("#");
            Map<String, String> serviceDetailsMap = new HashMap<>();
            serviceDetailsMap.put("active", fields[1]);
            extractBoolean(fields[1], "Rol/Dienst: Actief");

            if (2 < fields.length) {
                serviceDetailsMap.put("activeFrom", fields[2]);
                extractZonedDateTime(fields[2], "Rol/Dienst: Datum ingang", true);
            }

            if (3 < fields.length) {
                serviceDetailsMap.put("activeUntil", fields[3]);
                extractZonedDateTime(fields[3], "Rol/Dienst: Datum einde", false);
            }

            servicesMap.put(fields[0], serviceDetailsMap);
        }
        return servicesMap;
    }

    private OrganizationRoleType convertRoleType(String value) {
        boolean valid = validatePresent(value, "Organisatie Rol Type");
        OrganizationRoleType organizationRoleType = null;
        if (valid) {
            switch (value) {
                case "0" -> organizationRoleType = OrganizationRoleType.DIENSTAANBIEDER;
                case "1" -> organizationRoleType = OrganizationRoleType.ZELFSTANDIGE_AANSLUITHOUDER;
                case "2" -> organizationRoleType = OrganizationRoleType.LEVERANCIER_CLUSTERAANSLUITING;
                case "3" -> organizationRoleType = OrganizationRoleType.LEVERANCIER_ROUTERINGSVOORZIENING;
                default -> addResult(ERROR, INCORRECT + "Organisatie Rol Type" + ": " + value);
            }
        }

        return organizationRoleType;
    }

    @Override
    protected Map<String, Integer> getUniqueIdentifiers() {
        return UNIQUE_IDENTIFIERS;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
