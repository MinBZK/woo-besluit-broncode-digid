
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

package controller;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.controller.RegistrationController;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.RegistrationRepository;
import nl.logius.digid.ns.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class RegisterControllerTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private  RegistrationService service;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    public void testRegistrationWithNotifications(){
        Registration registration = new Registration("appId", "notificationId",  1L, "deviceName", true, 1, "8");
        when(service.saveRegistration(registration)).thenReturn(ReturnStatus.OK);
        ResponseEntity<Map<String, String>> response = registrationController.register(registration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.OK.toString());
        verify(service).asyncSendRegistrationToMns(registration);
    }

    @Test
    public void testRegistrationWithoutNotifications(){
        Registration registration = new Registration("appId", "notificationId",  1L, "deviceName", false, 1, "8");
        when(service.saveRegistration(registration)).thenReturn(ReturnStatus.OK);
        ResponseEntity<Map<String, String>> response = registrationController.register(registration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.OK.toString());
        verify(service, times(0)).asyncSendRegistrationToMns(registration);

    }

    @Test
    public void testDeregistration(){
        Registration registration = new Registration("appId", "notificationId",  1L, "deviceName", true, 1, "8");
        when(service.findRegistrationByAppId("appId")).thenReturn(Optional.of(registration));
        when(service.deleteRegistration(registration)).thenReturn(ReturnStatus.OK);
        ResponseEntity<Map<String, String>> response = registrationController.deregister(registration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.OK.toString());
    }

    @Test
    public void testUpdateRegistrationWithNotifications(){
        Registration oldRegistration = new Registration("appId", "notificationId1",  1L, "deviceName", true, 1, "8");
        Registration newRegistration = new Registration("appId", "notificationId2",  1L, "deviceName", true, 1, "8");
        when(service.findRegistrationByAppId("appId")).thenReturn(Optional.of(oldRegistration));
        when(service.saveRegistration(any(Registration.class))).thenReturn(ReturnStatus.OK);
        ResponseEntity<Map<String, String>> response = registrationController.updateNotification(newRegistration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.OK.toString());
        verify(service).asyncSendRegistrationToMns(oldRegistration);
    }

    @Test
    public void testUpdateRegistrationWithoutNotifications(){
        Registration oldRegistration = new Registration("appId", "notificationId1",  1L, "deviceName", false, 1, "8");
        Registration newRegistration = new Registration("appId", "notificationId2",  1L, "deviceName", false, 1, "8");
        when(service.findRegistrationByAppId("appId")).thenReturn(Optional.of(oldRegistration));
        when(service.saveRegistration(any(Registration.class))).thenReturn(ReturnStatus.OK);
        ResponseEntity<Map<String, String>> response = registrationController.updateNotification(newRegistration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.OK.toString());
        verify(service, times(0)).asyncSendRegistrationToMns(oldRegistration);
    }

    @Test
    public void testUpdateRegistrationAppNotFound(){
        Registration newRegistration = new Registration("appId", "notificationId2",  1L, "deviceName", true, 1, "8");
        when(service.findRegistrationByAppId("appId")).thenReturn(Optional.empty());
        ResponseEntity<Map<String, String>> response = registrationController.updateNotification(newRegistration);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertTrue(response.getBody().containsKey("status"));
        assertEquals(response.getBody().get("status"), ReturnStatus.APP_NOT_FOUND.toString());
    }

}
