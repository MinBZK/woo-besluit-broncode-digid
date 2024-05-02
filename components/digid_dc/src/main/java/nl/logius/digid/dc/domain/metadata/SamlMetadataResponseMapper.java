
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

package nl.logius.digid.dc.domain.metadata;

import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.service.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel="spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SamlMetadataResponseMapper {

    @Mapping(target = "samlMetadata", source = "samlMetadata")
    @Mapping(target = "federationName", source = "connection", qualifiedByName = "federationName")
    @Mapping(target = "minimumReliabilityLevel", source = "service.minimumReliabilityLevel")
    @Mapping(target = "encryptionIdType", source = "service.encryptionIdType")
    @Mapping(target = "appActive", source = "service.appActive")
    @Mapping(target = "appReturnUrl", source = "service.appReturnUrl")
    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "legacyWebserviceId", source = "service.legacyServiceId")
    @Mapping(target = "protocolType", source = "connection.protocolType")
    @Mapping(target = "requestStatus", source = "requestStatus")
    @Mapping(target= "serviceUuid", source = "service.serviceUuid")
    SamlMetadataResponse mapSuccessResponse (String samlMetadata, Connection connection, Service service,
                                             String requestStatus);

    @Mapping(target = "requestStatus", source = "requestStatus")
    @Mapping(target = "errorDescription", source = "errorDescription")
    SamlMetadataResponse mapErrorResponse (String requestStatus, String errorDescription);

    @Named("federationName")
    default String federationName(Connection connection) {
        return connection.getSsoStatus() && connection.getSsoDomain() != null && connection.getSsoDomain().length() != 0 ? connection.getSsoDomain() : null;
    }
}
