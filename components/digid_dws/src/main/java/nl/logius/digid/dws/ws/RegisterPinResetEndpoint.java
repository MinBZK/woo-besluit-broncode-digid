
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import nl.logius.digid.dws.service.NotificationService;
import org.apache.cxf.annotations.SchemaValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import https.digid_nl.schema.mu_pin_reset.FaultstringType;
import https.digid_nl.schema.mu_pin_reset.NotFoundFault;
import https.digid_nl.schema.mu_pin_reset.NotFoundFaultDetail;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetPort;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetRequest;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetResponse;
import https.digid_nl.schema.mu_pin_reset.ResponseMessageType;
import https.digid_nl.schema.mu_pin_reset.TechnicalFault;
import https.digid_nl.schema.mu_pin_reset.TechnicalFaultDetail;
import https.digid_nl.schema.mu_pin_reset.VersionMismatchFault;
import https.digid_nl.schema.mu_pin_reset.VersionMismatchFaultDetail;
import nl.logius.digid.dws.exception.DocumentNotFoundException;

@SchemaValidation(schemas = "${ws.server.pin_reset.xsd-file-name}")
public class RegisterPinResetEndpoint implements RegisterPinResetPort {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private NotificationService notificationService;

    @Value("${ws.server.pin_reset.version}")
    private String wsVersion;

    @Override
    public RegisterPinResetResponse registerPinReset(RegisterPinResetRequest request)
            throws NotFoundFault, TechnicalFault, VersionMismatchFault {
        try {
            if (!wsVersion.equals(request.getMsgVersion())) {
                final VersionMismatchFaultDetail detail = new VersionMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.VERSION_MISMATCH);
                logger.error("Soap version given {} not match required version {}", request.getMsgVersion(), wsVersion);
                throw new VersionMismatchFault(FaultstringType.VERSION_MISMATCH.value(), detail);
            }

            notificationService.updateStatus(request);

            return generateResponse(ResponseMessageType.VERWERKT);
        } catch (VersionMismatchFault fault) {
            throw fault;
        } catch (DocumentNotFoundException e) {
            final NotFoundFaultDetail detail = new NotFoundFaultDetail();
            detail.setFaultstring(FaultstringType.NOT_FOUND);
            throw new NotFoundFault(FaultstringType.NOT_FOUND.value(), detail, e);
        } catch (Exception e) {
            final TechnicalFaultDetail detail = new TechnicalFaultDetail();
            detail.setFaultstring(FaultstringType.TECHNICAL_FAULT);
            throw new TechnicalFault(e.getMessage(), detail, e);
        }
    }

    private RegisterPinResetResponse generateResponse(ResponseMessageType message) {
        final RegisterPinResetResponse response = new RegisterPinResetResponse();
        response.setMsgVersion(wsVersion);
        response.setDateTime(toXmlTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
        response.setResponseMessage(message);
        return response;
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
