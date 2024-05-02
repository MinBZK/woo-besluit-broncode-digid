
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

package nl.logius.digid.app.domain.activation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.logius.digid.app.shared.request.AppRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

public class RsStartAppApplicationRequest extends AppRequest {

    @NotBlank
    @Valid
    @Schema(description = "Indicates if this application is going to be done with a un/pw authentication", example = "false")
    private String authenticate;

    @Valid
    @Schema(description = "username of DigiD accoount ", example = "PPPPPPPPPPP")
    private String username;

    @Valid
    @Schema(description = "password of DigiD accoount ", example = "SSSSSSSSSSSS")
    private String password;

    @NotBlank
    @Valid
    @Schema(description = "Name of the device", example = "PPPPPPPPPPPPPPPPP")
    private String deviceName;

    @NotBlank
    @Valid
    @Schema(description = "A valid instance id in uuid format", example = "550e8400-e29b-41d4-a716-099999470000")
    private String instanceId;

    public String getAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(String authenticate) {
        this.authenticate = authenticate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}

