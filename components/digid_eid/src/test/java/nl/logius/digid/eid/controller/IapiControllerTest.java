
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

package nl.logius.digid.eid.controller;

import nl.logius.digid.eid.Application;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.config.BsnkServiceConfig;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.rest.digid.CancelRequest;
import nl.logius.digid.eid.models.rest.digid.StartProcessRequest;
import nl.logius.digid.eid.models.rest.digid.StartProcessResponse;
import nl.logius.digid.eid.repository.CRLRepository;
import nl.logius.digid.eid.repository.CertificateRepository;
import nl.logius.digid.eid.repository.EidSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class, BsnkServiceConfig.class })
@ActiveProfiles({ "default", "unit-test" })
public class IapiControllerTest extends BaseTest {
    @MockBean
    private CertificateRepository certificateRepository;
    @MockBean
    private CRLRepository crlRepository;
    @MockBean
    private EidSessionRepository sessionRepo;
    @InjectMocks
    private IapiController controller;

    @BeforeEach
    public void init() {
        // init mocks
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(controller, "timeout", 600);
        ReflectionTestUtils.setField(controller, "publicUrl", "SSSSSSSSSSSSSSSSSSSSSS");
        ReflectionTestUtils.setField(controller, "whitelistPattern", Pattern.compile("localhost"));
    }

    @Test
    public void testStartProcessRestService() {
        StartProcessRequest request = new StartProcessRequest();
        request.setReturnUrl("http://localhost");
        request.setConfirmId("confirmId");

        // the test
        StartProcessResponse response = controller.startProcessRestService(request);
        // asserts
        assertNotNull(response.getConfirmSecret());
        assertNotNull(response.getSessionId());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSS", response.getUrl());
        assertEquals(600, response.getExpiration());
    }

    @Test
    public void testCancelRestService() {
        CancelRequest request = new CancelRequest();

        request.setSessionId("id");
        getSession("id", new EidSession());
        Mockito.doNothing().when(sessionRepo).deleteById("id");

        Map<String, String> response = controller.cancelRestService(request);
        assertEquals("OK", response.get("arrivalStatus"));

    }

    private void getSession(String sessionId, EidSession session) {
        Optional<EidSession> optional = Optional.of(session);
        Mockito.when(sessionRepo.findById(sessionId)).thenReturn(optional);
    }
}
