
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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.exception.DienstencatalogusException;
import nl.logius.digid.saml.util.StringMetadataResolver;
import org.mapstruct.MappingTarget;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static nl.logius.digid.saml.domain.metadata.DcMetadataResponseStatus.STATUS_OK;

@Component
public abstract class DcMetadataResponseDecorator implements DcMetadataResponseMapper {

    private static Logger logger = LoggerFactory.getLogger(DcMetadataResponseDecorator.class);

    @Autowired
    @Qualifier("delegate")
    private DcMetadataResponseMapper delegate;

    @Autowired
    private ParserPool parserPool;

    @Override
    public SamlRequest dcMetadataToSamlRequest (@MappingTarget SamlRequest samlRequest, DcMetadataResponse dcMetadataResponse) throws DienstencatalogusException {
        checkRequirements(dcMetadataResponse);
        delegate.dcMetadataToSamlRequest(samlRequest, dcMetadataResponse);
        getEntityDescriptors(samlRequest, dcMetadataResponse);
        return samlRequest;
    }

    @Override
    public AuthenticationRequest dcMetadataToAuthenticationRequest (@MappingTarget AuthenticationRequest authenticationRequest, DcMetadataResponse dcMetadataResponse, String serviceEntityId) throws DienstencatalogusException {
        checkRequirements(dcMetadataResponse);
        delegate.dcMetadataToAuthenticationRequest(authenticationRequest, dcMetadataResponse, serviceEntityId);
        getEntityDescriptors(authenticationRequest, dcMetadataResponse);
        return authenticationRequest;
    }

    private void checkRequirements(DcMetadataResponse dcMetadataResponse) throws DienstencatalogusException {
        if (dcMetadataResponse == null) {
            throw new DienstencatalogusException("Unknown status from digid_dc");
        }

        if (!dcMetadataResponse.getRequestStatus().equals(STATUS_OK.label)) {
            throw new DienstencatalogusException("Metadata from dc not found or not active: " + dcMetadataResponse.getErrorDescription());
        }

        if (dcMetadataResponse.getProtocolType() == null) {
            throw new DienstencatalogusException("No protocol type set");
        }

        if (dcMetadataResponse.getMinimumReliabilityLevel() == null) {
            throw new DienstencatalogusException("Metadata from dc minimum reliability level not set");
        }
    }

    private void getEntityDescriptors(SamlRequest samlRequest, DcMetadataResponse dcMetadataResponse) {
        final String metadata = new String(Base64.getDecoder().decode(dcMetadataResponse.getSamlMetadata()));
        EntityDescriptor serviceEntityDescriptor = parseMetadata(metadata, samlRequest.getServiceEntityId());
        samlRequest.setServiceEntity(serviceEntityDescriptor);

        EntityDescriptor connectionEntityDescriptor = parseMetadata(metadata, samlRequest.getConnectionEntityId());
        samlRequest.setConnectionEntity(connectionEntityDescriptor);
    }

    private EntityDescriptor parseMetadata(String rawMetadata, String entityId) {
        StringMetadataResolver metadataResolver = new StringMetadataResolver();
        try {
            metadataResolver.setParserPool(parserPool);
            metadataResolver.setId("StringMetadataResolver");
            metadataResolver.initialize();
            metadataResolver.addMetadataString(rawMetadata);
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
            return metadataResolver.resolveSingle(criteria);
        } catch (UnmarshallingException | ResolverException | ComponentInitializationException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
