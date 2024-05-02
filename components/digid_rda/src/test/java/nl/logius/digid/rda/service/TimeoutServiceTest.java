
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

package nl.logius.digid.rda.service;

import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.test.util.ReflectionTestUtils;

class TimeoutServiceTest {
    @Mock
    private ConfirmService confirmService;
    @SuppressWarnings("rawtypes")
    @Mock
    private RedisKeyExpiredEvent event;
    @InjectMocks
    private TimeoutService timeoutService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(timeoutService, "confirmService", confirmService);
    }

    @Test
    void testHandleRedisKeyExpiredEventStatusInitialized() {
        RdaSession session = new RdaSession();
        session.setStatus(Status.INITIALIZED);
        session.setReturnUrl("http://localhost");
        session.setConfirmId("id");
        session.setConfirmSecret("secret");
        Mockito.when(event.getValue()).thenReturn(session);

        timeoutService.handleRedisKeyExpiredEvent(event);
        Mockito.verify(confirmService).sendConfirm(session.getReturnUrl(), "id", "secret", false, session.getApp(), RdaError.TIMEOUT);
    }

    @Test
    void testHandleRedisKeyExpiredEventStatusFailed() {
        RdaSession session = new RdaSession();
        session.setStatus(Status.FAILED);
        session.setReturnUrl("http://localhost");
        session.setId("id");
        session.setConfirmSecret("secret");
        Mockito.when(event.getValue()).thenReturn(session);

        timeoutService.handleRedisKeyExpiredEvent(event);
        Mockito.verify(confirmService, Mockito.never()).sendConfirm(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), Mockito.eq(false), Mockito.isNotNull(), Mockito.eq(RdaError.TIMEOUT));
    }
}
