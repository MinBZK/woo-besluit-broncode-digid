
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

package nl.logius.digid.dws.ws;

import com.fasterxml.jackson.databind.node.ObjectNode;
import https.digid_nl.schema.aanvraagstation.*;
import nl.logius.digid.dws.client.AppClient;
import nl.logius.digid.dws.client.DigidXClient;
import org.apache.cxf.annotations.SchemaValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SchemaValidation(schemas = "${ws.server.rvig-aanvraagstation.xsd-file-name}")
public class AppActivationEndpoint implements AanvraagstationPort {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${ws.server.rvig-aanvraagstation.version}")
    private String wsVersion;

    @Value("${ws.server.rvig-aanvraagstation.pkio-oin}")
    private String PkioOin;

    @Autowired
    private AppClient appClient;

    @Override
    public ValidateAppActivationResponse validateAppActivation(ValidateAppActivationRequest request)
            throws TechnicalFault, VersionMismatchFault, OinMismatchFault {
        try {
            if (!wsVersion.equals(request.getMsgVersion())) {
                final VersionMismatchFaultDetail detail = new VersionMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.VERSION_MISMATCH);
                logger.error("Soap version given {} not match required version {}", request.getMsgVersion(), wsVersion);
                throw new VersionMismatchFault(FaultstringType.VERSION_MISMATCH.value(), detail);
            } else if (!PkioOin.equals(request.getRequester())) {
                final OinMismatchFaultDetail detail = new OinMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.OIN_MISMATCH);
                logger.debug("Soap requester oin given {} not match required oin {}", request.getRequester(), PkioOin);
                throw new OinMismatchFault(FaultstringType.OIN_MISMATCH.value(), detail);
            }

            ObjectNode response_digid = appClient.validateAppActivation(request.getRequester(),request.getBSN(),request.getDocumentNr(), request.getStationId(), request.getTransactionId());

            ResponseMessageType responseMessage;
            ErrorCodeType responseErrorCode = null;
            if ( "OK".equals(response_digid.get("status").asText()) ) {
                responseMessage = ResponseMessageType.OK;
                logger.debug("Connection with DigiD Kern was successful and the response status was equal to OK");
            } else {
                logger.debug("Connection with DigiD Kern was successful, but the response status was not equal to OK");
                responseMessage = ResponseMessageType.NOK;
                responseErrorCode = determineErrorType(response_digid.get("error_code").asText());
            }

            return generateValidateAppActivationResponse(responseMessage, responseErrorCode);
        } catch (VersionMismatchFault | OinMismatchFault fault) {
            throw fault;
        } catch (Exception e) {
            final TechnicalFaultDetail detail = new TechnicalFaultDetail();
            detail.setFaultstring(FaultstringType.TECHNICAL_FAULT);
            throw new TechnicalFault(e.getMessage(), detail, e);
        }
    }

    @Override
    public AppActivationResponse appActivation(AppActivationRequest request)
        throws TechnicalFault, VersionMismatchFault, OinMismatchFault {
        try {
            if (!wsVersion.equals(request.getMsgVersion())) {
                final VersionMismatchFaultDetail detail = new VersionMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.VERSION_MISMATCH);
                logger.error("Soap version given {} not match required version {}", request.getMsgVersion(), wsVersion);
                throw new VersionMismatchFault(FaultstringType.VERSION_MISMATCH.value(), detail);
            } else if (!PkioOin.equals(request.getRequester())) {
                final OinMismatchFaultDetail detail = new OinMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.OIN_MISMATCH);
                logger.debug("Soap requester oin given {} not match required oin {}", request.getRequester(), PkioOin);
                throw new OinMismatchFault(FaultstringType.OIN_MISMATCH.value(), detail);
            }

            ObjectNode response_digid = appClient.appActivation(request.getRequester(),request.getTransactionId(),request.getAppActivationCode());

            ResponseMessageType responseMessage;
            ErrorCodeType responseErrorCode = null;
            if ( "OK".equals(response_digid.get("status").asText()) ) {
                logger.debug("Connection with DigiD Kern was successful and the response status was equal to OK");
                responseMessage = ResponseMessageType.OK;
            } else {
                logger.debug("Connection with DigiD Kern was successful, but the response status was not equal to OK");
                responseMessage = ResponseMessageType.NOK;
                responseErrorCode = determineErrorType(response_digid.get("error_code").asText());
            }

            return generateAppActivationResponse(responseMessage, responseErrorCode);
        } catch (VersionMismatchFault | OinMismatchFault fault) {
            throw fault;
        } catch (Exception e) {
            final TechnicalFaultDetail detail = new TechnicalFaultDetail();
            detail.setFaultstring(FaultstringType.TECHNICAL_FAULT);
            throw new TechnicalFault(e.getMessage(), detail, e);
        }
    }

    private ValidateAppActivationResponse generateValidateAppActivationResponse(ResponseMessageType message, ErrorCodeType errorcode) {
        final ValidateAppActivationResponse response = new ValidateAppActivationResponse();
        response.setMsgVersion(wsVersion);
        response.setDateTime(toXmlTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
        response.setResponseMessage(message);
        response.setErrorCode(errorcode);
        return response;
    }

    private AppActivationResponse generateAppActivationResponse(ResponseMessageType message, ErrorCodeType errorcode) {
        final AppActivationResponse response = new AppActivationResponse();
        response.setMsgVersion(wsVersion);
        response.setDateTime(toXmlTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
        response.setResponseMessage(message);
        response.setErrorCode(errorcode);
        return response;
    }

    private ErrorCodeType determineErrorType (String errorCode) {
        // Possible error codes from digid-x we only receive them and pass them trough
        return switch (errorCode) {
            case "E_FEATURE_NOT_AVAILABLE" -> ErrorCodeType.E_FEATURE_NOT_AVAILABLE;
            case "E_BSN_NOT_VALID" -> ErrorCodeType.E_BSN_NOT_VALID;
            case "E_DOCUMENT_NOT_VALID" -> ErrorCodeType.E_DOCUMENT_NOT_VALID;
            case "E_DIGID_ACCOUNT_NOT_VALID" -> ErrorCodeType.E_DIGID_ACCOUNT_NOT_VALID;
            case "E_APP_ACTIVATION_CODE_NOT_FOUND" -> ErrorCodeType.E_APP_ACTIVATION_CODE_NOT_FOUND;
            case "E_GENERAL" -> ErrorCodeType.E_GENERAL;
            default -> null;
        };
    }

    private XMLGregorianCalendar toXmlTime(String isoDateOrTime) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(isoDateOrTime);
        } catch (DatatypeConfigurationException e) {
            logger.error("SOAP endpoint date conversion error", e);
        }
        return null;
    }
}
