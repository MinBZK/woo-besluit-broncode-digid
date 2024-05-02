
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

package nl.logius.digid.dc.domain.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum LevelOfAssurance {
    BASIS(10),
    MIDDEN(20),
    SUBSTANTIEEL(25),
    DEPRECATED_HOOG(30);

    private Integer label;
    private static final Map<Integer, LevelOfAssurance> LABELS = new HashMap<>();
    static { for (LevelOfAssurance e: values()) LABELS.put(e.label, e); }

    LevelOfAssurance(int label) {
        this.label = label;
    }

    public static LevelOfAssurance valueOfLabel(Integer label) {
        return LABELS.get(label);
    }

    public Integer getLabel() {
        return label;
    }

    public static Set<Integer> getLabels() {
        return LABELS.keySet();
    }

}
