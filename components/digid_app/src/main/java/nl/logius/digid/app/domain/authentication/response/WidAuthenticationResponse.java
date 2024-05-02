
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

package nl.logius.digid.app.domain.authentication.response;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.logius.digid.app.shared.response.OkResponse;

public class WidAuthenticationResponse extends OkResponse {
    @Schema(description = "Webservice that requires authentication", example = "Mijn DigiD")
    private String webservice;
    @Schema(description = "Action that requires authentication", example = "change_email")
    private String action;
    @Schema(description = "Return url that app uses to return after succesful authentication", example = "https://digid.nl/inloggen_app_done")
    private String returnUrl;
    @Schema(description = "Url to continue the eid session with", example = "SSSSSSSSSSSSSSSSSSSS")
    private String url;
    @Schema(description = "Random sessionId provided by digid_eid", example = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP")
    private String sessionId;

    public String getWebservice() {
        return webservice;
    }
    public WidAuthenticationResponse() {
        super();
    }

    public void setWebservice(String webservice) {
        this.webservice = webservice;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
