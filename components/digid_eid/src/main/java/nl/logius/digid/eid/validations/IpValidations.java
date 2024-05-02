
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

package nl.logius.digid.eid.validations;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.EidSession;

@Component
public class IpValidations {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${source_ip_check}")
    private boolean sourceIpCheck;

    @Value("${source_ip_salt}")
    private String sourceIpSalt;

    public void ipCheck(EidSession session, String clientIp) {
        if (session.getClientIpAddress() != null && !session.getClientIpAddress().isEmpty()) {
            String[] clientIps = clientIp.split(", ");

            byte[] data = clientIps[0].concat(sourceIpSalt).getBytes(StandardCharsets.UTF_8);
            String anonimizedIp = Base64.toBase64String(DigestUtils.digest("SHA256").digest(data));
            if (!anonimizedIp.equals(session.getClientIpAddress())) {
                String logMessage = String.format(
                        "Security exception: Browser and Desktop client IP doesn't match: %s expected: %s",
                        anonimizedIp, session.getClientIpAddress());
                if (sourceIpCheck) {
                    throw new ClientException(logMessage);
                } else {
                    logger.warn(logMessage);
                }
            }
        }
    }
}
