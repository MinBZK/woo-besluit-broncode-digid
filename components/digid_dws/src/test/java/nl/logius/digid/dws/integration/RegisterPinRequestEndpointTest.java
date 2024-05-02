
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

package nl.logius.digid.dws.integration;

import https.digid_nl.schema.mu_pin_reset.*;
import nl.logius.digid.dws.service.NotificationService;
import nl.logius.digid.dws.util.XmlUtilsException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static nl.logius.digid.dws.utils.TestHelper.generateSoapRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class RegisterPinRequestEndpointTest extends AbstractRegisterPinRequestEndpointTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void registerPinResetRequestTestFromRdw() throws IOException, XmlUtilsException, NotFoundFault, TechnicalFault,
        ValidationErrorFault, VersionMismatchFault {

        RegisterPinResetRequest request = generateSoapRequest(validRequest);
        RegisterPinResetResponse response = registerPinResetPortService.registerPinReset(request);

        assertNotNull(response);
        assertNotNull(response.getDateTime());
        assertEquals(response.getMsgVersion(), wsVersion);
        assertEquals(response.getResponseMessage(),ResponseMessageType.VERWERKT);
        assertEquals(repository.count(), 1, "One record has been created"); // There is a new record created in the DB
    }


    // test real soap messages
    @Test
    public void rawValidTest() {
        RegisterPinResetResponse response = sendRequest(validRequest, "pin_reset", RegisterPinResetResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.VERWERKT);
    }

    @Test
    public void rawValidationFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(validationErrorRequest, "pin_reset", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void rawValidationFaultWithoutUnmarshalExceptionTest() throws Exception {
        Mockito.doThrow(new RuntimeException("Unexpected wrapper element")).when(notificationService).updateStatus(any(RegisterPinResetRequest.class));
        ValidationErrorFaultDetail detail = sendRequest(validRequest, "pin_reset", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void rawVersionMismatchFaultTest() {
        VersionMismatchFaultDetail detail = sendRequest(versionMismatchRequest, "pin_reset", VersionMismatchFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VERSION_MISMATCH);
    }

    @Test
    public void rawTechnicalFaultTest() throws Exception {
        Mockito.doThrow(new Exception("Custom technical error")).when(notificationService)
                .updateStatus(Mockito.any(RegisterPinResetRequest.class));

        TechnicalFaultDetail detail = sendRequest(validRequest, "pin_reset", TechnicalFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.TECHNICAL_FAULT);
    }

    @Test
    public void noAddressingVersionMismatchFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(noAddressingRequest, "pin_reset", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }
}
