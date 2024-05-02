
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

package nl.logius.digid.mijn.backend.domain.manage.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.AsyncResult;

import nl.logius.digid.mijn.backend.client.digid.NsClient;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private NotificationService notificationService;

    @Mock
    private NsClient nsClient;


    @BeforeEach
    public void setup() {
        notificationService = new NotificationService(nsClient);
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(nsClient);
    }

    @Test
    public void testGetNotificationStatusHasNotifications() throws InterruptedException, ExecutionException {
        when(nsClient.AsyncGetUnreadNotifications(eq(1L))).thenReturn(new AsyncResult(5));

        int unreadNotifications = notificationService.asyncUnreadNotificationCount(1L).get();

        assertEquals(5, unreadNotifications);
    }


    @Test
    public void testGetNotificationStatusHasNoNotifications() throws InterruptedException, ExecutionException {
        when(nsClient.AsyncGetUnreadNotifications(eq(1L))).thenReturn(new AsyncResult(0));

        int unreadNotifications = notificationService.asyncUnreadNotificationCount(1L).get();

        assertEquals(0, unreadNotifications);
    }

}
