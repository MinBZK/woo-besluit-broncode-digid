
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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.connection.ConnectionService;
import nl.logius.digid.dc.domain.service.ServiceService;
import nl.logius.digid.dc.util.OpenSAMLUtils;
import nl.logius.digid.dc.util.StringMetadataResolver;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static nl.logius.digid.dc.domain.metadata.MetadataResponseStatus.*;


/**
 * Service responsible for retrieving metadata from the database.
 */
@Service
public class MetadataRetrieverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataRetrieverService.class);
    private static final String SAML_SERVICE_UUID_NAME = "urn:nl-eid-gdi:1.0:ServiceUUID";

    private final ParserPool parserPool;
    private final SamlMetadataProcessResultRepository resultRepository;
    private final SamlMetadataProcessErrorRepository errorRepository;
    private final ConnectionService connectionService;
    private final ServiceService serviceService;
    private final SamlMetadataResponseMapper metadataResponseMapper;
    private final CacheService cacheService;

    @Autowired
    public MetadataRetrieverService(ParserPool parserPool,
                                    SamlMetadataProcessResultRepository resultRepository,
                                    SamlMetadataProcessErrorRepository errorRepository,
                                    ConnectionService connectionService,
                                    ServiceService serviceService,
                                    SamlMetadataResponseMapper metadataResponseMapper, CacheService cacheService) {
        this.parserPool = parserPool;
        this.resultRepository = resultRepository;
        this.errorRepository = errorRepository;
        this.connectionService = connectionService;
        this.serviceService = serviceService;
        this.metadataResponseMapper = metadataResponseMapper;
        this.cacheService = cacheService;
    }

    public Page<SamlMetadataProcessResult> getAllSamlMetadataById(Long connectionId, int pageIndex, int pageSize) {
        return resultRepository.findAllByConnectionIdOrderByCreatedAtDesc(connectionId, PageRequest.of(pageIndex, pageSize));
    }

    public Page<SamlMetadataProcessError> getSamlMetadataById(Long resultId, int pageIndex, int pageSize) {
        return errorRepository.findBySamlMetadataProcessResultId(resultId, PageRequest.of(pageIndex, pageSize));
    }

    public String getProcessedMetadata(Long resultId) {
        return resultRepository.findById(resultId).map(SamlMetadataProcessResult::getMetadata).orElse(null);
    }

    @Cacheable(value = "metadata-response", key = "#samlMetadataRequest.cacheableKey()")
    public SamlMetadataResponse resolveSamlMetadata(SamlMetadataRequest samlMetadataRequest) {
        LOGGER.info("Cache not found for saml-metadata {}", samlMetadataRequest.hashCode());

        Connection connection = connectionService.getConnectionByEntityId(samlMetadataRequest.getConnectionEntityId());

        MetadataResponseStatus metadataResponseStatus = null;
        nl.logius.digid.dc.domain.service.Service service = null;

        if (connection == null) {
            metadataResponseStatus = CONNECTION_NOT_FOUND;
        } else if (!connection.getStatus().isAllowed()) {
            metadataResponseStatus = CONNECTION_INACTIVE;
        } else if (!connection.getOrganization().getStatus().isAllowed()) {
            metadataResponseStatus = ORGANIZATION_INACTIVE;
        } else if (Boolean.FALSE.equals(connection.getOrganizationRole().getStatus().isAllowed())) {
            metadataResponseStatus = ORGANIZATION_ROLE_INACTIVE;
        } else {
            String serviceUUID = samlMetadataRequest.getServiceUuid() == null ? getServiceUUID(connection, samlMetadataRequest.getServiceEntityId(), samlMetadataRequest.getServiceIdx()) : samlMetadataRequest.getServiceUuid();
            samlMetadataRequest.setServiceUuid(serviceUUID);
            service = serviceService.serviceExists(connection, samlMetadataRequest.getServiceEntityId(), serviceUUID);
            if (service == null) {
                metadataResponseStatus = SERVICE_NOT_FOUND;
            } else if (!service.getStatus().isAllowed()) {
                metadataResponseStatus = SERVICE_INACTIVE;
            }
        }

        if (metadataResponseStatus != null) {
            return metadataResponseMapper.mapErrorResponse(metadataResponseStatus.name(), metadataResponseStatus.label);
        } else {
            String samlMetadata = generateReducedMetadataString(connection, service.getEntityId());
            return metadataResponseMapper.mapSuccessResponse(samlMetadata, connection, service, STATUS_OK.name());
        }
    }

    public String getServiceUUID(Connection connection, String serviceEntityId, int consumingServiceIdx) {
        if (connection == null || serviceEntityId == null)
            return null;

        EntityDescriptor serviceEntityDescriptor = resolveEntityDescriptorFromMetadata(connection, serviceEntityId);

        if (serviceEntityDescriptor == null || serviceEntityDescriptor.getRoleDescriptors() == null) {
            return null;
        }

        List<XMLObject> list = serviceEntityDescriptor.getRoleDescriptors().get(0).getOrderedChildren();

        if (list == null) {
            return null;
        }

        Optional<XMLObject> xmlObject = list
            .stream()
            .filter(obj -> obj instanceof AttributeConsumingService && ((AttributeConsumingService) obj).getIndex() == consumingServiceIdx)
            .findFirst();

        if (xmlObject.isEmpty()) {
            return null;
        }

        AttributeConsumingService attributeConsumingService = (AttributeConsumingService) xmlObject.get();
        Optional<RequestedAttribute> requestedAttribute = attributeConsumingService.getRequestedAttributes()
            .stream()
            .filter(o -> o.getName().equals(SAML_SERVICE_UUID_NAME))
            .findFirst();

        return requestedAttribute.isEmpty() ? null : ((XSAny) requestedAttribute.get().getAttributeValues().get(0)).getTextContent();
    }

    public String generateReducedMetadataString(Connection connection, String serviceEntityId) {
        EntitiesDescriptor entitiesDescriptor = generateReducedEntitiesDescriptor(connection, serviceEntityId);
        String xmlString = "";
        try {
            Marshaller out = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(entitiesDescriptor);
            out.marshall(entitiesDescriptor);
            Element element = entitiesDescriptor.getDOM();

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(element);

            transformer.transform(source, result);
            xmlString = Base64.getEncoder().encodeToString(result.getWriter().toString().getBytes());
        } catch (MarshallingException | TransformerException e) {
            LOGGER.error("An error has occurred generating metadata string: {}", e.getMessage());
        }

        return xmlString;
    }

    private EntitiesDescriptor generateReducedEntitiesDescriptor(Connection connection, String serviceEntityId) {
        EntitiesDescriptor entitiesDescriptor = OpenSAMLUtils.buildSAMLObject(EntitiesDescriptor.class);
        try {
            EntityDescriptor connectionEntity = XMLObjectSupport.cloneXMLObject(resolveEntityDescriptorFromMetadata(connection, connection.getEntityId()));
            EntityDescriptor serviceEntity = XMLObjectSupport.cloneXMLObject(resolveEntityDescriptorFromMetadata(connection, serviceEntityId));

            entitiesDescriptor.getEntityDescriptors().add(connectionEntity);
            entitiesDescriptor.getEntityDescriptors().add(serviceEntity);
        } catch (MarshallingException | UnmarshallingException e) {
            LOGGER.error("An error has occurred generating entities descriptor: {}", e.getMessage());
        }
        return entitiesDescriptor;
    }

    private EntityDescriptor resolveEntityDescriptorFromMetadata(Connection connection, String entityId) {
        StringMetadataResolver metadataResolver = new StringMetadataResolver();
        try {
            metadataResolver.setParserPool(parserPool);
            metadataResolver.setId("StringMetadataResolver");
            metadataResolver.initialize();
            metadataResolver.addMetadataString(cacheService.getCacheableMetadata(connection));
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
            return metadataResolver.resolveSingle(criteria);
        } catch (UnmarshallingException | ResolverException | ComponentInitializationException e) {
            LOGGER.error("An error has occurred resolving entity descriptor form metadata: {}", e.getMessage());
        }

        return null;
    }


    public MetadataResponseBase resolveOidcMetadata(OidcMetadataRequest request) {
        var service = serviceService.getServiceByClientId(request.getClientId());
        var connection = service != null ? service.getConnection() : null;
        var response = new OidcMetadataResponse();
        MetadataResponseStatus metadataResponseStatus = null;

        if (service == null) {
            metadataResponseStatus = SERVICE_NOT_FOUND;
        } else if (connection == null) {
            metadataResponseStatus = CONNECTION_NOT_FOUND;
        } else if (!connection.getStatus().isAllowed()) {
            metadataResponseStatus = CONNECTION_INACTIVE;
        } else if (!connection.getOrganization().getStatus().isAllowed()) {
            metadataResponseStatus = ORGANIZATION_INACTIVE;
        } else if (Boolean.FALSE.equals(connection.getOrganizationRole().getStatus().isAllowed())) {
            metadataResponseStatus = ORGANIZATION_ROLE_INACTIVE;
        } else if (!service.getStatus().isAllowed()) {
            metadataResponseStatus = SERVICE_INACTIVE;
        } else {
            Optional.ofNullable(service.getAppActive()).ifPresent(response::setAppActive);
            Optional.ofNullable(service.getAppReturnUrl()).ifPresent(response::setAppReturnUrl);
            Optional.ofNullable(service.getName()).ifPresent(response::setServiceName);
            Optional.ofNullable(service.getMinimumReliabilityLevel()).ifPresent(response::setMinimumReliabilityLevel);
            Optional.ofNullable(service.getLegacyServiceId()).ifPresent(response::setLegacyWebserviceId);
            Optional.ofNullable(service.getIconUri()).ifPresent(response::setIconUri);

            response.setRequestStatus(STATUS_OK.name());

            Optional.ofNullable(connection.getMetadataUrl()).ifPresent(response::setMetadataUrl);
            Optional.ofNullable(connection.getProtocolType()).ifPresent(response::setProtocolType);
        }

        if (metadataResponseStatus != null) {
            response.setRequestStatus(metadataResponseStatus.name());
            response.setErrorDescription(metadataResponseStatus.label);
        }

        return response;
    }
}
