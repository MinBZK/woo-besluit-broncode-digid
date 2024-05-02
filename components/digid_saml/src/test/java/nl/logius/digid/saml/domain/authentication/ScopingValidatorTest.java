
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

import nl.logius.digid.saml.extensions.InitializationExtension;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.core.impl.RequesterIDBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, InitializationExtension.class})
public class ScopingValidatorTest {

    private Scoping scoping;
    private RequesterID requesterID;
    private RequesterID requesterID2;

    @BeforeEach
    public void setup() {
        scoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);

        requesterID = new RequesterIDBuilder().buildObject();
        requesterID.setRequesterID("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000");
        requesterID2 = new RequesterIDBuilder().buildObject();
        requesterID2.setRequesterID("urn:nl-eid-gdi:1.0:DV:00000009999999999001:entities:0000");
    }

    @Test
    public void multipleRequesterIdsTest() {
        scoping.getRequesterIDs().add(requesterID);
        scoping.getRequesterIDs().add(requesterID2);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(scoping, "scoping");
        ValidationUtils.invokeValidator(new ScopingValidator(), scoping, result);

        assertEquals("Must not contain more than one requesterId.", result.getFieldError("requesterIDs").getCode());
    }

    @Test
    public void oneRequesterIdTest() {
        scoping.getRequesterIDs().clear();
        scoping.getRequesterIDs().add(requesterID);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(scoping, "scoping");
        ValidationUtils.invokeValidator(new ScopingValidator(), scoping, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @Test
    public void noRequesterIdIsPresentTest() {
        Scoping emptyScoping = OpenSAMLUtils.buildSAMLObject(Scoping.class);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(emptyScoping, "scoping");
        ValidationUtils.invokeValidator(new ScopingValidator(), scoping, result);

        assertEquals(0, result.getErrorCount());
    }
}
