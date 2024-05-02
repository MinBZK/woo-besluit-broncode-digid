
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

package nl.logius.digid.app.domain.pincodereset.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.SharedServiceClient;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.pincodereset.flow.flows.PincodeResetFlow;
import nl.logius.digid.app.domain.pincodereset.flow.step.InitializePincodeReset;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import nl.logius.digid.app.shared.request.AuthSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class InitializePincodeResetTest {

    protected static final PincodeResetFlow mockedPincodeResetFlow = mock(PincodeResetFlow.class);
    protected AuthSessionRequest mockedAppRequest;

    @Mock
    protected DigidClient digidClient;

    @Mock
    protected SharedServiceClient sharedServiceClient;

    @Mock
    protected SwitchService switchService;

    @Mock
    protected AppSessionService appSessionService;

    @Mock
    protected AppAuthenticatorService appAuthenticatorService;

    @Mock
    protected AppSession appSession;

    private InitializePincodeReset initializePincodeReset;

    @BeforeEach
    public void setup(){
        initializePincodeReset = new InitializePincodeReset(digidClient, switchService, appSessionService, appAuthenticatorService);
    }

    @Test
    public void processInitializePincodeResetSwitchesTest() {
        initializePincodeReset.setAppSession(appSession);
        when(switchService.digidAppSwitchEnabled()).thenReturn(false);

        assertThrows(SwitchDisabledException.class, () ->
            initializePincodeReset.process(mockedPincodeResetFlow, mockedAppRequest));
    }
}
