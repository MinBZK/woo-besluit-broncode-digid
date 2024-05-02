
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
import nl.logius.digid.app.shared.request.AppSessionRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

public class ChallengeResponseRequest extends AppSessionRequest {
    @NotBlank
    @Valid
    @Schema(description = "App public key in hex string", example = "046b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c2964fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5")
    private String appPublicKey;

    @NotBlank
    @Valid
    @Schema(description = "Signed challenge", example = "3045022100a49a32ca8152a73c1b363b14fa87fd539dfceb5a1a2ce20e607e163e16b9189302201240de39557f71bddcd634d837a8e0c958d25f2ec39b6517618bbd53d8c359ba")
    private String signedChallenge;

    @Valid
    private Boolean hardwareSupport;
    @Valid
    private Boolean nfcSupport;

    public String getAppPublicKey() {
        return appPublicKey;
    }

    public void setAppPublicKey(String appPublicKey) {
        this.appPublicKey = appPublicKey;
    }

    public String getSignedChallenge() {
        return signedChallenge;
    }

    public void setSignedChallenge(String signedChallenge) {
        this.signedChallenge = signedChallenge;
    }

    public Boolean isHardwareSupport() {
        return hardwareSupport;
    }

    public void setHardwareSupport(Boolean hardwareSupport) {
        this.hardwareSupport = hardwareSupport;
    }

    public Boolean isNfcSupport() {
        return nfcSupport;
    }

    public void setNfcSupport(Boolean nfcSupport) {
        this.nfcSupport = nfcSupport;
    }
}
