
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

package nl.logius.digid.app.domain.attempts;

import nl.logius.digid.app.shared.SQLObject;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "attempts")
public class Attempt extends SQLObject {

    private String attemptableType;
    private Long attemptableId;
    private String attemptType;

    public Attempt() {}

    public Attempt(String attemptableType, Long attemptableId, String attemptType) {
        this.attemptableType = attemptableType;
        this.attemptableId = attemptableId;
        this.attemptType = attemptType;
    }

    public String getAttemptableType() {
        return attemptableType;
    }

    public Long getAttemptableId() {
        return attemptableId;
    }

    public void setAttemptableId(Long attemptableId) {
        this.attemptableId = attemptableId;
    }

    public void setAttemptableType(String attemptableType) {
        this.attemptableType = attemptableType;
    }

    public String getAttemptType() {
        return attemptType;
    }

    public void setAttemptType(String attemptType) {
        this.attemptType = attemptType;
    }
}
