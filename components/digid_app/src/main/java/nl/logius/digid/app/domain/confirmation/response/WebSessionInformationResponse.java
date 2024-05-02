
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

package nl.logius.digid.app.domain.confirmation.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.logius.digid.app.shared.response.AppResponse;

public class WebSessionInformationResponse implements AppResponse {
    @Schema(description = "Webservice that requires authentication", example = "Mijn DigiD")
    private String webservice;
    @Schema(description = "Authentication level", example = "10")
    private String authenticationLevel;
    @Schema(description = "Action that requires authentication", example = "change_email")
    private String action;
    @Schema(description = "Return url that app uses to return after succesful authentication", example = "https://digid.nl/inloggen_app_done")
    private String returnUrl;
    @Schema(description = "New authentication level", example = "20")
    private String newAuthenticationLevel;
    @Schema(description = "New level start date", example = "2022-02-25T15:13:45+01:00")
    private String newLevelStartDate;
    @Schema(description = "Hashed pip value", example = "hashedpip")
    private String hashedPip;

    @Schema(description = "Icon uri that is used for app2app oidc", example = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS")
    private String iconUri;

    @Schema(description = "Indication if it is an oidc session")
    private boolean oidcSession;

    public String getWebservice() {
        return webservice;
    }

    public void setWebservice(String webservice) {
        this.webservice = webservice;
    }

    public String getAuthenticationLevel() {
        return authenticationLevel;
    }

    public void setAuthenticationLevel(String authenticationLevel) {
        this.authenticationLevel = authenticationLevel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getNewAuthenticationLevel() {
        return newAuthenticationLevel;
    }

    public void setNewAuthenticationLevel(String newAuthenticationLevel) {
        this.newAuthenticationLevel = newAuthenticationLevel;
    }

    public String getNewLevelStartDate() {
        return newLevelStartDate;
    }

    public void setNewLevelStartDate(String newLevelStartDate) {
        this.newLevelStartDate = newLevelStartDate;
    }

    public void setHashedPip(String hashedPip) {
        this.hashedPip = hashedPip;
    }

    public String getHashedPip() {
        return hashedPip;
    }

    public boolean isOidcSession() {
        return oidcSession;
    }

    public void setOidcSession(boolean oidcSession) {
        this.oidcSession = oidcSession;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.iconUri = iconUri;
    }
}
