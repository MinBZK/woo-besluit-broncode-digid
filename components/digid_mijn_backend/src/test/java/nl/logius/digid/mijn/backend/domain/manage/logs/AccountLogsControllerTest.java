
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

package nl.logius.digid.mijn.backend.domain.manage.logs;

import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.manage.Status;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSession;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLogsControllerTest {

    private AccountLogsController accountLogsController;

    private MijnDigidSession mijnDigiDSession;

    @Mock
    private MijnDigidSessionRepository mijnDigiDSessionRepository;

    @Mock
    private AccountService accountService;

    @BeforeEach
    public void setup() {
        accountLogsController = new AccountLogsController(mijnDigiDSessionRepository, accountService);
        mijnDigiDSession = new MijnDigidSession(1L);
        mijnDigiDSession.setAccountId(12l);
        mijnDigiDSession.setAuthenticated(true);
        mijnDigiDSession.setAppCode("appCode");
        mijnDigiDSession.setDeviceName("deviceName");

        lenient().when(mijnDigiDSessionRepository.findById(mijnDigiDSession.getId())).thenReturn(Optional.of(mijnDigiDSession));
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(accountService);
    }

    @Test
    public void testValidRequest() {
        AccountLogsRequest request = new AccountLogsRequest();
        request.setPageId(1);

        AccountLogsResult result = new AccountLogsResult();
        result.setTotalItems(10);
        result.setTotalPages(1);
        List<AccountLog> results = new ArrayList<>();
        result.setResults(results);
        result.setStatus(Status.OK);
        result.setError("error");

        when(accountService.getAccountLogs(anyLong(), anyString(), anyString(), any())).thenReturn(result);

        AccountLogsResult accountLogs = accountLogsController.getAccountLogs(request, mijnDigiDSession.getId());

        assertEquals(Status.OK, accountLogs.getStatus());
        assertEquals("error", accountLogs.getError());
        assertEquals(10, accountLogs.getTotalItems());
        assertEquals(1, accountLogs.getTotalPages());
        assertEquals(results, accountLogs.getResults());
    }

}
