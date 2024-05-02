
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

package nl.logius.digid.dc;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "statuses")
public class Status extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean active;
    private ZonedDateTime activeFrom;
    private ZonedDateTime activeUntil;

    public Status() {
    }

    public Status(Boolean active) {
        this.active = active;
    }

    public Status(Boolean active, ZonedDateTime activeFrom, ZonedDateTime activeUntil) {
        this.active = active;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
    }

    public Long getId() {
        return id;
    }
    public Boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public ZonedDateTime getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(ZonedDateTime activeFrom) {
        this.activeFrom = activeFrom;
    }

    public ZonedDateTime getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(ZonedDateTime activeUntil) {
        this.activeUntil = activeUntil;
    }

    @JsonIgnore
    public Boolean isAllowed() {
        return isActive() && ((getActiveFrom() == null || ZonedDateTime.now().isAfter(getActiveFrom())) && ((getActiveUntil() == null || ZonedDateTime.now().isBefore(getActiveUntil()))));
    }
}
