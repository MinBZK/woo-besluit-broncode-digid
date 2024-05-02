
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

package nl.logius.digid.saml.domain.metadata;

import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.exception.DienstencatalogusException;
import org.mapstruct.*;

@Mapper(componentModel="spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@DecoratedWith(DcMetadataResponseDecorator.class)
public interface DcMetadataResponseMapper {

    @Mapping(target = "federationName", source = "dcMetadataResponse.federationName")
    @Mapping(target = "legacyWebserviceId", source = "dcMetadataResponse.legacyWebserviceId")
    @Mapping(target = "serviceUuid", source = "dcMetadataResponse.serviceUuid")
    @Mapping(target = "permissionQuestion", source = "dcMetadataResponse.permissionQuestion")
    SamlRequest dcMetadataToSamlRequest (@MappingTarget SamlRequest samlRequest, DcMetadataResponse dcMetadataResponse) throws DienstencatalogusException;

    @Mapping(target = "federationName", source = "dcMetadataResponse.federationName")
    @Mapping(target = "legacyWebserviceId", source = "dcMetadataResponse.legacyWebserviceId")
    @Mapping(target = "serviceUuid", source = "dcMetadataResponse.serviceUuid")
    @Mapping(target = "permissionQuestion", source = "dcMetadataResponse.permissionQuestion")
    @Mapping(target = "appActive", source = "dcMetadataResponse.appActive")
    @Mapping(target = "appReturnUrl", source = "dcMetadataResponse.appReturnUrl")
    @Mapping(target = "serviceName", source = "dcMetadataResponse.serviceName")
    @Mapping(target = "entityId", source = "serviceEntityId")
    @Mapping(target = "minimumRequestedAuthLevel", source = "dcMetadataResponse.minimumReliabilityLevel")
    @Mapping(target = "encryptionIdType", source = "dcMetadataResponse.encryptionIdType")
    @Mapping(target = "protocolType", source = "dcMetadataResponse.protocolType")
    AuthenticationRequest dcMetadataToAuthenticationRequest (@MappingTarget AuthenticationRequest authenticationRequest, DcMetadataResponse dcMetadataResponse, String serviceEntityId) throws DienstencatalogusException;
}
