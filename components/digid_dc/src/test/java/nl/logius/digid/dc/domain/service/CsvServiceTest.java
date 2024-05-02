
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.dc.client.DigidAdminClient;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.connection.ConnectionRepository;
import nl.logius.digid.dc.domain.metadata.CacheService;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @InjectMocks
    private CsvService csvService;

    @Mock
    private ServiceRepository serviceRepositoryMock;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private DigidAdminClient digidAdminClientMock;

    @Mock
    private CacheService cacheService;

    @Test
    void processCsvFileSuccessCreationServiceTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        mockAdmin();
        mockconnection();

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
    }

    @Test
    void processCsvFileSuccessUpdatedServiceTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        Service service = new Service();
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);

        mockAdmin();
        mockconnection();

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
        assertEquals((Long)13L, service.getLegacyServiceId());
    }

    @Test
    void processCsvFileFailInvalidDuplicateUUIDTest() throws JsonProcessingException, UnsupportedEncodingException {
        // Contains 2 duplicate UUID, 2 empty UUID
        String csvData =  """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        List<String> failedArray = new ArrayList<>();
        failedArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        assertEquals("Bestand niet verwerkt", resultMap.get("result"));
        assertTrue(((List) resultMap.get("failed")).contains("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
    }

    @Test
    void processCsvFileFailInvalidConnectionEntityIdTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }
    @Test
    void processCsvFileFailMissingServiceEntityIdTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }
    @Test
    void processCsvFileFailMissingServiceUuidTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "Regel: 1 UUID:  Foutbericht: Service UUID ontbreekt";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }
    @Test
    void processCsvFileFailMissingNameTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIncorrectMinimumLevelTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIncorrectEncryptionTypeTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIncorrectNewMinimumLevelTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";
        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIncorrectNewMinimumLevelDateTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIncorrectServiceUUID() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailDigidTrueWithMultipleServiceOrganizationRolesTest() throws IOException {
        mockconnection();
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        Service service = new Service();
        service.setDigid(false);
        service.setServiceOrganizationRoles(List.of(new ServiceOrganizationRole(), new ServiceOrganizationRole()));
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((ArrayList) resultMap.get("succeeded")).isEmpty());
        assertTrue(((ArrayList) resultMap.get("failed")).size() == 1);
        assertTrue(((ArrayList) resultMap.get("failed")).contains(expectedValue));
    }

    @Test
    void processCsvFileEmptyRowTest() throws IOException {
        String csvData = """
        "","","","","","","","","","","","","","","","","","","","","";
        """;

        String[] expectedValues  = {
            "Regel: 1 UUID:  Foutbericht: Indicatie DigiD ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Indicatie Machtigen ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Entity ID dienst ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Service UUID ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Naam ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Minimum betrouwbaarheidsniveau ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Soort encryptie ontbreekt",
            "Regel: 1 UUID:  Foutbericht: Geldigheid ontbreekt"
        };

        processCsvFileFailMissingOrInvalidFields(csvData, expectedValues);
    }

    @Test
    void processCsvFileFailUniqueEntityIdAndNameTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        Service service = new Service();
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);
        when(serviceRepositoryMock.findServicesByUuidAndEntityId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(List.of(new Service(), new Service()));

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));

        String expectedValue = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        assertTrue(((ArrayList) resultMap.get("failed")).size() == 1);
        assertTrue(((ArrayList) resultMap.get("failed")).contains(expectedValue));

        assertTrue(((ArrayList) resultMap.get("succeeded")).isEmpty());
    }

    private void processCsvFileFailMissingOrInvalidField(String csvData, String expectedValue) throws IOException {
        when(serviceRepositoryMock.findFirstByServiceUuid(anyString())).thenReturn(Optional.empty());

        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((List) resultMap.get("failed")).contains(expectedValue));
    }

    private void processCsvFileFailMissingOrInvalidFields(String csvData, String[] expectedValues) throws IOException {
        Map<String, Object> resultMap = csvService.processCsvFile(encodeCsv(csvData), false);
        assertEquals("Bestand verwerkt", resultMap.get("result"));

        for (String value: expectedValues)
            assertTrue(((List) resultMap.get("failed")).contains(value));
    }

    private void mockAdmin() throws JsonProcessingException {
        String jsonString = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);
        final ObjectNode responseAdmin = ((ObjectNode) actualObj);
        when(digidAdminClientMock.retrieveLegacyServiceIds(Mockito.anyList())).thenReturn(responseAdmin);
    }

    private void mockconnection(){
        Connection connection = new Connection();
        connection.setId(1L);
        when(connectionRepository.findByEntityId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(Optional.of(connection));
    }

    private String encodeCsv(String csv) throws UnsupportedEncodingException {
        byte[] decodedBytes =  Base64.encodeBase64(csv.getBytes());
        return new String(decodedBytes, StandardCharsets.UTF_8.name());
    }
}

