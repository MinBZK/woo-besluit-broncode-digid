
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

package nl.logius.digid.eid.config;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import nl.logius.digid.eid.service.BsnkService;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;

import java.security.GeneralSecurityException;

@Configuration
public class BsnkServiceConfig {
    private static final ECPoint IDENTITY_POINT = BrainpoolP320r1.CURVE.decodePoint(Base64.decode(
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
    private static final ECPoint PSEUDONYM_POINT = BrainpoolP320r1.CURVE.decodePoint(Base64.decode(
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));

    @Bean
    public BsnkService bsnkService() throws GeneralSecurityException {
        return new Service();
    }

    private static class Service extends BsnkService {
        public Service() throws GeneralSecurityException {
        }

        @Override
        public void init() {
            ReflectionTestUtils.setField(this, "identityPublicPoint", IDENTITY_POINT);
            ReflectionTestUtils.setField(this, "pseudonymPublicPoint", PSEUDONYM_POINT);
        }
    }
}
