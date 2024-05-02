
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
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.dc.client.DigidXClient;
import nl.logius.digid.dc.domain.certificate.CertificateType;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.connection.ConnectionService;
import nl.logius.digid.dc.domain.service.Service;
import nl.logius.digid.dc.domain.service.ServiceService;
import nl.logius.digid.dc.exception.CollectSamlMetadataException;
import nl.logius.digid.dc.exception.MetadataParseException;
import nl.logius.digid.dc.util.StringMetadataResolver;
import nl.logius.digid.dc.util.X509CertificateDataParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.xml.security.utils.XMLUtils;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for persisting metadata to the database.
 */

@org.springframework.stereotype.Service
public class MetadataProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataProcessorService.class);
    private final ConnectionService connectionService;
    private final ServiceService serviceService;
    private final SamlMetadataProcessResultRepository samlMetadataProcessResultRepository;
    private final DigidXClient digidXClient;
    private final CloseableHttpClient httpClient;

    @Value("${validate_oin_in_certificate:true}")
    public boolean oinCheckEnabled;

    @Autowired
    public MetadataProcessorService(ConnectionService connectionService,
                                    ServiceService serviceService,
                                    SamlMetadataProcessResultRepository samlMetadataProcessResultRepository,
                                    DigidXClient digidXClient,
                                    CloseableHttpClient httpClient) {
        this.connectionService = connectionService;
        this.serviceService = serviceService;
        this.samlMetadataProcessResultRepository = samlMetadataProcessResultRepository;
        this.digidXClient = digidXClient;
        this.httpClient = httpClient;
    }

    @Transactional
    public Map<String, String> collectSamlMetadata(String id) throws CollectSamlMetadataException {
        Map<String, String> map = new HashMap<>();
        List<Connection> list;
        LOGGER.info("Start collecting metadata!!");
        digidXClient.remoteLog("1446", null);

        try {
            list = id.equals("all") ? connectionService.listWithAllConnections() : connectionService.listWithOneConnection(Long.valueOf(id));
            for (Connection con : list) startCollectMetadata(con, map);
            map.put("count", String.valueOf(list.size()));
        } catch (Exception e) {
            LOGGER.error("An error has occurred collecting metadata connections: {}", e.getMessage());
            throw new CollectSamlMetadataException(e.getMessage());
        }
        digidXClient.remoteLog("1447", map);
        return map;
    }

    @Async
    @Transactional
    public SamlMetadataProcessResult startCollectMetadata(Connection con, Map<String, String> map) {
        SamlMetadataProcessResult result = new SamlMetadataProcessResult(con.getId());
        EntitiesDescriptor descriptor;

        try {
            String metadataXML = getMetadataFromConnection(con);
            descriptor = convertMetadataXMLtoEntitiesDescriptor(metadataXML);
            String hash = getSignatureValue(descriptor.getSignature());

            Optional<SamlMetadataProcessResult> process = samlMetadataProcessResultRepository.findByConnectionIdAndHash(con.getId(), hash);

            if (process.isPresent()) return result;

            updateMetadata(descriptor, con, map, result);
            result.setMetadata(metadataXML);

            if (result.allEntriesSuccessful()) {
                result.setHash(hash);
            }
        } catch (InitializationException | ComponentInitializationException | UnmarshallingException | IOException | MetadataParseException e) {
            map.put("status", "failed");
            LOGGER.error("Failed to collect/parse metadata: {}", e.getMessage());
            result.addProcessError(e.getMessage(), "");
        }

        samlMetadataProcessResultRepository.saveAndFlush(result);

        return result;
    }

    private String getMetadataFromConnection(Connection con) throws IOException {
        if (con.getMetadataUrl() != null && !con.getMetadataUrl().isEmpty()) {
            return fetchDataFromUrl(con.getMetadataUrl());
        } else if ((con.getSamlMetadata() != null && !con.getSamlMetadata().isEmpty())) {
            return con.getDecodedSamlMetadata();
        }

        return null;
    }

    public void updateMetadata(EntitiesDescriptor descriptor, Connection con, Map<String, String> map, SamlMetadataProcessResult result) {
        List<EntityDescriptor> listOfEntityDescriptors = descriptor.getEntityDescriptors();

        EntityDescriptor lcEntity = listOfEntityDescriptors.get(0);
        if (StringUtils.equals(lcEntity.getEntityID(), con.getEntityId())) {
            updateConnectionMetadata(descriptor, con);

            for (EntityDescriptor dv : listOfEntityDescriptors.subList(1, listOfEntityDescriptors.size()))
                updateServiceMetadata(con, dv, result);

        } else {
            map.put("status", "failed");
            result.addProcessError("EntityID aansluiting niet gevonden", convertXmlObjectToString(lcEntity));
        }
    }

    private void updateConnectionMetadata(EntitiesDescriptor lcEntity, Connection connection) {
        connection.setSamlMetadata(Base64.getEncoder().encodeToString(Objects.requireNonNull(convertXmlObjectToString(lcEntity)).getBytes()));
        List<X509Certificate> list = getCertificatesFromEntity(lcEntity.getEntityDescriptors().get(0));
        if (!list.isEmpty()) connection.addCertificate(list.get(0).getValue(), CertificateType.SIGNING);
        if (list.size() > 1) connection.addCertificate(list.get(1).getValue(), CertificateType.TLS);

        connectionService.updateConnection(connection);
    }

    private void updateServiceMetadata(Connection connection, EntityDescriptor dvEntity, SamlMetadataProcessResult result) {
        try {
            Service service = serviceService.findAllowedServiceById(connection.getId(), dvEntity.getEntityID());
            if (service == null) {
                result.addProcessError("Dienst: entityID bestaat niet", convertXmlObjectToString(dvEntity));
            } else {
                final X509Certificate cert = getCertificateFromEntity(dvEntity);
                service.addCertificate(cert.getValue());
                serviceService.updateService(service);
                result.incrementUpdated();
            }
        } catch (Exception e) {
            LOGGER.error("Error updating service definition: {}", e.getMessage());
            result.addProcessError(e.getMessage(), convertXmlObjectToString(dvEntity));
        }
        result.incrementProcessed();
    }


    private String fetchDataFromUrl(String targetUrl) throws IOException {
        HttpGet request = new HttpGet(targetUrl);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        }

        return null;
    }

    private EntitiesDescriptor convertMetadataXMLtoEntitiesDescriptor(String metadataXML) throws InitializationException, ComponentInitializationException, UnmarshallingException {
        InitializationService.initialize();
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        StringMetadataResolver stringMetadataResolver = new StringMetadataResolver();

        stringMetadataResolver.setParserPool(parserPool);
        stringMetadataResolver.setId("StringMetadataResolver");
        stringMetadataResolver.initialize();

        stringMetadataResolver.addMetadataString(metadataXML);

        EntitiesDescriptor descriptor = stringMetadataResolver.getEntitiesDescriptor();

        checkSignature(descriptor);

        return descriptor;
    }

    private void checkSignature(EntitiesDescriptor descriptor) {
        try {
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(descriptor.getSignature());
            EntityDescriptor entityDescriptor = descriptor.getEntityDescriptors().get(0);
            Credential credentials = retrieveServiceProviderCredential(entityDescriptor);
            for (KeyDescriptor key : entityDescriptor.getRoleDescriptors().get(0).getKeyDescriptors()) {
                for (X509Certificate certificate : key.getKeyInfo().getX509Datas().get(0).getX509Certificates()) {
                    checkCertificate(entityDescriptor.getEntityID(), certificate);
                }
            }

            SignatureValidator.validate(descriptor.getSignature(), credentials);
        } catch (CertificateException e) {
            throw new MetadataParseException("Metadata certificate invalid");
        } catch (SignatureException e) {
            throw new MetadataParseException("Metadata signature invalid");
        }
    }

    public void checkCertificate(String entityId, X509Certificate x509cert) {
        if (oinCheckEnabled) {
            byte[] certBytes = Base64.getDecoder().decode(x509cert.getValue().replaceAll("\\s+", ""));
            checkOin(entityId, X509CertificateDataParser.getOin(certBytes));
        }
    }

     public void checkOin(String entityId, String oin) {
        Pattern pattern = Pattern.compile("urn:nl-eid-gdi:1.0:\\w+:" + oin + ":entities:\\d+");
        Matcher matcher = pattern.matcher(entityId);

        if (!matcher.matches()) {
            throw new MetadataParseException("OIN certificate does not match entityID");
        }
    }


    private Credential retrieveServiceProviderCredential(EntityDescriptor entity) throws CertificateException {
        final X509Certificate cert = getCertificateFromEntity(entity);
        return CredentialSupport.getSimpleCredential(KeyInfoSupport.getCertificate(cert), null);
    }

    private X509Certificate getCertificateFromEntity(EntityDescriptor entity) {
        return getCertificatesFromEntity(entity).get(0);
    }

    private List<X509Certificate> getCertificatesFromEntity(EntityDescriptor entity) {
        return entity.getRoleDescriptors().get(0).getKeyDescriptors().get(0).getKeyInfo().getX509Datas().get(0).getX509Certificates();
    }

    private String convertXmlObjectToString(XMLObject object) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();

            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(object.getDOM());

            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            LOGGER.error("Error transforming xml to string: {}", e.getMessage());
        }
        return null;
    }

    private String getSignatureValue(Signature signature) {
        Element xmlElement = signature.getDOM();
        while (xmlElement != null) {
            switch (xmlElement.getTagName()) {
                case "ds:Signature":
                case "ds:SignedInfo":
                case "ds:Reference":
                    xmlElement = XMLUtils.getNextElement(xmlElement.getFirstChild());
                    break;
                case "ds:CanonicalizationMethod":
                case "ds:SignatureMethod":
                case "ds:Transforms":
                case "ds:DigestMethod":
                    xmlElement = XMLUtils.getNextElement(xmlElement.getNextSibling());
                    break;
                case "ds:DigestValue":
                    return XMLUtils.getFullTextChildrenFromNode(xmlElement);
                default: LOGGER.warn("Signature value not available");
                    return null;
            }
        }
        return null;
    }
}
