
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

import nl.logius.digid.dws.exception.BvBsnException;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import nl.logius.digid.dws.model.TravelDocumentRequest;
import nl.logius.digid.dws.service.BvBsnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class BvBsnIapiControllerTest {

    @InjectMocks
    private BvBsnIapiController controller;

    @Mock
    private BvBsnService BvBsnServiceMock;

    @Test
    public void checkBvBsnReturnOk() throws BvBsnException, SoapValidationException {
        Map<String,String> serviceResponse = new HashMap<>();
        serviceResponse.put("status", "OK");
        when(BvBsnServiceMock.verifyTravelDocument(any(TravelDocumentRequest.class))).thenReturn(serviceResponse);

        TravelDocumentRequest travelDocumentRequest= new TravelDocumentRequest();
        travelDocumentRequest.setDocumentType("ID_CARD");
        travelDocumentRequest.setDocumentNumber("1");
        Map<String, String> controllerResponse = controller.checkBvBsn(travelDocumentRequest);
        assertEquals("OK", controllerResponse.get("status"));
    }

    @Test
    public void handleBvBsnClientExceptionShouldReturnStatusNok() {
        Map<String, String> controllerResponse = controller.handleBvBsnClientException();
        assertEquals("NOK", controllerResponse.get("status"));
    }

    @Test
    public void handleSoapValidationExceptionShouldReturnStatusNok() {
        Map<String, String> controllerResponse = controller.handleSoapValidationException(new SoapValidationException("Soap Validation error"));
        assertEquals("NOK", controllerResponse.get("status"));
    }
}
