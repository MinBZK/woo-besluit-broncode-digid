
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

package nl.logius.digid.dws.integration;

import https.digid_nl.schema.mu_pin_reset.RegisterPinResetPort;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetRequest;
import nl.logius.digid.dws.TestPinResetApplication;
import nl.logius.digid.dws.repository.PenRequestRepository;
import nl.logius.digid.dws.service.NotificationService;
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

import static nl.logius.digid.dws.utils.TestHelper.sendResourceSoapRequest;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes={
        TestPinResetApplication.class})
@ActiveProfiles({ "default", "integration-test" })
public abstract class AbstractRegisterPinRequestEndpointTest {

    @SpyBean
    protected NotificationService notificationService;

    @Autowired
    protected RegisterPinResetPort registerPinResetPortService;

    @Autowired
    protected PenRequestRepository repository;

    @Autowired
    protected Flyway flyway;

    @Value(value="classpath:requests/ValidRequest.xml")
    protected Resource validRequest;

    @Value(value="classpath:requests/ValidPenResponseFromRdw.xml")
    protected Resource validResponseFromRdw;

    @Value(value="classpath:requests/VersionMismatchRequest.xml")
    protected Resource versionMismatchRequest;

    @Value(value="classpath:requests/NoAddressingRequest.xml")
    protected Resource noAddressingRequest;

    @Value(value="classpath:requests/ActivateRequest.xml")
    protected Resource activateRequest;

    @Value(value="classpath:requests/ValidationErrorRequest.xml")
    protected Resource validationErrorRequest;

    @Value("${ws.server.pin_reset.version}")
    protected String wsVersion;

    @Value("${ws.server.pin_reset.xsd-file-name}")
    protected String xsdFileName;

    @Value(value="classpath:ws/${ws.server.pin_reset.xsd-file-name}")
    protected Resource xsdSchema;

    @LocalServerPort
    protected int serverPort;

    @BeforeEach
    public void init() throws Exception {
        flyway.clean();
        flyway.migrate();

        Mockito.doCallRealMethod().when(notificationService).updateStatus(Mockito.any(RegisterPinResetRequest.class));
    }

    protected <T> T sendRequest(Resource request, String service, Class<T> responseClass) {
        T detail = null;
        try{
            detail = sendResourceSoapRequest(request, responseClass, serverPort, service);
        } catch(Exception e ) {
            fail("Exception raised:" + e.getMessage());
        }

        return detail;
    }
}
