
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

import nl.logius.digid.dc.client.DigidXClient;
import nl.logius.digid.dc.domain.certificate.Certificate;
import nl.logius.digid.dc.domain.certificate.CertificateType;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.connection.ConnectionService;
import nl.logius.digid.dc.domain.service.LevelOfAssurance;
import nl.logius.digid.dc.domain.service.Service;
import nl.logius.digid.dc.domain.service.ServiceService;
import nl.logius.digid.dc.exception.CollectSamlMetadataException;
import nl.logius.digid.dc.exception.MetadataParseException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles({"default", "integration-test"})
@ExtendWith(MockitoExtension.class)
public class MetadataProcessorServiceTest {

    @Mock
    private ConnectionService connectionServiceMock;
    @Mock
    private ServiceService serviceServiceMock;
    @Mock
    private CloseableHttpClient httpClientMock;
    @Mock
    private CloseableHttpResponse httpResponseMock;
    @Mock
    private HttpEntity httpEntityMock;
    @Mock
    private DigidXClient digidXClientMock;

    @Mock // MetadataProcessorService silent dependency
    private SamlMetadataProcessResultRepository resultRepositoryMock;

    @InjectMocks
    private MetadataProcessorService metadataProcessorServiceMock;

    @Test
    public void collectSamlMetadataValidTest() throws IOException, CollectSamlMetadataException {
        List<Connection> connections = new ArrayList<>();
        connections.add(newConnection());

        Map<String, String> expected = new HashMap<>();
        expected.put("count", "1");

        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("metadata/valid-metadata.xml"));
        when(connectionServiceMock.listWithAllConnections()).thenReturn(connections);
        doNothing().when(digidXClientMock).remoteLog(anyString(), nullable(Map.class));

        Map<String, String> result = metadataProcessorServiceMock.collectSamlMetadata("all");

        verify(connectionServiceMock, times(1)).listWithAllConnections();
        assertNotNull(result);
    }

    @Test
    public void startCollectMetadataValidTest() throws IOException {
        // Connection, service and certificate expected values from metadata/valid-valid-metadata.xml
        Connection connection = newConnection();
        Service service = newService();
        List<Certificate> certificates = getServiceCertificates();

        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("metadata/valid-metadata.xml"));
        when(serviceServiceMock.findAllowedServiceById(anyLong(), anyString())).thenReturn(service);

        // Map can be null if no errors occur
        SamlMetadataProcessResult result = metadataProcessorServiceMock.startCollectMetadata(connection, null);

        // Result
        assertEquals(3, result.getTotalUpdated());
        assertEquals(0, result.getTotalErrors());
        assertEquals(3, result.getTotalProcessed());
        assertEquals(0, result.getSamlMetadataProcessErrors().size());
        assertEquals(3, service.getCertificates().size());

        // Connection
        assertNotNull(result.getMetadata());
        assertEquals(CertificateType.SIGNING, connection.getCertificates().get(0).getCertType());

        // Service
        for (int i = 0; i<certificates.size(); i++) {
            assertEquals(certificates.get(i).getCachedCertificate(), service.getCertificates().get(i).getCachedCertificate());
            assertEquals(certificates.get(i).getFingerprint(), service.getCertificates().get(i).getFingerprint());
            assertEquals(certificates.get(i).getDistinguishedName(), service.getCertificates().get(i).getDistinguishedName());
            assertEquals(certificates.get(i).getActiveFrom(), service.getCertificates().get(i).getActiveFrom());
            assertEquals(certificates.get(i).getActiveUntil(), service.getCertificates().get(i).getActiveUntil());
            assertEquals(certificates.get(i).getCertType(), service.getCertificates().get(i).getCertType());
        }
    }

    @Test
    public void startCollectMetadataWithUnknownConnectionEntityIDTest() throws IOException {
        Map<String, String> map = new HashMap<>();
        Connection connection = new Connection();
        connection.setMetadataUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        connection.setSamlMetadata("samlmetadata");
        connection.setEntityId("entity id");

        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("metadata/valid-metadata.xml"));

        SamlMetadataProcessResult result = metadataProcessorServiceMock.startCollectMetadata(connection, map);

        assertEquals(0, result.getTotalUpdated());
        assertEquals(1, result.getTotalErrors());
        assertEquals(0, result.getTotalProcessed());
        assertEquals(1, result.getSamlMetadataProcessErrors().size());
        assertEquals("failed", map.get("status"));
        assertEquals("EntityID aansluiting niet gevonden", result.getSamlMetadataProcessErrors().get(0).getErrorReason());
        assertNotNull(result.getSamlMetadataProcessErrors().get(0).getService());

        assertNotNull(result.getMetadata());
        assertEquals(0, connection.getCertificates().size());
    }

    @Test
    public void startCollectMetadataWithUnknownServiceEntityIDTest() throws IOException {
        Connection connection = newConnection();

        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("metadata/valid-metadata.xml"));
        when(serviceServiceMock.findAllowedServiceById(anyLong(), anyString())).thenReturn(null);

        SamlMetadataProcessResult result = metadataProcessorServiceMock.startCollectMetadata(connection, null);

        assertEquals(0, result.getTotalUpdated());
        assertEquals(3, result.getTotalErrors());
        assertEquals(3, result.getTotalProcessed());
        assertEquals(3, result.getSamlMetadataProcessErrors().size());
        assertEquals("Dienst: entityID bestaat niet", result.getSamlMetadataProcessErrors().get(0).getErrorReason());
        assertNotNull(result.getSamlMetadataProcessErrors().get(0).getService());

        assertNotNull(result.getMetadata());
        assertEquals(CertificateType.SIGNING, connection.getCertificates().get(0).getCertType());
    }

    @Test
    public void startCollectMetadataWithInvalidSignatureTest() throws IOException {
        Map<String, String> map = new HashMap<>();
        Connection connection = newConnection();

        when(httpClientMock.execute(any(HttpGet.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(httpEntityMock);
        when(httpEntityMock.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("metadata/invalid-signature-metadata.xml"));

        SamlMetadataProcessResult result = metadataProcessorServiceMock.startCollectMetadata(connection, map);

        assertEquals(0, result.getTotalUpdated());
        assertEquals(0, result.getTotalProcessed());
        assertEquals(1, result.getTotalErrors());
        assertEquals(1, result.getSamlMetadataProcessErrors().size());
        assertEquals("failed", map.get("status"));
        assertEquals("Metadata signature invalid", result.getSamlMetadataProcessErrors().get(0).getErrorReason());

        assertNull(result.getMetadata());
        assertEquals(0, connection.getCertificates().size());
    }

    @Test
    public void checkOinTest() {
        metadataProcessorServiceMock.checkOin("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSS");
        metadataProcessorServiceMock.checkOin("urn:nl-eid-gdi:1:0:DV:00000008888888888001:entities:0001", "SSSSSSSSSSSSSSSSSSSS");
    }

    @Test
    public void checkOinFailTest() {
        assertThrows(MetadataParseException.class, () -> {
            metadataProcessorServiceMock.checkOin("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSS");
        });
    }

    @Test
    public void startCollectMetadataSignatureTest() throws CollectSamlMetadataException {
        metadataProcessorServiceMock.collectSamlMetadata("all");

        verify(digidXClientMock, times(1)).remoteLog(anyString(), anyMap());
    }

    private Connection newConnection() {
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setMetadataUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        connection.setEntityId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        connection.setSamlMetadata("samlmetadata");
        return connection;
    }

    private Service newService() {
        Service service = new Service();
        service.setLegacyServiceId(34L);
        service.setMinimumReliabilityLevel(LevelOfAssurance.MIDDEN.getLabel());
        service.setEntityId("urn:nl-eid-gdi:1:0:entities:00000009999999999001");
        service.setServiceUuid("UUID");
        return service;
    }

    @java.lang.SuppressWarnings("squid:S1192") // certificates are not identical, only certain lines
    private List<Certificate> getServiceCertificates() {
        List<Certificate> certificates = new ArrayList<>();

        ZonedDateTime activeFrom = ZonedDateTime.parse("2020-03-11T10:31:46+01:00[Europe/Amsterdam]");
        ZonedDateTime activeUntil = ZonedDateTime.parse("2047-07-28T11:31:46+02:00[Europe/Amsterdam]");

        Certificate c1 = new Certificate();
        c1.setCachedCertificate(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );
        c1.setFingerprint("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        c1.setDistinguishedName("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        c1.setActiveFrom(activeFrom);
        c1.setActiveUntil(activeUntil);
        c1.setCertType(CertificateType.ENCRYPTION);

        Certificate c2 = new Certificate();
        c2.setCachedCertificate(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );
        c2.setFingerprint("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        c2.setDistinguishedName("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        c2.setActiveFrom(activeFrom);
        c2.setActiveUntil(activeUntil);
        c2.setCertType(CertificateType.ENCRYPTION);

        Certificate c3 = new Certificate();
        c3.setCachedCertificate(
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
        );
        c3.setFingerprint("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        c3.setDistinguishedName("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        c3.setActiveFrom(activeFrom);
        c3.setActiveUntil(activeUntil);
        c3.setCertType(CertificateType.ENCRYPTION);

        certificates.add(c1);
        certificates.add(c2);
        certificates.add(c3);

        return certificates;
    }
}
