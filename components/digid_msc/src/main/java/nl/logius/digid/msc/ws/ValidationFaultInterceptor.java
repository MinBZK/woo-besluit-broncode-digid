
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

import javax.xml.bind.UnmarshalException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.exc.WstxUnexpectedCharException;

import https.digid_nl.schema.mu_status_controller.FaultstringType;
import https.digid_nl.schema.mu_status_controller.ValidationErrorFault;
import https.digid_nl.schema.mu_status_controller.ValidationErrorFaultDetail;
import nl.logius.digid.msc.util.XmlUtils;

// TODO: refactor this class
public class ValidationFaultInterceptor extends AbstractSoapInterceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ValidationFaultInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {
        Fault fault = (Fault) soapMessage.getContent(Exception.class);
        final String faultMessage = fault.getMessage();

        if (isValidationError(fault.getCause(), faultMessage)) {
            appendValidationFault(soapMessage, fault);
        }
    }

    private boolean isValidationError(Throwable faultCause, String faultMessage) {
        return (isIndicatingNotSchemeCompliantXml(faultCause, faultMessage) || isIndicatingSyntacticallyIncorrect(faultCause) || isIndicatingAddressingIncorrect(faultCause, faultMessage));
    }

    // non-schema compliant errors
    private boolean isIndicatingNotSchemeCompliantXml(Throwable faultCause, String faultMessage) {
        // 1.) If the root-Element of the SoapBody is syntactically correct, but not scheme-compliant,
        //              there is no UnmarshalException and we have to look for
        // 2.) Missing / lead to Faults without Causes, but to Messages like "Unexpected wrapper element XYZ found. Expected"
        //              One could argue, that this is syntactically incorrect, but here we just take it as Non-Scheme-compliant
        if(faultCause instanceof UnmarshalException || isNotNull(faultMessage) && faultMessage.contains("Unexpected wrapper element")) {
                return true;
        }
        return false;
    }

    // syntax errors
    private boolean isIndicatingSyntacticallyIncorrect(Throwable faultCause) {
        // If Xml-Header is invalid, there is a wrapped Cause in the original Cause we have to check
        if(faultCause instanceof WstxException || isNotNull(faultCause) && faultCause.getCause() instanceof WstxUnexpectedCharException
                || faultCause instanceof IllegalArgumentException) {
                return true;
        }
        return false;
    }

    // addressing errors
    private boolean isIndicatingAddressingIncorrect(Throwable faultCause, String faultMessage) {
        if(isNotNull(faultMessage) && faultMessage.contains("Message Addressing Property is not present")) {
                return true;
        }
        return false;
    }

//    MessageAddressingHeaderRequired

    private boolean isNotNull(Object object) {
            return object != null;
    }

    private void appendValidationFault(SoapMessage soapMessage, Fault fault) {
        try {
            ValidationErrorFaultDetail detail = new ValidationErrorFaultDetail();
            detail.setFaultstring(FaultstringType.VALIDATION_ERROR);
            // final ValidationErrorFault validationFault = new ValidationErrorFault(FaultstringType.VALIDATION_ERROR.value(), detail, fault.getCause());
            final Element element = XmlUtils.appendAsChildElement2NewElement(XmlUtils.marhallJaxbElementIntoDocument(detail));
            fault.setMessage(FaultstringType.VALIDATION_ERROR.value());
            fault.setDetail(element);
            soapMessage.setContent(ValidationErrorFault.class, fault);
        } catch (Exception e) {
            logger.error("Error in appending validation fault: {}", e.getMessage());
        }
    }
}
