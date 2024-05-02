
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

package nl.logius.digid.ns.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "switches")
public class Switch extends SQLObject{
    public static final long MNS_SWITCH_ID = 1L;
    public static final long FCM_SWITCH_ID = 2L;
    public static final long APNS_SWITCH_ID = 3L;

    private String name;

    private String description;

    private SwitchStatus status;
    private int pilotGroupId;

    public Switch() {}

    public Switch(String name, String description, SwitchStatus status){
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public ZonedDateTime timestampToZonedDateTime(String text) {
        return ZonedDateTime.of(LocalDateTime.parse(text.substring(0, 19)), ZoneOffset.UTC);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SwitchStatus getStatus() { return status; }

    public void setStatus(SwitchStatus status) {
        this.status = status;
    }

    public int getPilotGroupId() {
        return pilotGroupId;
    }

    public void setPilotGroupId(int pilotGroupId) {
        this.pilotGroupId = pilotGroupId;
    }
}
