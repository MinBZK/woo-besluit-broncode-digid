
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

package nl.logius.digid.hsm.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class BaseActivateRequest {
    private String identifier;
    private char identifierType = 'B';
    private int schemeKeyVersion = 1;
    private String identityProvider = "SSSSSSSSSSSSSSSSSSSS";
    private int identityProviderKeySetVersion = 1;
    private int signKeyVersion = 1;

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public char getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(char identifierType) {
        this.identifierType = identifierType;
    }

    @Min(1)
    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public void setSchemeKeyVersion(int schemeKeyVersion) {
        this.schemeKeyVersion = schemeKeyVersion;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Min(1)
    public int getIdentityProviderKeySetVersion() {
        return identityProviderKeySetVersion;
    }

    public void setIdentityProviderKeySetVersion(int identityProviderKeySetVersion) {
        this.identityProviderKeySetVersion = identityProviderKeySetVersion;
    }

    @Min(1)
    public int getSignKeyVersion() {
        return signKeyVersion;
    }

    public void setSignKeyVersion(int signKeyVersion) {
        this.signKeyVersion = signKeyVersion;
    }
}
