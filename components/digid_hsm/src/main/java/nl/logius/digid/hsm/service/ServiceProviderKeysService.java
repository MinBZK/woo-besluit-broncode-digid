
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

package nl.logius.digid.hsm.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import nl.logius.digid.hsm.model.ServiceProviderKeysRequest;
import nl.logius.digid.hsm.provider.KeysProvider;

@Service
public class ServiceProviderKeysService {
    @Autowired
    KeysProvider provider;

    public Map<String,byte[]> serviceProviderKeys(ServiceProviderKeysRequest request) {
        final ServiceProviderKeysInput input = request.toInput();
        final Map<String,byte[]> result = new HashMap<>(3);
        if (request.isIdentity()) {
            result.putAll(provider.serviceProviderKeys(input, false));
        }
        if (request.isPseudonym()) {
            result.putAll(provider.serviceProviderKeys(input, true));
        }
        return result;
    }
}
