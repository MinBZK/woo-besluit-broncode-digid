
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

package nl.logius.digid.dws.config;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import nl.logius.digid.dws.util.BsnkUtils;
import nl.logius.digid.pp.parser.PublicKeyParser;
import nl.logius.digid.sharedlib.config.AbstractErrorConfig;

@Configuration
public class BsnkConfig extends AbstractErrorConfig {
    @Value("${ws.client.bsnk_activate.digid_mu_oin}")
    private String digidMuOin;

    @Value("${ws.client.bsnk_activate.digid_mu_ksv}")
    private BigInteger digidMuKsv;

    @Value("${ws.client.bsnk_activate.bsnk_u_pub_key}")
    private String bsnkUPubkeyBase64;

    @Value("${ws.client.bsnk_activate.bsnk_u_ksv}")
    private BigInteger bsnkUKsv;

    @Bean
    public BsnkUtils bsnkUtils() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        ECPublicKey bsnkUPubkey = (ECPublicKey) PublicKeyParser
                .decodeKey(Base64.getDecoder().decode(bsnkUPubkeyBase64));
        return new BsnkUtils(digidMuOin, digidMuKsv, bsnkUPubkey, bsnkUKsv);
    }
}
