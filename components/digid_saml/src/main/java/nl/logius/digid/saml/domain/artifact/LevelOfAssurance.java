
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

package nl.logius.digid.saml.domain.artifact;

import nl.logius.digid.saml.exception.SamlSessionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LevelOfAssurance {
    public static final String DEPRECATED_BASIS = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    public static final String DEPRECATED_MIDDEN = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract";
    public static final String DEPRECATED_SUBSTANTIEEL = "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard";
    public static final String DEPRECATED_HOOG = "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI";

    public static final String BASIS = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    public static final String MIDDEN = "http://eidas.europa.eu/LoA/low";
    public static final String SUBSTANTIEEL = "http://eidas.europa.eu/LoA/substantial";
    public static final String HOOG = "http://eidas.europa.eu/LoA/high";

    private static final Map<String, String> referenceMap = initializeLoaMap();
    private static final Map<String, Integer> numberMap = initializeLoaNumberMap();

    private LevelOfAssurance() {
        throw new IllegalStateException("Utility class");
    }

    static Map<String, String> initializeLoaMap() {
        Map<String, String> map = new HashMap<>();
        map.put(DEPRECATED_BASIS, BASIS);
        map.put(DEPRECATED_MIDDEN, MIDDEN);
        map.put(DEPRECATED_SUBSTANTIEEL, SUBSTANTIEEL);
        map.put(DEPRECATED_HOOG, HOOG);
        map.put("10", BASIS);
        map.put("20", MIDDEN);
        map.put("25", SUBSTANTIEEL);
        map.put("30", HOOG);

        return map;
    }

    static Map<String, Integer> initializeLoaNumberMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put(DEPRECATED_BASIS, 10);
        map.put(BASIS, 10);
        map.put(DEPRECATED_MIDDEN, 20);
        map.put(MIDDEN, 20);
        map.put(DEPRECATED_SUBSTANTIEEL, 25);
        map.put(SUBSTANTIEEL, 25);
        map.put(DEPRECATED_HOOG, 30);
        map.put(HOOG, 30);

        return map;
    }

    public static String map(String key) {
        String result = referenceMap.get(key);
        return result != null ? result : key;
    }

    public static int getAssuranceLevel(String key) throws SamlSessionException {
        if (!numberMap.containsKey(key)) {
            throw new SamlSessionException("Assurance level not found");
        }
        return numberMap.get(key);
    }

    public static boolean validateAssuranceLevel(int assuranceLevel) {
        return numberMap.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), assuranceLevel))
                .findFirst()
                .map(Map.Entry::getKey)
                .isPresent();
    }
}
