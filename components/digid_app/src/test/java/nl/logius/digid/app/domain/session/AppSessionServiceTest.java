
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

package nl.logius.digid.app.domain.session;

import nl.logius.digid.app.Application;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
class AppSessionServiceTest {
    protected static final String T_APP_SESSION_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_DEVICE_NAME = "PPPPPPPPPPPPPPP";
    protected static final String T_USER_APP_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    protected static final String T_INSTANCE_ID = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    @Autowired
    private AppSessionService service;

    @Autowired
    private AppSessionRepository repository;

    @Test
    void removeByInstanceIdAndIdNotTest() {
        // persist app session
        AppSession session = new AppSession();
        session.setId(T_APP_SESSION_ID);
        session.setFlow(AuthenticateLoginFlow.NAME);
        session.setState("AUTHENTICATED");
        session.setUserAppId(T_USER_APP_ID);
        session.setInstanceId(T_INSTANCE_ID);
        session.setDeviceName(T_DEVICE_NAME);

        repository.save(session);

        // Given app session is created
        assertTrue(repository.findById(T_APP_SESSION_ID).isPresent());
        // Should not be removed when removing with same instanceId and appSessionId
        service.removeByInstanceIdAndIdNot(T_INSTANCE_ID, T_APP_SESSION_ID);
        assertTrue(repository.findById(T_APP_SESSION_ID).isPresent());
        // Old session should be removed when removing with same instanceId and new appSessionId
        service.removeByInstanceIdAndIdNot(T_INSTANCE_ID, T_APP_SESSION_ID + "1");
        assertFalse(repository.findById(T_APP_SESSION_ID).isPresent());
    }
}
