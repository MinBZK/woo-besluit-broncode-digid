
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

import nl.logius.digid.dws.util.XmlUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class AppActivationValidationFaultInterceptor extends AbstractValidationFaultInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AppActivationValidationFaultInterceptor() {
        super();
    }

    protected void appendValidationFault(SoapMessage soapMessage, Fault fault) {
        try {

            https.digid_nl.schema.aanvraagstation.ValidationErrorFaultDetail detail = new https.digid_nl.schema.aanvraagstation.ValidationErrorFaultDetail();
            detail.setFaultstring(https.digid_nl.schema.aanvraagstation.FaultstringType.VALIDATION_ERROR);
            final Element element = XmlUtils.appendAsChildElement2NewElement(XmlUtils.marhallJaxbElementIntoDocument(detail));
            fault.setMessage(https.digid_nl.schema.aanvraagstation.FaultstringType.VALIDATION_ERROR.value());
            fault.setDetail(element);
            soapMessage.setContent(https.digid_nl.schema.aanvraagstation.ValidationErrorFault.class, fault);
        } catch (Exception e) {
            logger.error("Error in appending validation fault: {}", e.getMessage());
        }
    }
}
