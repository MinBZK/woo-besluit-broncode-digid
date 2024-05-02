
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

package nl.logius.digid.dc.domain.connection;

import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.domain.organization.OrganizationRole;

public class ConnectionDTO {

    private String name;
    private String websiteUrl;
    private Status status;
    private String entityId;
    private ProtocolType protocolType;
    private String samlMetadata;
    private String metadataUrl;
    private OrganizationRole organizationRole;
    private String ssoDomain;
    private boolean ssoStatus;

    public String getName() {
        return name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public Status getStatus() {
        return status;
    }

    public String getEntityId() {
        return entityId;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public String getSamlMetadata() {
        return samlMetadata;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public OrganizationRole getOrganizationRole() {
        return organizationRole;
    }

    public String getSsoDomain() {
        return ssoDomain;
    }

    public boolean isSsoStatus() {
        return ssoStatus;
    }
}
