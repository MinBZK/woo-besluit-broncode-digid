
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

package nl.logius.digid.rda.controller;

import com.google.common.collect.ImmutableList;
import nl.logius.digid.rda.Application;
import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.models.MrzInfo;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.rest.app.AppRequest;
import nl.logius.digid.rda.models.rest.digid.CreateRequest;
import nl.logius.digid.rda.models.rest.digid.CreateResponse;
import nl.logius.digid.rda.repository.RdaSessionRepository;
import nl.logius.digid.rda.validations.IpValidations;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class IapiControllerTest {

    @MockBean
    private RdaSessionRepository sessionRepo;

    @MockBean
    private IpValidations ipValidations;

    @InjectMocks
    private IapiController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(controller, "timeout", 600);
        ReflectionTestUtils.setField(controller, "publicUrl", "SSSSSSSSSSSSSSSSSSSSSS");
    }

    @Test
    public void testCancelRestService() {
        AppRequest appRequest = new AppRequest();
        appRequest.setSessionId("sessionId");

        RdaSession session = new RdaSession();
        session.setId("sessionId");
        mockSession(session);
        Map<String, String> responseData = controller.cancel(appRequest);
        assertEquals("OK", responseData.get("status"));
        Mockito.verify(sessionRepo, Mockito.times(1)).save(Mockito.isA(RdaSession.class));
    }

    @Test
    public void testAbortRestService() {
        AppRequest appRequest = new AppRequest();
        appRequest.setSessionId("sessionId");

        RdaSession session = new RdaSession();
        session.setId("sessionId");
        mockSession(session);
        Map<String, String> responseData = controller.abort(appRequest);
        assertEquals("OK", responseData.get("status"));
        Mockito.verify(sessionRepo, Mockito.times(1)).save(Mockito.isA(RdaSession.class));
    }

    @Test
    public void testCreateRestService() {

        CreateRequest request = new CreateRequest();
        request.setDrivingLicences(ImmutableList.of("dl"));
        request.setTravelDocuments(ImmutableList.of(new MrzInfo("1", "1", "1")));
        request.setReturnUrl("url");
        request.setConfirmId("id");
        request.setClientIpAddress("1");
        CreateResponse response = controller.create(request);
        assertEquals(600, response.getExpiration());
        assertEquals("SSSSSSSSSSSSSSSSSSSSSS", response.getUrl());
        assertNotNull(response.getConfirmSecret());
        assertNotNull(response.getSessionId());

    }

    @Test
    public void testCreateRestServiceWithError() {
        CreateRequest request = new CreateRequest();
        request.setDrivingLicences(new ArrayList<>());
        request.setTravelDocuments(new ArrayList<>());
        Exception exception = assertThrows(BadRequestException.class, () -> {
            controller.create(request);;
        });
        assertEquals("No card information specified", exception.getMessage());

    }

    private void mockSession(RdaSession session) {
        Optional<RdaSession> optional = Optional.of(session);
        Mockito.when(sessionRepo.findById(session.getId())).thenReturn(optional);
    }

}
