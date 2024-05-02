
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

package nl.logius.digid.saml.domain.authentication;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BindingContextValidatorTest {
    private SAMLBindingContext bindingContext;

    @BeforeEach
    public void setup() {
        bindingContext = new MessageContext().getSubcontext(SAMLBindingContext.class, true);
    }

    @Test
    public void successfullValidation() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(bindingContext, "bindingContext");
        ValidationUtils.invokeValidator(new BindingContextValidator(), bindingContext, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void relayStateTest() {
        bindingContext.setRelayState(StringUtils.repeat("a", 80));
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(bindingContext, "bindingContext");
        ValidationUtils.invokeValidator(new BindingContextValidator(), bindingContext, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void relayStateTooLongTest() {
        bindingContext.setRelayState(StringUtils.repeat("a", 81));
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(bindingContext, "bindingContext");
        ValidationUtils.invokeValidator(new BindingContextValidator(), bindingContext, result);

        assertEquals("Exceeded maximum length", result.getFieldError("relayState").getCode());
    }
}
