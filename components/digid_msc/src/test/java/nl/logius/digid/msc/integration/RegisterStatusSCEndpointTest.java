
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

package nl.logius.digid.msc.integration;

import static nl.logius.digid.msc.utils.TestHelper.generateSoapRequest;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import https.digid_nl.schema.mu_status_controller.FaultstringType;
import https.digid_nl.schema.mu_status_controller.NotFoundFault;
import https.digid_nl.schema.mu_status_controller.NotFoundFaultDetail;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCRequest;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCResponse;
import https.digid_nl.schema.mu_status_controller.ResponseMessageType;
import https.digid_nl.schema.mu_status_controller.StatusType;
import https.digid_nl.schema.mu_status_controller.TechnicalFault;
import https.digid_nl.schema.mu_status_controller.TechnicalFaultDetail;
import https.digid_nl.schema.mu_status_controller.ValidationErrorFault;
import https.digid_nl.schema.mu_status_controller.ValidationErrorFaultDetail;
import https.digid_nl.schema.mu_status_controller.VersionMismatchFault;
import https.digid_nl.schema.mu_status_controller.VersionMismatchFaultDetail;
import nl.logius.digid.msc.model.DocumentStatus;
import nl.logius.digid.msc.util.XmlUtilsException;

import static org.junit.jupiter.api.Assertions.*;

public class RegisterStatusSCEndpointTest extends AbstractRegisterStatusSCEndpointTest {

    @Test
    public void registerNewStatusSCRequestTest() throws IOException, XmlUtilsException, NotFoundFault, TechnicalFault,
            ValidationErrorFault, VersionMismatchFault {

        RegisterStatusSCRequest request = generateSoapRequest(validRequest);
        RegisterStatusSCResponse response = registerSCStatusPortService.registerStatusSC(request);

        assertNotNull(response);
        assertNotNull(response.getDateTime());
        String[] versions = wsVersion.split(",");
        assertEquals(response.getMsgVersion(), versions[versions.length-1]);
        assertEquals(response.getResponseMessage(),ResponseMessageType.VERWERKT);
    }

    @Test
    public void registerUpdateStatusSCRequestsTest() throws IOException, XmlUtilsException, NotFoundFault, TechnicalFault,
            ValidationErrorFault, VersionMismatchFault {
        RegisterStatusSCRequest request = generateSoapRequest(validRequest);
        RegisterStatusSCResponse response = registerSCStatusPortService.registerStatusSC(request);
        assertEquals(response.getResponseMessage(),ResponseMessageType.VERWERKT);

        assertEquals(repository.count(), 1, "One record has been created"); // There is a new record created in the DB
        DocumentStatus ds = ((Optional<DocumentStatus>)repository.findById(new Long(1))).get();
        assertEquals(ds.getStatus(), StatusType.UITGEREIKT, "Status is Uitgereikt");

        request = generateSoapRequest(activateRequest);
        response = registerSCStatusPortService.registerStatusSC(request);
        assertEquals(response.getResponseMessage(),ResponseMessageType.VERWERKT);

        assertEquals(repository.count(), 1, "Current record has been updated"); // The record has been updated
        ds = ((Optional<DocumentStatus>)repository.findById(new Long(1))).get(); // reload record

        assertEquals(ds.getStatus(), StatusType.GEACTIVEERD, "Status is Geactiveerd");
    }

    // test real soap messages
    @Test
    public void rawValidTest() {
        RegisterStatusSCResponse response = sendRequest(validRequest, RegisterStatusSCResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.VERWERKT);
    }

    @Test
    public void rawValidationFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(validationErrorRequest, ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void rawVersionMismatchFaultTest() {
        VersionMismatchFaultDetail detail = sendRequest(versionMismatchRequest, VersionMismatchFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VERSION_MISMATCH);
    }

    @Test
    public void rawNotFoundFaultTest() {
        NotFoundFaultDetail detail = sendRequest(activateRequest, NotFoundFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.NOT_FOUND);
    }

    @Test
    public void rawTechnicalFaultTest() throws Exception {
       Mockito.doThrow(new Exception("Custom technical error")).when(documentStatusService)
               .updateStatus(Mockito.any(RegisterStatusSCRequest.class));

       TechnicalFaultDetail detail = sendRequest(validRequest, TechnicalFaultDetail.class);
       assertEquals(detail.getFaultstring(), FaultstringType.TECHNICAL_FAULT);
    }

    @Test
    public void noAddressingVersionMismatchFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(noAddressingRequest, ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }
}
