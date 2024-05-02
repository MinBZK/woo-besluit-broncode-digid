
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

import nl.logius.digid.saml.Application;
import nl.logius.digid.saml.client.DienstencatalogusClient;
import nl.logius.digid.saml.domain.artifact.ArtifactResolveRequest;
import nl.logius.digid.saml.domain.authentication.AuthenticationRequest;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.exception.DienstencatalogusException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.logius.digid.saml.domain.metadata.DcMetadataResponseStatus.STATUS_OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
@SpringBootTest(classes = {Application.class})
public class DcMetadataServiceTest {
    protected static final String CONNECTION_ENTITY_ID = "urn:nl-eid-gdi:1:0:entities:00000008888888888000";
    protected static final String SERVICE_ENTITY_ID = "urn:nl-eid-gdi:1:0:entities:00000009999999999001";
    private DcMetadataService dcMetadataService;

    @Mock
    private DienstencatalogusClient dienstencatalogusClientMock;
    @Autowired
    private DcMetadataResponseMapper dcMetadataResponseMapper;

    @Value("classpath:saml/test-ca-metadata.xml")
    private Resource stubsCaMetadataFile;

    private static String readMetadata(Resource metadataFile) {
        try (Reader reader = new InputStreamReader(metadataFile.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DcMetadataResponse dcClientStubGetMetadata(Resource metadata, String federation) {
        return dcClientStubGetMetadata(metadata, federation, null);
    }

    private static DcMetadataResponse dcClientStubGetMetadata(Resource metadata, String federation, Long legacyWebserviceId) {
        return dcClientStubGetMetadata(metadata, federation, legacyWebserviceId, 10);
    }

    private static DcMetadataResponse dcClientStubGetMetadata(Resource metadata, String federation, Long legacyWebserviceId, int minimumReliabilityLevel) {
        final DcMetadataResponse response = new DcMetadataResponse();
        String metadataString = readMetadata(metadata);
        response.setSamlMetadata(Base64.getEncoder().encodeToString(metadataString.getBytes()));
        response.setFederationName(federation);
        response.setLegacyWebserviceId(legacyWebserviceId);
        response.setMinimumReliabilityLevel(minimumReliabilityLevel);
        response.setRequestStatus(STATUS_OK.label);
        response.setServiceUuid("serviceUUID");
        response.setPermissionQuestion("permissionQuestion");
        response.setAppActive(true);
        response.setAppReturnUrl("appReturnUrl");
        response.setServiceName("serviceName");
        response.setEncryptionIdType("BSN");
        response.setProtocolType(ProtocolType.SAML_ROUTERINGSDIENST);

        return response;
    }

    @BeforeAll
    public static void setUpClass() throws InitializationException {
        InitializationService.initialize();
    }

    @BeforeEach
    public void setup() {
        dcMetadataService = new DcMetadataService(dienstencatalogusClientMock, dcMetadataResponseMapper);
    }

    @Test
    public void parseMetadataWrongIdTest() throws DienstencatalogusException {
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(stubDcResponse());
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId("someNonExistingEntityId");

        dcMetadataService.resolveDcMetadata(request);

        assertNotNull(request.getConnectionEntity());
        assertNull(request.getServiceEntity());
    }

    @Test
    public void resolveAuthenticationDcMetadataTest() throws DienstencatalogusException {
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(stubDcResponse());
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        dcMetadataService.resolveDcMetadata(request);

        assertEquals("federation1", request.getFederationName());
        assertEquals(1, request.getLegacyWebserviceId());
        assertEquals(10, ((AuthenticationRequest) request).getMinimumRequestedAuthLevel());
        assertEquals("serviceUUID", request.getServiceUuid());
        assertEquals("permissionQuestion", request.getPermissionQuestion());
        assertEquals(true, ((AuthenticationRequest) request).getAppActive());
        assertEquals("appReturnUrl", ((AuthenticationRequest) request).getAppReturnUrl());
        assertEquals("serviceName", ((AuthenticationRequest) request).getServiceName());
        assertEquals("urn:nl-eid-gdi:1:0:entities:00000009999999999001", ((AuthenticationRequest) request).getEntityId());
        assertEquals(10, ((AuthenticationRequest) request).getMinimumRequestedAuthLevel());
        assertEquals("BSN", ((AuthenticationRequest) request).getEncryptionIdType());
        assertNotNull(request.getConnectionEntity());
        assertNotNull(request.getServiceEntity());
    }

    @Test
    public void resolveSamlRequestDcMetadataTest() throws DienstencatalogusException {
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(stubDcResponse());
        SamlRequest request = new ArtifactResolveRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        dcMetadataService.resolveDcMetadata(request);

        assertEquals("federation1", request.getFederationName());
        assertEquals(1, request.getLegacyWebserviceId());
        assertEquals("serviceUUID", request.getServiceUuid());
        assertEquals("permissionQuestion", request.getPermissionQuestion());
        assertNotNull(request.getConnectionEntity());
        assertNotNull(request.getServiceEntity());
    }

    @Test
    public void resolveDcMetadataUnknownStatusTest() throws DienstencatalogusException {
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(null);
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        try {
            dcMetadataService.resolveDcMetadata(request);
        } catch (DienstencatalogusException e) {
            assertEquals("Unknown status from digid_dc", e.getMessage());
        }
    }

    @Test
    public void resolveDcMetadataNotFoundErrorsTest() throws DienstencatalogusException {
        HashMap<String, String> PossibleDcRetrieveErrorStatus = new HashMap<>();
        PossibleDcRetrieveErrorStatus.put("CONNECTION_NOT_FOUND", "connection not found");
        PossibleDcRetrieveErrorStatus.put("CONNECTION_INACTIVE", "connection not active");
        PossibleDcRetrieveErrorStatus.put("ORGANIZATION_INACTIVE", "organization not active");
        PossibleDcRetrieveErrorStatus.put("ORGANIZATION_ROLE_INACTIVE", "organization role not active");
        PossibleDcRetrieveErrorStatus.put("SERVICE_NOT_FOUND", "service not found");
        PossibleDcRetrieveErrorStatus.put("SERVICE_INACTIVE", "service not active");

        for (String i : PossibleDcRetrieveErrorStatus.keySet()) {
            resolveDcMetadataNotFoundTest(i, PossibleDcRetrieveErrorStatus.get(i));
        }
    }

    private void resolveDcMetadataNotFoundTest(String status, String description) throws DienstencatalogusException {
        DcMetadataResponse response = new DcMetadataResponse();
        response.setRequestStatus(status);
        response.setErrorDescription(description);
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(response);
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        try {
            dcMetadataService.resolveDcMetadata(request);
        } catch (DienstencatalogusException e) {
            assertEquals("Metadata from dc not found or not active: "+ description, e.getMessage());
        }
    }

    @Test
    public void resolveDcMetadataNoFederationTest() throws DienstencatalogusException {
        DcMetadataResponse dcMetadataResponse = dcClientStubGetMetadata(stubsCaMetadataFile, null, 1L);
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(dcMetadataResponse);
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        assertDoesNotThrow(() -> dcMetadataService.resolveDcMetadata(request));
    }

    @Test
    public void resolveDcMetadataNoLegacyWebserviceIdTest() throws DienstencatalogusException {
        DcMetadataResponse dcMetadataResponse = dcClientStubGetMetadata(stubsCaMetadataFile, null);
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(dcMetadataResponse);
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        assertDoesNotThrow(() -> dcMetadataService.resolveDcMetadata(request));
    }

    @Test
    public void resolveDcMinimumAuthLevelNotFoundTest() throws DienstencatalogusException {
        DcMetadataResponse dcMetadataResponse = dcClientStubGetMetadata(stubsCaMetadataFile, null, 1L);
        dcMetadataResponse.setMinimumReliabilityLevel(null);
        when(dienstencatalogusClientMock.retrieveMetadataFromDc(any(SamlRequest.class))).thenReturn(dcMetadataResponse);
        SamlRequest request = new AuthenticationRequest();
        request.setConnectionEntityId(CONNECTION_ENTITY_ID);
        request.setServiceEntityId(SERVICE_ENTITY_ID);

        try {
            dcMetadataService.resolveDcMetadata(request);
        } catch (DienstencatalogusException e) {
            assertEquals("Metadata from dc minimum reliability level not set", e.getMessage());
        }
    }

    private DcMetadataResponse stubDcResponse() {
        return dcClientStubGetMetadata(stubsCaMetadataFile, "federation1", 1L, 10);
    }
}
