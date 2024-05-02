
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

package nl.logius.digid.app.domain.confirmation.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.confirmation.ConfirmationController;
import nl.logius.digid.app.domain.confirmation.ConfirmationFlowService;
import nl.logius.digid.app.domain.confirmation.request.ConfirmRequest;
import nl.logius.digid.app.domain.confirmation.request.MultipleSessionsRequest;
import nl.logius.digid.app.domain.flow.FlowNotDefinedException;
import nl.logius.digid.app.domain.flow.FlowStateNotDefinedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConfirmationControllerTest {

    private ConfirmationController confirmationController;

    @Mock
    private ConfirmationFlowService confirmationFlowService;

    @BeforeEach
    public void setup() {
        confirmationController = new ConfirmationController(confirmationFlowService);
    }

    @Test
    void infoTest() throws FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException {
        MultipleSessionsRequest multipleSessionsRequest = new MultipleSessionsRequest();

        confirmationController.info(multipleSessionsRequest);

        verify(confirmationFlowService, times(1)).processAction(anyString(), any(nl.logius.digid.app.domain.confirmation.flow.Action.class), any(MultipleSessionsRequest.class));

    }

    @Test
    void userConfirmTest() throws FlowNotDefinedException, SharedServiceClientException, NoSuchAlgorithmException, IOException, FlowStateNotDefinedException {
        ConfirmRequest confirmRequest = new ConfirmRequest();

        confirmationController.confirm(confirmRequest);

        verify(confirmationFlowService, times(1)).processAction(anyString(), any(nl.logius.digid.app.domain.confirmation.flow.Action.class), any(ConfirmRequest.class));

    }
}
