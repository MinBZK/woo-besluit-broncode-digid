
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

package nl.logius.digid.hsm.config;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.hsm.crypto.Utils;
import nl.logius.digid.hsm.exception.ConfigException;
import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import nl.logius.digid.hsm.provider.KeysProvider;
import nl.logius.digid.hsm.provider.keys.StaticProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name="keys.provider", havingValue="static")
public class StaticKeysConfig extends KeyedConfig {
    private final Logger logger = LoggerFactory.getLogger(StaticKeysConfig.class);

    @Override
    protected String getPrefix() {
        return "keys.static";
    }

    @Bean
    public KeysProvider provider() {
        final Map<String, Map<String,byte[]>> keys = new HashMap<>();
        for (final String loadKey : getLoadKeys(true)) {
            addServiceKey(keys, loadKey);
        }
        return new StaticProvider(keys);
    }

    private void addServiceKey(Map<String,Map<String,byte[]>> keys, String loadKey) {
        final ServiceProviderKeysInput input = new ServiceProviderKeysInput();
        input.setSchemeVersion(getProperty(loadKey, "schemeVersion", Integer.class, true));
        input.setSchemeKeyVersion(getProperty(loadKey, "schemeKeyVersion", Integer.class, true));
        input.setClosingKeyVersion(getProperty(loadKey, "closingKeyVersion", Integer.class, true));
        input.setCertificate(Utils.decodeCertificate(getProperty(loadKey, "certificate", String.class)));

        final byte[] id = prepareKey(loadKey, "ID", false, true, input.getCertificate());
        final byte[] pd = prepareKey(loadKey, "PD", false, true, input.getCertificate());
        final byte[] pc = prepareKey(loadKey, "PC", false, true, input.getCertificate());
        final byte[] drki = prepareKey(loadKey, "DRKi", false, true, input.getCertificate());

        final String cacheKey = input.cacheKey();
        if (id != null && pd != null && pc != null && drki != null) {
            logger.info("Adding identity and pseudonym keys + drki for <{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion());
            keys.put(cacheKey, ImmutableMap.of("ID", id, "PD", pd, "PC", pc, "DRKi", drki));
        } else if (id != null && pd != null && pc != null) {
            logger.info("Adding identity and pseudonym keys for <{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion());
            keys.put(cacheKey, ImmutableMap.of("ID", id, "PD", pd, "PC", pc));
        } else if (pd != null && pc != null && drki != null) {
            logger.info("Adding pseudonym keys + drki for <{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion());
            keys.put(cacheKey, ImmutableMap.of("PD", pd, "PC", pc, "DRKi", drki));
        } else if (pd != null && pc != null) {
            logger.info("Adding pseudonym keys for <{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion());
            keys.put(cacheKey, ImmutableMap.of("PD", pd, "PC", pc));
        } else if (id != null) {
            logger.info("Adding identity key for <{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion());
            keys.put(cacheKey, ImmutableMap.of("ID", id));
        } else {
            throw new ConfigException(String.format("No keys found for %s", loadKey));
        }
    }
}
