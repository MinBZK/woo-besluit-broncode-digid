
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

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.hsm.exception.NotFoundError;
import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import nl.logius.digid.hsm.provider.KeysProvider;

public class StaticProvider implements KeysProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ImmutableMap<String, Map<String,byte[]>> keys;

    public StaticProvider(Map<String, Map<String,byte[]>> keys) {
        this.keys = ImmutableMap.copyOf(keys);
    }

    @Override
    public Map<String,byte[]> serviceProviderKeys(ServiceProviderKeysInput input, boolean pseudonym) {
        final Map<String, byte[]> result = keys.get(input.cacheKey());
        logger.info("ServiceProviderKeys-static <{},{},{},{}>", input.getServiceProvider(),
                input.getServiceProviderKeySetVersion(), input.getClosingKeyVersion(), pseudonym ? "P" : "I");
        if (result == null) {
            throw new NotFoundError("No key available for this request");
        }
        if (pseudonym) {
            final byte[] pd = result.get("PD");
            final byte[] pc = result.get("PC");
            final byte[] drki = result.get("DRKi");

            if (pd != null && pc != null && drki != null) {
                return ImmutableMap.of("PD", pd, "PC", pc, "DRKi", drki);
            }
            else if (pd != null && pc != null) {
                return ImmutableMap.of("PD", pd, "PC", pc);
            }
        }
         else {
            final byte[] id = result.get("ID");
            return ImmutableMap.of("ID", id);
        }
        throw new NotFoundError("No key available for this request and type");
    }
}
