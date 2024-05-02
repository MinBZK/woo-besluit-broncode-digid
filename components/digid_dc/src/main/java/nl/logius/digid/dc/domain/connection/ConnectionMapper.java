
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

package nl.logius.digid.dc.domain.connection;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.Collection;
import java.util.List;

@Mapper(componentModel="spring")
public interface ConnectionMapper {

    @Mapping(target = "name", source = "connectionDTO.name")
    @Mapping(target = "websiteUrl", source = "connectionDTO.websiteUrl")
    @Mapping(target = "status.id", ignore = true)
    @Mapping(target = "status.createdAt", ignore = true)
    @Mapping(target = "status.updatedAt", ignore = true)
    @Mapping(target = "status.active", source = "connectionDTO.status.active")
    @Mapping(target = "status.activeFrom", source = "connectionDTO.status.activeFrom")
    @Mapping(target = "status.activeUntil", source = "connectionDTO.status.activeUntil")
    @Mapping(target = "entityId", source = "connectionDTO.entityId")
    @Mapping(target = "protocolType", source = "connectionDTO.protocolType")
    @Mapping(target = "samlMetadata", source = "connectionDTO.samlMetadata")
    @Mapping(target = "metadataUrl", source = "connectionDTO.metadataUrl")
    @Mapping(target = "organizationRole", source = "connectionDTO.organizationRole")
    @Mapping(target = "ssoDomain", source = "connectionDTO.ssoDomain")
    @Mapping(target = "ssoStatus", source = "connectionDTO.ssoStatus")
    Connection toUpdatedConnection(@MappingTarget Connection connection, ConnectionDTO connectionDTO);

    @Mapping(target = "id", source = "connection.id")
    @Mapping(target = "name", source = "connection.name")
    @Mapping(target = "entityId", source = "connection.entityId")
    @Mapping(target = "status", source = "connection.status")
    @Mapping(target = "organizationId", source = "connection.organizationId")
    @Mapping(target = "organizationName", source = "connection.organization.name")
    @Mapping(target = "organizationOin", source = "connection.organization.oin")
    @Mapping(target = "organizationRoleId", source = "connection.organizationRoleId")
    ConnectionResponse toConnectionResponse(Connection connection);

    List<ConnectionResponse> toConnectionResponse(Collection<Connection> connections);
}
