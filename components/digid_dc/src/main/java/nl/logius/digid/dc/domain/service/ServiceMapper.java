
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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel="spring")
public interface ServiceMapper {

    @Mapping(target = "status.id", ignore = true)
    @Mapping(target = "status.createdAt", ignore = true)
    @Mapping(target = "status.updatedAt", ignore = true)
    @Mapping(target = "status.active", source = "serviceDTO.status.active")
    @Mapping(target = "status.activeFrom", source = "serviceDTO.status.activeFrom")
    @Mapping(target = "status.activeUntil", source = "serviceDTO.status.activeUntil")
    @Mapping(target = "serviceUuid", source = "serviceDTO.serviceUuid")
    @Mapping(target = "entityId", source = "serviceDTO.entityId")
    @Mapping(target = "name", source = "serviceDTO.name")
    @Mapping(target = "websiteUrl", source = "serviceDTO.websiteUrl")
    @Mapping(target = "permissionQuestion", source = "serviceDTO.permissionQuestion")
    @Mapping(target = "appActive", source = "serviceDTO.appActive")
    @Mapping(target = "appReturnUrl", source = "serviceDTO.appReturnUrl")
    @Mapping(target = "clientId", source = "serviceDTO.clientId")
    @Mapping(target = "minimumReliabilityLevel", source = "serviceDTO.minimumReliabilityLevel")
    @Mapping(target = "legacyMachtigenId", source = "serviceDTO.legacyMachtigenId")
    @Mapping(target = "newReliabilityLevel", source = "serviceDTO.newReliabilityLevel")
    @Mapping(target = "newReliabilityLevelStartingDate", source = "serviceDTO.newReliabilityLevelStartingDate")
    @Mapping(target = "newReliabilityLevelChangeMessage", source = "serviceDTO.newReliabilityLevelChangeMessage")
    Service toUpdatedService(@MappingTarget Service service, ServiceDTO serviceDTO);
}
