
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

package nl.logius.digid.sharedlib.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BsnkClient extends JsonClient {
    public BsnkClient(HttpUrl baseUrl, int timeout) {
        super(baseUrl, timeout);
    }

    public String decryptDep(String directEncryptedPseudonym, List<String> serviceProviderKeysArray, String schemeVersion, String schemeKey, String targetClosingKey, String authorizedParty) {
        Map<String, String> schemeKeysMap = new HashMap<>();
        schemeKeysMap.put("urn:nl-gdi-eid:1.0:pp-key:<ENVIRONMENT>:1:U:" + schemeVersion, schemeKey);

        final Map<Object, Object> body = ImmutableMap.builder()
            .put("signedDirectEncryptedPseudonym", directEncryptedPseudonym)
            .put("serviceProviderKeys", serviceProviderKeysArray)
            .put("schemeKeys", schemeKeysMap)
            .put("targetClosingKey", targetClosingKey)
            .put("authorizedParty", authorizedParty)
            .build();

        JsonNode response = execute("pep-crypto/api/v1/signed-direct-encrypted-pseudonym", body);
        return response.path("decodedPseudonym").path("pseudonymValue").asText();
    }

    public String decryptEp(String encryptedPseudonym, List<String> serviceProviderKeysArray, String schemeKeyVersion, String schemeKey, String targetClosingKey) {
        Map<String, String> schemeKeysMap = new HashMap<>();
        schemeKeysMap.put("urn:nl-gdi-eid:1.0:pp-key:<ENVIRONMENT>:1:PP_P:" + schemeKeyVersion, schemeKey);

        final Map<Object, Object> body = ImmutableMap.builder()
            .put("signedEncryptedPseudonym", encryptedPseudonym)
            .put("serviceProviderKeys", serviceProviderKeysArray)
            .put("schemeKeys", schemeKeysMap)
            .put("targetClosingKey", targetClosingKey)
            .build();

        JsonNode response = execute("pep-crypto/api/v1/signed-encrypted-pseudonym", body);
        return response.path("decodedPseudonym").path("pseudonymValue").asText();
    }
}
