
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

package nl.logius.digid.msc.ws;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.annotations.SchemaValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import https.digid_nl.schema.mu_status_controller.FaultstringType;
import https.digid_nl.schema.mu_status_controller.NotFoundFault;
import https.digid_nl.schema.mu_status_controller.NotFoundFaultDetail;
import https.digid_nl.schema.mu_status_controller.RegisterSCStatusPort;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCRequest;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCResponse;
import https.digid_nl.schema.mu_status_controller.ResponseMessageType;
import https.digid_nl.schema.mu_status_controller.TechnicalFault;
import https.digid_nl.schema.mu_status_controller.TechnicalFaultDetail;
import https.digid_nl.schema.mu_status_controller.VersionMismatchFault;
import https.digid_nl.schema.mu_status_controller.VersionMismatchFaultDetail;
import nl.logius.digid.msc.exception.DocumentNotFoundException;
import nl.logius.digid.msc.service.DocumentStatusService;

@SchemaValidation(schemas = "${ws.xsd-file-name}")
public class RegisterStatusSCEndpoint implements RegisterSCStatusPort {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DocumentStatusService documentStatusService;

    @Value("${ws.version}")
    private String wsVersion;

    @Override
    public RegisterStatusSCResponse registerStatusSC(RegisterStatusSCRequest request)
            throws NotFoundFault, TechnicalFault, VersionMismatchFault {
        try {
            if (!ArrayUtils.contains(wsVersion.split(","), request.getMsgVersion())) {
                final VersionMismatchFaultDetail detail = new VersionMismatchFaultDetail();
                detail.setFaultstring(FaultstringType.VERSION_MISMATCH);
                logger.error("Soap version given {} not match required version {}", request.getMsgVersion(), wsVersion);
                throw new VersionMismatchFault(FaultstringType.VERSION_MISMATCH.value(), detail);
            }

            documentStatusService.updateStatus(request);

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

    private RegisterStatusSCResponse generateResponse(ResponseMessageType message) {
        final RegisterStatusSCResponse response = new RegisterStatusSCResponse();
        String[] versions = wsVersion.split(",");
        response.setMsgVersion(versions[versions.length-1]);
        response.setDateTime(toXmlTime(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)));
        response.setResponseMessage(ResponseMessageType.VERWERKT);
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
