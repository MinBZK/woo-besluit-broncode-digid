
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

package nl.logius.digid.hsm.provider.keys;

import nl.logius.digid.hsm.crypto.bsnk.ServiceProviderKeys;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import nl.logius.digid.hsm.provider.KeysProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnProperty(name="keys.provider", havingValue="hsm")
public class HsmProvider implements KeysProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionPool pool;

    @Override
    @Cacheable(cacheNames="service-provider-keys", key="#input.cacheKey().concat(#pseudonym ? 'P' : 'I')")
    public Map<String,byte[]> serviceProviderKeys(ServiceProviderKeysInput input, boolean pseudonym) {
        logger.info("ServiceProviderKeys-hsm <{},{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion(), pseudonym ? "P" : "I");
        try (final ServiceProviderKeys action = new ServiceProviderKeys(pool)) {
            return action.getKeys(input, pseudonym);
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the signature
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    @Override
    public boolean anyKey() {
        return true;
    }
}
