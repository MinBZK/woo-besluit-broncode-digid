
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

package nl.logius.digid.dc.domain.metadata;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

public class SamlMetadataRequest {

    @NotNull
    private String connectionEntityId;
    @NotNull
    private String serviceEntityId;

    private String serviceUuid;
    @Min(0)
    private int serviceIdx;

    public String getConnectionEntityId() {
        return connectionEntityId;
    }

    public void setConnectionEntityId(String connectionEntityId) {
        this.connectionEntityId = connectionEntityId;
    }

    public String getServiceEntityId() {
        return serviceEntityId;
    }

    public void setServiceEntityId(String serviceEntityId) {
        this.serviceEntityId = serviceEntityId;
    }

    public String getServiceUuid() {
        return serviceUuid;
    }

    public void setServiceUuid(String serviceUuid) {
        this.serviceUuid = serviceUuid;
    }

    public int getServiceIdx() {
        return serviceIdx;
    }

    public void setServiceIdx(int serviceIdx) {
        this.serviceIdx = serviceIdx;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(connectionEntityId, serviceEntityId, serviceUuid, serviceIdx);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SamlMetadataRequest)) return false;
        final SamlMetadataRequest that = (SamlMetadataRequest) o;
        return Objects.equals(connectionEntityId, that.connectionEntityId) &&
            Objects.equals(serviceEntityId, that.serviceEntityId) &&
            Objects.equals(serviceUuid, that.serviceUuid) &&
            Objects.equals(serviceIdx, that.serviceIdx);
    }

    public String cacheableKey() {
        return String.format("%s-%s-%s", getConnectionEntityId(), getServiceUuid(), hashCode());
    }
}
