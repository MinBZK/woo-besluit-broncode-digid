
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

package nl.logius.digid.dc.domain.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.service.LevelOfAssurance;
import nl.logius.digid.dc.domain.service.Service;
import nl.logius.digid.dc.domain.service.ServiceOrganizationRole;
import nl.logius.digid.dc.domain.service.ServiceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static nl.logius.digid.dc.domain.organization.OrganizationRoleType.DIENSTAANBIEDER;
import static nl.logius.digid.dc.domain.organization.OrganizationRoleType.ZELFSTANDIGE_AANSLUITHOUDER;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationCsvServiceTest {

    @InjectMocks
    private OrganizationCsvService organizationCsvService;

    @Mock
    private ServiceRepository serviceRepositoryMock;

    @Mock
    private OrganizationRepository organizationRepositoryMock;

    @Mock
    private OrganizationRoleRepository organizationRoleRepositoryMock;

    @Test
    void processCsvFileSuccessAllNewCreationTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        Service service = new Service();
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
    }

    @Test
    void processCsvFileSuccessOrganizationRoleAndServiceOrganizationRoleCreatedTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        setupMockRepositories(null, ZELFSTANDIGE_AANSLUITHOUDER.name(),  "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", false);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));

        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
    }

    @Test
    void processCsvFileMultipleServiceOrganizationRole() throws JsonProcessingException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        setupMockRepositories(null, ZELFSTANDIGE_AANSLUITHOUDER.name(),  "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", false);
        setupMockRepositories(null, ZELFSTANDIGE_AANSLUITHOUDER.name(),  "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", false);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);
        assertEquals("Bestand verwerkt", resultMap.get("result"));

        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());

    }

    @Test
    void processCsvFileSuccessOnlyServiceOrganizationRoleCreatedTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        setupMockRepositories(null,"Dienstaanbieder",  "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", false);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));

        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
    }

    @Test
    void processCsvFileSuccessAllUpdatedTest() throws IOException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        setupMockRepositories("SSSSSSSSSSSSSSSSSSSS", "Dienstaanbieder", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", true);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));

        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());

        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        assertArrayEquals(((ArrayList) resultMap.get("succeeded")).toArray(), succeededArray.toArray());
    }

    @Test
    public void processCsvFileSuccessUpdatingServiceOrganizationRoleWithDigidTrueTest() throws JsonProcessingException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";
        List<String> succeededArray = new ArrayList<>();
        succeededArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        OrganizationRole organizationRole = new OrganizationRole(DIENSTAANBIEDER);
        mockServiceRepository(organizationRole);
        mockOrganizationRepository(organizationRole);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((ArrayList) resultMap.get("failed")).isEmpty());
        assertArrayEquals(succeededArray.toArray(), ((ArrayList) resultMap.get("succeeded")).toArray());
    }

    @Test
    void processCsvFileFailInvalidNumberofColumnsTest() throws IOException {
        // csvData bevat 1 regel maar organisateveld ontbreekt
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";
        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Het ingevoerde CSV-bestand heeft een onjuist formaat. We verwachten 11 kolommen maar dit zijn er 10", resultMap.get("result"));
        assertFalse(resultMap.containsKey("failed"));
        assertFalse(resultMap.containsKey("succeeded"));
    }

    @Test
    void processCsvFileFailInvalidDuplicateOinTest() throws JsonProcessingException {
        //SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS" +
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        List<String> failedArray = new ArrayList<>();
        failedArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        failedArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(csvData, false);

        assertEquals("Bestand niet verwerkt", resultMap.get("result"));
        assertArrayEquals(((ArrayList) resultMap.get("failed")).toArray(), failedArray.toArray());
    }

    @Test
    public void processCsvFileFailCreatingServiceOrganizationRoleWithDigidTrueTest() throws JsonProcessingException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";
        List<String> failedArray = new ArrayList<>();
        failedArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        Service service = new Service();
        service.setDigid(true);
        service.setServiceOrganizationRoles(List.of(new ServiceOrganizationRole()));
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertArrayEquals(failedArray.toArray(), ((ArrayList) resultMap.get("failed")).toArray());
        assertTrue(((ArrayList) resultMap.get("succeeded")).isEmpty());
    }

    @Test
    public void processCsvFileFailUpdatingServiceOrganizationRolesWithDigidIndicationTest() throws JsonProcessingException {
        String csvData = """SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS""";

        List<String> failedArray = new ArrayList<>();
        failedArray.add("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

        OrganizationRole organizationRole = new OrganizationRole(DIENSTAANBIEDER);
        mockServiceRepository(organizationRole);
        mockOrganizationRepository(new OrganizationRole(DIENSTAANBIEDER));

        Map<String, Object> resultMap = organizationCsvService.processCsvFile(Base64.getEncoder().encodeToString(csvData.getBytes()), false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertArrayEquals(failedArray.toArray(), ((ArrayList) resultMap.get("failed")).toArray());
        assertTrue(((ArrayList) resultMap.get("succeeded")).isEmpty());
    }

    @Test
    void processCsvFileFailMissingOinTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: OIN Organisatie ontbreekt";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailMissingOrganizationActiveTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Organisatie: Actief ontbreekt";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidOrganizationActiveFromTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Organisatie: Datum ingang: waarde is 01-20-2020 15:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidOrganizationActiveUntilTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Organisatie: Datum einde: waarde is 01-20-2020 15:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailMissingOrganizationRoleActiveTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Organisatie Rol: Actief ontbreekt";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidOrganizationRoleActiveFromTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Organisatie Rol: Datum ingang: waarde is 01-20-2020 15:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidOrganizationRoleActiveUntilTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Organisatie Rol: Datum einde: waarde is 01-20-2020 15:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailIvalidOrganizationRoleRoleTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Organisatie Rol Type: Invalid Role";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailMissingServiceUuidTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Diensten ontbreekt";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    public void processCsvFileFailRelationServiceRoleActiefTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Rol/Dienst: Actief: a";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidRelationServiceRoleActiveFromTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Rol/Dienst: Datum ingang: waarde is 01-20-2020 18:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileFailInvalidRelationServiceRoleActiveUntilTest() throws IOException {
        String csvData = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        String expectedValue = "Regel: 1 Foutbericht: Incorrect Rol/Dienst: Datum einde: waarde is 01-20-2020 18:00 maar verwachten dit formaat: dd-MM-yyyy HH:mm";
        processCsvFileFailMissingOrInvalidField(csvData, expectedValue);
    }

    @Test
    void processCsvFileEmptyRowTest() throws IOException {
        String csvData = "IiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiIsIiI=";
        String[] expectedValues  = {
            "Regel: 1 Foutbericht: OIN Organisatie ontbreekt",
            "Regel: 1 Foutbericht: Organisatie: Actief ontbreekt",
            "Regel: 1 Foutbericht: Organisatie Rol Type ontbreekt",
            "Regel: 1 Foutbericht: Organisatie Rol: Actief ontbreekt",
            "Regel: 1 Foutbericht: Diensten ontbreekt"
        };

        processCsvFileFailMissingOrInvalidFields(csvData, expectedValues);
    }

    private void processCsvFileFailMissingOrInvalidFields(String csvData, String[] expectedValues) throws IOException {
        Map<String, Object> resultMap = organizationCsvService.processCsvFile(csvData, false);
        assertEquals("Bestand verwerkt", resultMap.get("result"));

        for (String value: expectedValues)
            assertTrue(((List) resultMap.get("failed")).contains(value));
    }

    private void processCsvFileFailMissingOrInvalidField(String csvData, String expectedValue) throws IOException {
        Map<String, Object> resultMap = organizationCsvService.processCsvFile(csvData, false);

        assertEquals("Bestand verwerkt", resultMap.get("result"));
        assertTrue(((List) resultMap.get("failed")).contains(expectedValue));
    }

    private void mockServiceRepository(OrganizationRole organizationRole) {
        organizationRole.setStatus(new Status());

        ServiceOrganizationRole serviceOrganizationRole = new ServiceOrganizationRole();
        serviceOrganizationRole.setOrganizationRole(organizationRole);
        serviceOrganizationRole.setStatus(new Status());

        Service service = new Service();
        service.setDigid(true);
        service.setServiceOrganizationRoles(new ArrayList(Arrays.asList(serviceOrganizationRole)));
        Optional<Service> optService = Optional.of(service);
        when(serviceRepositoryMock.findFirstByServiceUuid("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")).thenReturn(optService);
    }

    private void mockOrganizationRepository(OrganizationRole organizationRole) {
        Organization organization = new Organization();
        organization.setOrganizationRoles(new ArrayList(Arrays.asList(organizationRole)));
        organization.setStatus(new Status());
        String oin = "SSSSSSSSSSSSSSSSSSSS";
        when(organizationRepositoryMock.findByOin(oin)).thenReturn(Optional.of(organization));
    }

    private void setupMockRepositories(String orgOin, String repositoryOrgRole, String repositoryUuid, String searchUuid, boolean createServiceOrgRole) {
        String value = "01-09-2020 18:00";
        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(value, dateTimeformatter);
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("Europe/Amsterdam"));

        Status orgStatus = new Status();
        Organization organization = new Organization();
        organization.setOin(orgOin == null ?  "SSSSSSSSSSSSSSSSSSSS" : orgOin);
        organization.setName("name");
        organization.setDescription("description");
        organization.setStatus(orgStatus);
        organization.getStatus().setActive(true);
        organization.getStatus().setActiveFrom(zonedDateTime);
        organization.getStatus().setActiveUntil(zonedDateTime);

        Service service = new Service();
        service.setLegacyServiceId(34L);
        service.setMinimumReliabilityLevel(LevelOfAssurance.MIDDEN.getLabel());
        service.setEntityId("urn:nl-eid-gdi:1:0:entities:00000009999999999001");
        service.setServiceUuid(repositoryUuid);
        service.setDigid(true);
        service.setMachtigen(true);
        service.setName("Service Name");

        Status orgRoleStatus = new Status();
        OrganizationRole organizationRole = new OrganizationRole();
        organizationRole.setOrganization(organization);
        organizationRole.setType(OrganizationRoleType.valueOfLabel(repositoryOrgRole));
        organizationRole.setStatus(orgRoleStatus);
        organizationRole.getStatus().setActive(true);
        organizationRole.getStatus().setActiveFrom(zonedDateTime);
        organizationRole.getStatus().setActiveUntil(zonedDateTime);

        Status serviceOrganizationRoleStatus = new Status();
        ServiceOrganizationRole serviceOrganizationRole = new ServiceOrganizationRole();
        serviceOrganizationRole.setService(service);
        serviceOrganizationRole.setOrganizationRole(organizationRole);
        serviceOrganizationRole.setStatus(serviceOrganizationRoleStatus);
        serviceOrganizationRole.getStatus().setActive(true);
        serviceOrganizationRole.getStatus().setActiveFrom(zonedDateTime);
        serviceOrganizationRole.getStatus().setActiveUntil(zonedDateTime);

        // Create relation between OrganizationRole and Organization
        organizationRole.setOrganization(organization);
        organization.addOrganizationRole(organizationRole);

        if (createServiceOrgRole){
            // Create relation between OrganizationRole and ServiceOrganizationRole
            serviceOrganizationRole.setOrganizationRole(organizationRole);
            // Create relation between ServiceOrganizationRole and Service
            serviceOrganizationRole.setService(service);
            service.addServiceOrganizationRole(serviceOrganizationRole);
        }

        // Repository mocks
        if (orgOin != null && orgOin.equals("")) {
            when(organizationRepositoryMock.findByOin(orgOin)).thenReturn(Optional.empty());
        } else {
            Optional<Organization> optOrganization = Optional.of(organization);
            when(organizationRepositoryMock.findByOin(orgOin == null ?  "SSSSSSSSSSSSSSSSSSSS" : orgOin)).thenReturn(optOrganization);
        }

        if (repositoryUuid.equals("")) {
            when(serviceRepositoryMock.findFirstByServiceUuid(searchUuid)).thenReturn(Optional.empty());
        } else {
            Optional<Service> optService = Optional.of(service);
            when(serviceRepositoryMock.findFirstByServiceUuid(searchUuid)).thenReturn(optService);
        }
    }
}

