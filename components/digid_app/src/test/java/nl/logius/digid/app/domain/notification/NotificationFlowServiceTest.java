
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

package nl.logius.digid.app.domain.notification;

import nl.logius.digid.app.client.NsClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.flow.*;
import nl.logius.digid.app.domain.notification.flow.Action;
import nl.logius.digid.app.domain.notification.flow.*;
import nl.logius.digid.app.domain.notification.request.NotificationUpdateRequest;
import nl.logius.digid.app.shared.request.AppSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static nl.logius.digid.app.domain.notification.flow.Action.GET_NOTIFICATIONS;
import static nl.logius.digid.app.domain.notification.flow.Action.UPDATE_NOTIFICATION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class NotificationFlowServiceTest {

    @Mock
    private FlowFactoryFactory flowFactoryFactory;

    @Mock
    private NsClient nsClient;

    @InjectMocks
    private NotificationFlowService notificationFlowService;

    @Test
    void processUpdateNotificationTest() throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        Action action = UPDATE_NOTIFICATION;
        NotificationUpdateRequest request = mock(NotificationUpdateRequest.class);
        FlowFactory flowFactory = mock(FlowFactory.class);
        AbstractFlowStep flowStep = mock(AbstractFlowStep.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getStep(any())).thenReturn(flowStep);

        notificationFlowService.processNotification(action, request);

        verify(flowFactoryFactory, times(1)).getFactory(NotificationFlowFactory.TYPE);
        verify(flowFactory, times(1)).getStep(action);
        verify(flowStep, times(1)).process(null, request);
    }

    @Test
    void deregisterAppsTest() {
        AppAuthenticator appAuthenticator = new AppAuthenticator();
        appAuthenticator.setUserAppId("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");

        List<AppAuthenticator> appAuthenticators = List.of(appAuthenticator, appAuthenticator, appAuthenticator);

        notificationFlowService.deregisterAppsWhenDeleting(appAuthenticators);

        verify(nsClient, times(3)).deregisterApp("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
    }

    @Test
    void processFetNotificationTest() throws NoSuchAlgorithmException, FlowNotDefinedException, FlowStateNotDefinedException, IOException, SharedServiceClientException {
        Action action = GET_NOTIFICATIONS;
        AppSessionRequest request = mock(AppSessionRequest.class);
        FlowFactory flowFactory = mock(FlowFactory.class);
        AbstractFlowStep flowStep = mock(AbstractFlowStep.class);

        when(flowFactoryFactory.getFactory(any())).thenReturn(flowFactory);
        when(flowFactory.getStep(any())).thenReturn(flowStep);

        notificationFlowService.processNotification(action, request);

        verify(flowFactoryFactory, times(1)).getFactory(NotificationFlowFactory.TYPE);
        verify(flowFactory, times(1)).getStep(action);
        verify(flowStep, times(1)).process(null, request);
    }


}
