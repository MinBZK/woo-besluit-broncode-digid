
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import https.digid_nl.schema.aanvraagstation.*;
import nl.logius.digid.dws.client.AppClient;
import nl.logius.digid.dws.client.DigidXClient;
import nl.logius.digid.dws.util.XmlUtilsException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static nl.logius.digid.dws.utils.TestHelper.sendResourceSoapRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AppActivationEndpointTest extends AbstractAppActivationEndpointTest {

    @MockBean
    private AppClient mockAppClient;

    @Test
    public void ValidateAppActivationValidSignedMessageTest() throws IOException {
        String jsonString = "{\"status\":\"OK\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.OK);
    }

    @Test
    public void AppActivationValidSignedMessageTest() throws IOException {
        String jsonString = "{\"status\":\"OK\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.appActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        AppActivationResponse response = sendRequest(validSignedAppActivationRequest, "aanvraagstation_app_activation", AppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.OK);
    }

    @Test
    public void ValidateAppActivationSigningFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(invalidSignedValidateAppActivationRequest,"aanvraagstation_app_activation", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void AppActivationSigningFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(invalidSignedAppActivationRequest,"aanvraagstation_app_activation", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void ValidateAppActivationSigningFaultWithoutWSSecurityExceptionTest() throws IOException {
        Mockito.doThrow(new RuntimeException("A security error was encountered when verifying the message")).when(mockAppClient).appActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        ValidationErrorFaultDetail detail = sendRequest(validSignedAppActivationRequest,"aanvraagstation_app_activation", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void ValidateAppActivationValidationFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(SignedInvalidDocNrValidateAppActivationRequest,"aanvraagstation_app_activation", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void AppActivationValidationFaultTest() {
        ValidationErrorFaultDetail detail = sendRequest(SignedInvalidRequesterAppActivationRequest,"aanvraagstation_app_activation", ValidationErrorFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VALIDATION_ERROR);
    }

    @Test
    public void rawVersionMismatchFaultTest() {
        VersionMismatchFaultDetail detail = sendRequest(versionMismatchRequest,"aanvraagstation_app_activation", VersionMismatchFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.VERSION_MISMATCH);
    }

    @Test
    public void ValidateAppActivationOinMismatchFaultTest() {
        OinMismatchFaultDetail detail = sendRequest(SignedOinMismatchValidateAppActivationRequest,"aanvraagstation_app_activation", OinMismatchFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.OIN_MISMATCH);
    }

    @Test
    public void AppActivationOinMismatchFaultTest() {
        OinMismatchFaultDetail detail = sendRequest(SignedOinMismatchAppActivationRequest,"aanvraagstation_app_activation", OinMismatchFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.OIN_MISMATCH);
    }

    @Test
    public void ValidateAppActivationValidSignedEBsnNotValidMessageTest() throws IOException {
        String jsonString = "{\"status\":\"NOK\", \"error_code\":\"E_BSN_NOT_VALID\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.NOK);
        assertEquals(response.getErrorCode(), ErrorCodeType.E_BSN_NOT_VALID);
    }

    @Test
    public void ValidateAppActivationValidSignedEDocumentNotValidMessageTest() throws IOException {
        String jsonString = "{\"status\":\"NOK\", \"error_code\":\"E_DOCUMENT_NOT_VALID\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.NOK);
        assertEquals(response.getErrorCode(), ErrorCodeType.E_DOCUMENT_NOT_VALID);
    }

    @Test
    public void ValidateAppActivationValidSignedEDigidAccountNotValidMessageTest() throws IOException {
        String jsonString = "{\"status\":\"NOK\", \"error_code\":\"E_DIGID_ACCOUNT_NOT_VALID\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.NOK);
        assertEquals(response.getErrorCode(), ErrorCodeType.E_DIGID_ACCOUNT_NOT_VALID);
    }

    @Test
    public void ValidateAppActivationValidSignedEAppActivationCodeNotFoundMessageTest() throws IOException {
        String jsonString = "{\"status\":\"NOK\", \"error_code\":\"E_APP_ACTIVATION_CODE_NOT_FOUND\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.NOK);
        assertEquals(response.getErrorCode(), ErrorCodeType.E_APP_ACTIVATION_CODE_NOT_FOUND);
    }

    @Test
    public void ValidateAppActivationValidSignedEGeneralMessageTest() throws IOException {
        String jsonString = "{\"status\":\"NOK\", \"error_code\":\"E_GENERAL\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(jsonString);

        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        ValidateAppActivationResponse response = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", ValidateAppActivationResponse.class);
        assertEquals(response.getResponseMessage(), ResponseMessageType.NOK);
        assertEquals(response.getErrorCode(), ErrorCodeType.E_GENERAL);
    }

    @Test
    public void AppActivationValidSignedDigidNotRespondingMessageTest() throws IOException {
        Mockito.doThrow(new RuntimeException("Custom technical error")).when(mockAppClient).appActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        TechnicalFaultDetail detail = sendRequest(validSignedAppActivationRequest, "aanvraagstation_app_activation", TechnicalFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.TECHNICAL_FAULT);
    }

    @Test
    public void ValidateAppActivationValidSignedDigidNotRespondingMessageTest() throws IOException {
        Mockito.doThrow(new RuntimeException("Custom technical error")).when(mockAppClient).validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        TechnicalFaultDetail detail = sendRequest(validSignedValidateAppActivationRequest, "aanvraagstation_app_activation", TechnicalFaultDetail.class);
        assertEquals(detail.getFaultstring(), FaultstringType.TECHNICAL_FAULT);
    }

    @Test
    public void ValidateAppActivationEmptyResponseDigiDTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree("{}");
        final ObjectNode responseDigid = ((ObjectNode)actualObj);
        Mockito.when(mockAppClient.validateAppActivation(Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(responseDigid);

        XmlUtilsException exception = assertThrows(XmlUtilsException.class, () -> sendResourceSoapRequest(validSignedValidateAppActivationRequest, ValidateAppActivationResponse.class, serverPort, "aanvraagstation_app_activation"));
        assertEquals("No rootXml found in JAXBObjects ValidateAppActivationResponse", exception.getCause().getMessage());
        assertEquals("Error in unmarshalling JAXBObjects ValidateAppActivationResponse with SoapMessage.", exception.getMessage());
    }
}
