
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

package nl.logius.digid.mijn.backend.domain.deprecated.manage.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.logius.digid.mijn.backend.client.app.AppClient;
import nl.logius.digid.mijn.backend.client.app.AppSession;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.logs.DAccountLogsController;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.logs.DAccountLogsRequest;
import nl.logius.digid.mijn.backend.domain.deprecated.manage.logs.DAccountLogsResult;
import nl.logius.digid.mijn.backend.domain.manage.AccountService;
import nl.logius.digid.mijn.backend.domain.manage.logs.AccountLogsResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ExtendWith(MockitoExtension.class)
class DAccountControllerTest {

    private DAccountLogsController accountController;

    @Mock
    private AccountService accountService;

    @Mock
    private AppClient appClient;

    @BeforeEach
    public void setup() {
        accountController = new DAccountLogsController(appClient, accountService);
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(accountService, appClient);
    }


    @Test
    public void testExceptionHandler() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(accountController).setControllerAdvice(accountController).build();
        ObjectMapper mapper = new ObjectMapper();

        DAccountLogsRequest request = new DAccountLogsRequest();
        MvcResult mvcResult = mockMvc
                .perform(
                        post("/apps/logs/get_logs")
                                .content(mapper.valueToTree(request).toString())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andReturn();
        DAccountErrorMessage error = mapper.readValue(mvcResult.getResponse().getContentAsString(), DAccountErrorMessage.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus());
        assertEquals("Missing parameters.", error.getMessage());
    }

    @Test
    public void testNoSessionId() {
        DAccountLogsRequest accountLogsRequest = new DAccountLogsRequest();
        accountLogsRequest.setPageId(1);

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            accountController.getAccountLogs(accountLogsRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("missing parameters.", exc.getAccountErrorMessage().getMessage());
    }

    @Test
    public void testUnknownSessionId(){
        DAccountLogsRequest accountLogsRequest = new DAccountLogsRequest();
        accountLogsRequest.setAppSessionId("id");
        accountLogsRequest.setPageId(1);

        when(appClient.getAppSession(any())).thenReturn(Optional.empty());

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            accountController.getAccountLogs(accountLogsRequest);
        });

        assertEquals(HttpStatus.OK, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("NOK", exc.getAccountErrorMessage().getStatus());
        assertEquals("no_session", exc.getAccountErrorMessage().getError());
    }

    @Test
    public void testUnauthenticatedSessionId() {
        DAccountLogsRequest accountLogsRequest = new DAccountLogsRequest();
        accountLogsRequest.setAppSessionId("id");
        accountLogsRequest.setPageId(1);

        AppSession session = new AppSession();
        session.setState("NOT_AUTHENTICATED");
        when(appClient.getAppSession(any())).thenReturn(Optional.of(session));

        DAccountException exc = assertThrows(DAccountException.class, () -> {
            accountController.getAccountLogs(accountLogsRequest);
        });

        assertEquals(HttpStatus.OK, exc.getAccountErrorMessage().getHttpStatus());
        assertEquals("NOK", exc.getAccountErrorMessage().getStatus());
        assertEquals("no_session", exc.getAccountErrorMessage().getError());
    }

    @Test
    public void testValidRequest() {
        DAccountLogsRequest accountLogsRequest = new DAccountLogsRequest();
        accountLogsRequest.setAppSessionId("id");
        accountLogsRequest.setPageId(1);

        AppSession session = new AppSession();
        session.setState("AUTHENTICATED");
        session.setDeviceName("deviceName");
        session.setAppCode("appCode");
        session.setAccountId(12L);

        when(appClient.getAppSession(any())).thenReturn(Optional.of(session));
        when(accountService.getAccountLogs(anyLong(), anyString(), anyString(), any())).thenReturn(new AccountLogsResult());

        DAccountLogsResult accountLogs = accountController.getAccountLogs(accountLogsRequest);

        verify(accountService, times(1)).getAccountLogs(eq(12L), any(), any(), any());
    }



}
