
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

package service;


import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.client.KernClient;
import nl.logius.digid.ns.client.LogEnum;
import nl.logius.digid.ns.client.MnsStatus;
import nl.logius.digid.ns.client.MobileNotificationServerClient;
import nl.logius.digid.ns.controller.utils.ReturnStatus;
import nl.logius.digid.ns.model.AppNotification;
import nl.logius.digid.ns.model.MnsResponse;
import nl.logius.digid.ns.model.Registration;
import nl.logius.digid.ns.repository.RegistrationRepository;
import nl.logius.digid.ns.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class RegistrationServiceTest {

    @Mock
    MobileNotificationServerClient mnsClient;

    @Mock
    KernClient kernClient;

    String deviceToken;
    String user;
    int platform;

    @Mock
    private RegistrationRepository repository;

    @InjectMocks
    private RegistrationService service;

    private Registration registration;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        registration = new Registration();
        registration.setAppId("app_id");
        registration.setAccountId(1L);
        registration.setNotificationId("notification_id");
        registration.setOsType(1);
        registration.setOsVersion("11.1");
        registration.setDeviceName("device_name");
        registration.setReceiveNotifications(true);
        deviceToken = registration.getNotificationId();
        user = registration.getNotificationId();
        platform = registration.getOsType();
    }

    @Test
    public void testRegistrationAndDeregistrationOK() {
        ReturnStatus result = service.saveRegistration(registration);
        assertEquals(ReturnStatus.OK, result);

        ReturnStatus resultDeregistration = service.deleteRegistration(registration);
        assertEquals(ReturnStatus.OK, resultDeregistration);
    }

    @Test
    public void testRegistrationNOK() {
        ReturnStatus result = service.saveRegistration(new Registration());
        assertEquals(ReturnStatus.NOK, result);
    }

    @Test
    public void testRegistrationMnsOK() {
        when(mnsClient.sendRegistration(registration)).thenReturn(new MnsResponse(MnsStatus.OK, null, null));
        service.asyncSendRegistrationToMns(registration);
        verify(mnsClient).sendRegistration(registration);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.REGISTER_MNS_OK), anyMap());
        assertTrue(registration.isRegistrationSucceeded());
        verify(repository, times(1)).save(registration);
    }

    @Test
    public void testDeregistrationMnsOK() {
        registration.setRegistrationSucceeded(true);
        when(mnsClient.sendDeregistration(deviceToken)).thenReturn(new MnsResponse(MnsStatus.OK, null, null));
        service.asyncDeleteRegistrationFromMns(registration, registration.getNotificationId());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.DEREGISTER_MNS_OK), anyMap());
        assertFalse(registration.isRegistrationSucceeded());
    }

    @Test
    public void testRegistrationMnsSwitchOff() {
        when(mnsClient.sendRegistration(registration)).thenReturn(new MnsResponse(MnsStatus.SWITCH_OFF, null, null));
        service.asyncSendRegistrationToMns(registration);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.MNS_SWITCH_OFF), anyMap());
    }

    @Test
    public void testDeregistrationMnsSwitchOff() {
        when(mnsClient.sendDeregistration(deviceToken)).thenReturn(new MnsResponse(MnsStatus.SWITCH_OFF, null, null));
        service.asyncDeleteRegistrationFromMns(registration, registration.getNotificationId());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.MNS_SWITCH_OFF), anyMap());
    }

    @Test
    public void testSendRegistrationstoMns() {
        List<AppNotification> appNotifications = new ArrayList<>();
        AppNotification appNotification = new AppNotification();
        appNotification.setRegistration(registration);
        appNotifications.add(appNotification);
        when(mnsClient.sendRegistration(registration)).thenReturn(new MnsResponse(MnsStatus.OK, null, null));
        service.sendRegistrationsToMnsIfNotRegisteredYet(appNotifications);
        verify(mnsClient).sendRegistration(registration);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.REGISTER_MNS_OK), anyMap());
        assertTrue(appNotification.getRegistration().isRegistrationSucceeded());
        verify(repository, times(1)).save(registration);
    }

    @Test
    public void testRegistrationMnsError() {
        when(mnsClient.sendRegistration(registration)).thenReturn(new MnsResponse(MnsStatus.ERROR, null, null));
        service.asyncSendRegistrationToMns(registration);
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.REGISTER_MNS_ERROR), anyMap());
    }

    @Test
    public void testDeregistrationMnsError() {
        when(mnsClient.sendDeregistration(deviceToken)).thenReturn(new MnsResponse(MnsStatus.ERROR, null, null));
        service.asyncDeleteRegistrationFromMns(registration, registration.getNotificationId());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.DEREGISTER_MNS_ERROR), anyMap());
    }

    @Test
    public void testDeleteRegistrations() {
        registration.setRegistrationSucceeded(true);
        List<Registration> registrations = new ArrayList<>();
        registrations.add(registration);
        when(mnsClient.sendDeregistration(deviceToken)).thenReturn(new MnsResponse(MnsStatus.OK, null, null));
        service.deleteRegistrations(registrations);
        assertFalse(registration.isRegistrationSucceeded());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.DEREGISTER_MNS_OK), anyMap());
        verify(repository, times(1)).deleteById(any());
        verify(mnsClient, times(1)).sendDeregistration(any(String.class));
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.DEREGISTER_APP_OK), anyMap());
        verify(kernClient, times(1)).remoteLog(eq(LogEnum.DEREGISTER_MNS_OK), anyMap());
    }
}
