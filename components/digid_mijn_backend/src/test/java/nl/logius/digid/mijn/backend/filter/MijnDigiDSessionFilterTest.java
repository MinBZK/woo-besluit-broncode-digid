
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

package nl.logius.digid.mijn.backend.filter;

import nl.logius.digid.mijn.backend.domain.session.MijnDigidSession;
import nl.logius.digid.mijn.backend.domain.session.MijnDigidSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@ExtendWith(MockitoExtension.class)
class MijnDigiDSessionFilterTest {

    private MijnDigiDSessionFilter mijnDigiDSessionFilter;

    private MockMvc mockMvc;

    @Mock
    private MijnDigidSessionRepository mijnDigiDSessionRepository;

    @BeforeEach
    public void setup() {
        mijnDigiDSessionFilter = new MijnDigiDSessionFilter(mijnDigiDSessionRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new Object()).addFilter(mijnDigiDSessionFilter).build();
    }

    @AfterEach
    public void verifyAfterEach() {
        verifyNoMoreInteractions(mijnDigiDSessionRepository);
    }

    @Test
    void testNoSessionHeader() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/account/")).andReturn();

        Assertions.assertEquals(mvcResult.getResponse().getStatus(), HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testUnknownSessionHeader() throws Exception {
        String mijnDigiDSessionId = "unknown";
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = get("/account");
        mockHttpServletRequestBuilder.header(MijnDigidSession.MIJN_DIGID_SESSION_HEADER, "unknown");

        when(this.mijnDigiDSessionRepository.findById(eq(mijnDigiDSessionId))).thenReturn(Optional.empty());

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andReturn();

        verify(this.mijnDigiDSessionRepository, times(1)).findById(eq(mijnDigiDSessionId));

        Assertions.assertEquals(mvcResult.getResponse().getStatus(), HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testSessionInvalid() throws Exception {
        MijnDigidSession mijnDigiDSession = new MijnDigidSession(100);
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = get("/account");
        mockHttpServletRequestBuilder.header(MijnDigidSession.MIJN_DIGID_SESSION_HEADER, mijnDigiDSession.getId());

        when(this.mijnDigiDSessionRepository.findById(eq(mijnDigiDSession.getId()))).thenReturn(Optional.of(mijnDigiDSession));

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andReturn();

        verify(this.mijnDigiDSessionRepository, times(1)).findById(eq(mijnDigiDSession.getId()));

        Assertions.assertEquals(mvcResult.getResponse().getStatus(), HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testSessionValid() throws Exception {
        MijnDigidSession mijnDigiDSession = new MijnDigidSession(100);
        mijnDigiDSession.setAuthenticated(true);
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = get("/account");
        mockHttpServletRequestBuilder.header(MijnDigidSession.MIJN_DIGID_SESSION_HEADER, mijnDigiDSession.getId());

        when(this.mijnDigiDSessionRepository.findById(eq(mijnDigiDSession.getId()))).thenReturn(Optional.of(mijnDigiDSession));

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andReturn();

        verify(this.mijnDigiDSessionRepository, times(1)).findById(eq(mijnDigiDSession.getId()));

        // Not found as the actual endpoint does not exist in an empty controller
        Assertions.assertEquals(mvcResult.getResponse().getStatus(), HttpStatus.NOT_FOUND.value());
    }

}
