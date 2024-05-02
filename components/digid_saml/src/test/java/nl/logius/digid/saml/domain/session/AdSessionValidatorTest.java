
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

package nl.logius.digid.saml.domain.session;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdSessionValidatorTest {

    @SuppressWarnings("squid:UnusedPrivateMethod")
    private static Stream<Arguments> getInvalidAdSessions() {
        return Stream.of(
                Arguments.of(1, AdAuthenticationStatus.STATUS_SUCCESS.label, "123456789", ImmutableMap.of("authenticationLevel","Invalid")),
                Arguments.of(10, "Status", "987654321", ImmutableMap.of("authenticationStatus","Invalid")),
                Arguments.of(30, AdAuthenticationStatus.STATUS_CANCELED.label, "12345678E",  ImmutableMap.of("bsn","Invalid length and/or contains non-numeric characters")),
                Arguments.of(5, "Unknown", "1234567890", ImmutableMap.of("authenticationLevel","Invalid", "authenticationStatus","Invalid", "bsn","Invalid length and/or contains non-numeric characters"))
        );
    }

    @Test
    public void successfulValidationTest() {
        AdSession adSession = new AdSession();
        adSession.setAuthenticationLevel(30);
        adSession.setAuthenticationStatus(AdAuthenticationStatus.STATUS_SUCCESS.label);
        adSession.setBsn("PPPPPPPPP");

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(adSession, "adSession");
        ValidationUtils.invokeValidator(new AdSessionValidator(), adSession, result);

        assertTrue(result.getAllErrors().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getInvalidAdSessions")
    public void unsuccessfulValidationTest(int authenticationLevel, String authenticationStatus, String bsn, Map<String, String> errors) {
        AdSession adSession = new AdSession();
        adSession.setAuthenticationLevel(authenticationLevel);
        adSession.setAuthenticationStatus(authenticationStatus);
        adSession.setBsn(bsn);

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(adSession, "adSession");
        ValidationUtils.invokeValidator(new AdSessionValidator(), adSession, result);

        errors.forEach((k, v) -> assertEquals(v, result.getFieldError(k).getCode()));
    }
}
