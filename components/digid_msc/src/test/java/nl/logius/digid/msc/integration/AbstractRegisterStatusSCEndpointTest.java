
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

package nl.logius.digid.msc.integration;

import static nl.logius.digid.msc.utils.TestHelper.sendResourceSoapRequest;

import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import https.digid_nl.schema.mu_status_controller.RegisterSCStatusPort;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCRequest;
import nl.logius.digid.msc.TestWebServiceApplication;
import nl.logius.digid.msc.repository.DocumentStatusRepository;
import nl.logius.digid.msc.config.DecryptTestConfig;
import nl.logius.digid.msc.service.DocumentStatusService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={
        TestWebServiceApplication.class, DecryptTestConfig.class })
@ActiveProfiles({ "default", "integration-test" })
public abstract class AbstractRegisterStatusSCEndpointTest {

    @SpyBean
    protected DocumentStatusService documentStatusService;

    @Autowired
    protected RegisterSCStatusPort registerSCStatusPortService;

    @Autowired
    protected DocumentStatusRepository repository;

    @Autowired
    protected Flyway flyway;

    @Value(value="classpath:requests/ValidRequest.xml")
    protected Resource validRequest;

    @Value(value="classpath:requests/VersionMismatchRequest.xml")
    protected Resource versionMismatchRequest;

    @Value(value="classpath:requests/NoAddressingRequest.xml")
    protected Resource noAddressingRequest;

    @Value(value="classpath:requests/ActivateRequest.xml")
    protected Resource activateRequest;

    @Value(value="classpath:requests/ValidationErrorRequest.xml")
    protected Resource validationErrorRequest;

    @Value("${ws.version}")
    protected String wsVersion;

    @Value("${ws.xsd-file-name}")
    protected String xsdFileName;

    @Value(value="classpath:ws/${ws.xsd-file-name}l")
    protected Resource xsdSchema;

    @LocalServerPort
    protected int serverPort;

    @BeforeEach
    public void init() throws Exception {
        flyway.clean();
        flyway.migrate();

        Mockito.doCallRealMethod().when(documentStatusService).updateStatus(Mockito.any(RegisterStatusSCRequest.class));
    }

    protected <T> T sendRequest(Resource request, Class<T> responseClass) {
        T detail = null;
        try{
            detail = sendResourceSoapRequest(request, responseClass, serverPort);
        } catch(Exception e ) {
            fail("Exception raised:" + e.getMessage());
        }

        return detail;
    }
}
