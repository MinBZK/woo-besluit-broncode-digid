
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

package nl.logius.digid.oidc.model;

import java.util.HashMap;
import java.util.Map;

public final class LevelOfAssurance {
    public static final String DEPRECATED_BASIS = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    public static final String DEPRECATED_MIDDEN = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract";
    public static final String DEPRECATED_SUBSTANTIEEL = "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard";
    public static final String DEPRECATED_HOOG = "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI";

    public static final String BASIS = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    public static final String MIDDEN = "http://eidas.europa.eu/LoA/low";
    public static final String SUBSTANTIEEL = "http://eidas.europa.eu/LoA/substantial";
    public static final String HOOG = "http://eidas.europa.eu/LoA/high";

    private static final Map<Integer, String> referenceMap = initializeLoaNumberMap();

    private LevelOfAssurance() {
        throw new IllegalStateException("Utility class");
    }

    static Map<Integer, String> initializeLoaNumberMap() {
        Map<Integer, String> map = new HashMap();

        map.put(10, BASIS);
        map.put(20, MIDDEN);
        map.put(25, SUBSTANTIEEL);
        map.put(30, HOOG);

        return map;
    }

    public static String map(Integer key) {
        String result = referenceMap.get(key);
        return result != null ? result : null;
    }

}
