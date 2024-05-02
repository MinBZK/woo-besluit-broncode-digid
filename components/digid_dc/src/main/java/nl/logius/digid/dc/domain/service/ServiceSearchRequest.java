
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

import java.util.Optional;

public class ServiceSearchRequest {
    private Long id;
    private Long connectionId;
    private String connectionEntityId;
    private String name;
    private String serviceUuid;
    private String entityId;
    private Boolean active;
    private Boolean digid;
    private Boolean machtigen;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionEntityId() {
        return connectionEntityId;
    }

    public void setConnectionEntityId(String connectionEntityId) {
        this.connectionEntityId = connectionEntityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(String s) {
        this.active = getBooleanValue(s).orElse(null);
    }

    public Boolean getDigid() {
        return digid;
    }

    public void setDigid(String s) {
        this.digid = getBooleanValue(s).orElse(null);
    }

    public Boolean getMachtigen() {
        return machtigen;
    }

    public void setMachtigen(String s) {
        this.machtigen = getBooleanValue(s).orElse(null);
    }

    private Optional<Boolean> getBooleanValue(String s) {
        if (s == null) { return Optional.empty(); }

        switch (s) {
            case "true":
                return Optional.of(Boolean.TRUE);
            case "false":
                return Optional.of(Boolean.FALSE);
            default:
                return Optional.empty();
        }
    }
}
