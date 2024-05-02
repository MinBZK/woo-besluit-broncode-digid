
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

package nl.logius.digid.dws.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.dws.model.BsnkActivateRequest;
import nl.logius.digid.dws.model.BsnkActivateResponse;
import nl.logius.digid.dws.service.BsnkService;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class BsnkIapiControllerTest {

    @InjectMocks
    private BsnkIapiController controller;

    @Mock
    private BsnkService bsnkActivateService;

    @Test
    public void bsnkActivateResponseOkTest() throws BsnkException {
        BsnkActivateRequest request = new BsnkActivateRequest();
        request.setBsn("PPPPPPPPP");

        Mockito.when(bsnkActivateService.bsnkActivate(any())).thenReturn("pip");

        BsnkActivateResponse result = controller.bsnkActivate(request);
        assertEquals("OK", result.getStatus());
        assertEquals("pip", result.getPip());
    }

    @Test
    public void bsnkActivateResponseNOkTest() throws BsnkException {
        BsnkException ex = new BsnkException("SomeFault", "Some error occurred", null);

        BsnkActivateResponse result = controller.handleBvBsnClientException(ex);
        assertEquals("NOK", result.getStatus());
        assertEquals("SomeFault", result.getFaultReason());
        assertEquals("Some error occurred", result.getFaultDescription());
    }

    @Test
    public void bsnkActivateResponseNOkWithCauseTest() throws BsnkException {
        BsnkException ex = new BsnkException("SomeFault", "Some error occurred", new Exception("Some exception"));

        BsnkActivateResponse result = controller.handleBvBsnClientException(ex);
        assertEquals("NOK", result.getStatus());
        assertEquals("SomeFault", result.getFaultReason());
        assertEquals("Some error occurred", result.getFaultDescription());
    }
}
