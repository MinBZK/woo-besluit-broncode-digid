
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.client.DigidAdminClient;
import nl.logius.digid.dc.domain.CsvImportBase;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.connection.ConnectionRepository;
import nl.logius.digid.dc.domain.metadata.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class CsvService extends CsvImportBase {
    private static final Map<String, Integer> UNIQUE_IDENTIFIERS = Map.of("Service UUID", 2);
    private static final Logger logger = LoggerFactory.getLogger(CsvService.class);

    private final DigidAdminClient digidAdminClient;
    private final ServiceRepository serviceRepository;
    private final ConnectionRepository connectionRepository;
    private final CacheService cacheService;

    private String currentUUID;
    private Map<String, String> serviceParentChildren;
    private Map<Service, CsvLoggingInfo> services;

    @Autowired
    public CsvService(ServiceRepository serviceRepository, DigidAdminClient digidAdminClient, ConnectionRepository connectionRepository, CacheService cacheService) {
        this.serviceRepository = serviceRepository;
        this.digidAdminClient = digidAdminClient;
        this.connectionRepository = connectionRepository;
        this.cacheService = cacheService;
        this.columnSize = 21;
        this.defaultCharSize =  255;
    }

    @Override
    public Map<String, Object> processCsvFile(String encodedCsvData, boolean dryRun) throws JsonProcessingException {
        services = new HashMap<>();
        serviceParentChildren = new HashMap<>();
        Map<String, Object> result = super.processCsvFile(encodedCsvData, dryRun);
        if (!services.isEmpty()) {
            retrieveLegacyServiceIds();
            saveAll(dryRun);
            processServiceParentChildren(serviceParentChildren, dryRun);
        }
        return result;
    }

    @Override
    protected void processCsvLine(String[] line, boolean dryRun) {
        try {
            validRow = true;
            currentUUID = "";
            // Read line 3 first because it's unique and used for logging/showing messages
            currentUUID = extractMandatoryValue(line[2], "Service UUID", 255);
            checkUUIDFormat(currentUUID, "Service UUID");

            Boolean digid = extractBoolean(line[9], "Indicatie DigiD");
            String connectionEntityId = extractString(line[0], "Aansluiting - entity ID", digid, 255);
            String serviceEntityId = extractMandatoryValue(line[1], "Entity ID dienst", 255);

            Optional<Service> service = serviceRepository.findFirstByServiceUuid(currentUUID);
            Service updateService = service.orElseGet(Service::new);

            String name = extractMandatoryValue(line[3], "Naam", 255);
            Integer minimumLevel = extractMinimumReliabilityLevel(line[4], true);
            String encryptTypeString = extractStringEnum(line[5], "Soort encryptie", EncryptionType.getLabels().toArray(new String[0]), true);
            EncryptionType encryptType = encryptTypeString.equals("") ? null : EncryptionType.valueOfLabel(encryptTypeString);
            Integer newLevel = extractIntegerList(line[6], "Nieuw betrouwbaarheidsniveau", LevelOfAssurance.getLabels(), false);
            ZonedDateTime newLevelDate = extractZonedDateTime(line[7], "Datum ingang nieuw betrouwbaarheidsniveau", newLevel != null);
            String newLevelmessage = extractString(line[8], "Wijzigingsbericht nieuw betrouwbaarheidsniveau", newLevel != null, 255);
            String permission = extractString(line[10], "Toestemmingsvraag", digid, 255);
            Boolean machtigen = extractBoolean(line[11], "Indicatie Machtigen");
            Integer pos = extractInteger(line[12], "Weergavevolgorde", machtigen);
            String authTypeString = (line[13].isEmpty() && !machtigen) ? "" : extractStringEnum(line[13], "Soort gemachtigde", AuthorizationType.getLabels().toArray(new String[0]), machtigen);
            AuthorizationType authType = authTypeString.equals("") ? null : AuthorizationType.valueOfLabel(authTypeString);
            Integer duration = extractInteger(line[14], "Looptijd machtigingsaanvraag", machtigen);
            String description = extractString(line[15], "Omschrijving", machtigen, 300);
            String explanation = extractString(line[16], "Toelichting", machtigen, 2000);
            Boolean active = extractBoolean(line[17], "Geldigheid");
            ZonedDateTime activeFrom = extractZonedDateTime(line[18], "Geldigheid: datum ingang", machtigen);
            ZonedDateTime activeUntil = extractZonedDateTime(line[19], "Geldigheid: datum einde", false);

            checkUniqueAttributes(currentUUID, serviceEntityId);

            if (validRow) {
                if (!validateConnection(updateService, connectionEntityId)) {
                    return;
                }

                if (!validateDigiD(updateService, digid)) {
                    return;
                }

                updateService.setEntityId(serviceEntityId);
                updateService.setServiceUuid(currentUUID);
                updateService.setName(name);
                updateService.setMinimumReliabilityLevel(minimumLevel);
                updateService.setEncryptionIdType(encryptType);
                updateService.setNewReliabilityLevel(newLevel);
                updateService.setNewReliabilityLevelChangeMessage(newLevelmessage);
                updateService.setNewReliabilityLevelStartingDate(newLevelDate);
                updateService.setDigid(digid);
                updateService.setPermissionQuestion(permission);
                updateService.setMachtigen(machtigen);
                updateService.setPosition(pos);
                updateService.setAuthorizationType(authType);
                updateService.setDurationAuthorization(duration);
                updateService.setDescription(description);
                updateService.setExplanation(explanation);
                updateService.getStatus().setActive(active);
                updateService.getStatus().setActiveFrom(activeFrom);
                updateService.getStatus().setActiveUntil(activeUntil);

                // CsvLoggingInfo is used for showing executed actions to user
                services.put(updateService, new CsvLoggingInfo(currentRow, service.isEmpty()));

                if (!line[20].isEmpty())
                    serviceParentChildren.put(updateService.getServiceUuid(), line[20]);
            }

        } catch (Exception e) {
            addResult(FAILED, e.getMessage());
            logger.error("An error has occurred parsing column: {}", e.getMessage());
        }
    }

    private void processServiceParentChildren(Map<String,  String> map, boolean dryRun) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            Optional<Service> childService = serviceRepository.findFirstByServiceUuid(entry.getKey());
            if (childService.isPresent() && entry.getValue() != null) {
                for (String relation : entry.getValue().split(",")) {
                    String[] fields = relation.split("#", 5);

                    Optional<Service> parentService = serviceRepository.findFirstByServiceUuid(fields[0]);

                    Status status = new Status(
                        extractBoolean(fields[2], "Dienstenset: geldigheid"),
                        extractZonedDateTime(fields[3], "Dienstenset:  datum ingang", true),
                        extractZonedDateTime(fields[4], "Dienstenset: datum einde", false)
                    );

                    parentService.ifPresent(service -> processChildService(childService.get(), service, ServiceRelationType.valueOfLabel(fields[1]), status, dryRun));
                }
            }
        }
    }

    private void processChildService(Service parentService, Service childService, ServiceRelationType serviceRelationType, Status status, boolean dryRun) {
        if (serviceParentChildExists(childService, parentService)) {
            ServiceParentChild serviceParentChild = retrieveServiceParentChild(childService, parentService);
            if (serviceParentChild != null) {
                updateServiceParentChild(serviceParentChild, serviceRelationType, status);
            }
        } else if (!serviceParentChildExists(parentService, childService)) {
            if (!parentService.addChildService(childService, serviceRelationType, status)) {
                addResult(FAILED, "DIENSTENSET niet aangemaakt. Een dienst kan niet zowel een child als een parent zijn: " + parentService.getServiceUuid() + ", " +childService.getServiceUuid());
            }
        } else {
            addResult(FAILED, "Circular dependency is niet toegestaan. DIENSTENSET: " + parentService.getServiceUuid() + ", " +childService.getServiceUuid());
        }

        if (!dryRun) {
            serviceRepository.save(parentService);
        }
    }

    private boolean serviceParentChildExists(Service childService, Service parentService) {
        for ( ServiceParentChild serviceParentChild : parentService.getChildServices() ) {
            Optional<Long> childServiceId = serviceParentChild.getChildServiceId();

            if (childServiceId.isPresent() && childServiceId.get().equals(childService.getId())) {
                return true;
            }
        }
        return false;
    }

    private ServiceParentChild retrieveServiceParentChild(Service childService, Service parentService) {
        for ( ServiceParentChild serviceParentChild : parentService.getChildServices() ) {
            Optional<Long> childServiceId = serviceParentChild.getChildServiceId();

            if (childServiceId.isPresent() && childServiceId.get().equals(childService.getId())) {
                return serviceParentChild;
            }
        }
        return null;
    }

    private void updateServiceParentChild (ServiceParentChild serviceParentChild, ServiceRelationType serviceRelationType, Status status) {
        Status updateStatus = serviceParentChild.getStatus();
        updateStatus.setActive(status.isActive());
        updateStatus.setActiveFrom(status.getActiveFrom());
        updateStatus.setActiveUntil(status.getActiveUntil());
        serviceParentChild.setType(serviceRelationType);
    }

    private void retrieveLegacyServiceIds() throws JsonProcessingException {
        // Duplicate names will get the same legacy id in response -> use distinct
        List<String> servicesNames = services.keySet().stream()
            .map(Service::getName)
            .distinct()
            .toList();
        JsonNode legacyIdsJson = digidAdminClient.retrieveLegacyServiceIds(servicesNames).get("legacy_ids");

        ObjectMapper mapper = new ObjectMapper();
        // Map json response by <service name, legacy id>
        Map<String, Long> serviceLegacyIds = mapper.readValue(mapper.writeValueAsString(legacyIdsJson), new TypeReference<>() {});
        services.keySet().forEach(service -> service.setLegacyServiceId(serviceLegacyIds.get(service.getName())));
    }

    private void saveAll(boolean dryRun) {
        services.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getValue().getRow()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new))
            .forEach((k, v) -> succeededRows.add("Regel: " + v.getRow() + " UUID: " + k.getServiceUuid() + " " + (v.isNew() ? "Aangemaakt" : "Overschreven")));

        for (Optional<String> connectionEntityId : services.keySet().stream().map(Service::getConnectionEntityID).distinct().toList()) {
            connectionEntityId.ifPresent(id -> cacheService.evictRelatedCacheValues("metadata-response", id));
        }

        if (!dryRun) {
            serviceRepository.saveAll(services.keySet());
        }
    }

    private boolean validateConnection(Service service, String connectionEntityId) {
        if (connectionEntityId == null) {
            return true;
        }

        Optional<Connection> con = connectionRepository.findByEntityId(connectionEntityId);
        if (con.isPresent()) {
            service.setConnectionId(con.get().getId());
            return true;
        } else {
            addResult(FAILED, "Geen aansluiting gevonden met entityID: " + connectionEntityId);
            return false;
        }
    }

    private boolean validateDigiD(Service updateService, Boolean digid) {
        boolean isInvalid = updateService.getDigid() != null
            && !updateService.getDigid()
            && digid
            && updateService.getServiceOrganizationRoles().size() > 1;
        if (isInvalid) {
            addResult(FAILED, "Indicatie DigiD niet toegestaan, dienst is gekoppeld aan meerdere organisaties");
        }
        return !isInvalid;
    }

    private Integer extractMinimumReliabilityLevel(String value, Boolean required) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, "Minimum betrouwbaarheidsniveau"))
            return  null;

        return extractIntegerList(value, "Minimum betrouwbaarheidsniveau", LevelOfAssurance.getLabels(), required);
    }

    private void checkUniqueAttributes(String serviceUUID, String entityId) {
        List<Service> servicesDb = serviceRepository.findServicesByUuidAndEntityId(serviceUUID, entityId);

        if (!servicesDb.isEmpty()) {
            addResult(FAILED, "UUID is al in gebruik bij andere dienst");
            validRow = false;
        }
    }

    @Override
    protected String failedPrefix() {
        return "Regel: " + currentRow + " UUID: " + currentUUID + " Foutbericht: ";
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
