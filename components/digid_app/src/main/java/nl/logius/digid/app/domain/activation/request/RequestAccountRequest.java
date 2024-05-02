
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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import nl.logius.digid.app.shared.request.AppRequest;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class RequestAccountRequest extends AppRequest {

    @NotBlank
    @Valid
    @Schema(example = "PPPPPPPPP")
    @JsonProperty("bsn")
    @JsonAlias("BSN")
    private String bsn;

    @NotBlank
    @Valid
    @Schema(example = "PPPPPPPP")
    private String dateOfBirth;

    @NotBlank
    @Valid
    @Schema(example = "PPPPPP")
    private String postalCode;

    @NotBlank
    @Valid
    @Schema(example = "76")
    private String houseNumber;

    @Valid
    @Schema()
    private String houseNumberAdditions;

    @NotBlank
    @Valid
    @Pattern(regexp = "NL|EN")
    @Schema(example = "NL")
    private String language;

    @Valid
    @Schema(example = "true")
    private boolean nfcSupport;

    public String getBsn() {
        return bsn;
    }

    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getHouseNumberAdditions() {
        return houseNumberAdditions;
    }

    public void setHouseNumberAdditions(String houseNumberAdditions) {
        this.houseNumberAdditions = houseNumberAdditions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean getNfcSupport() {
        return nfcSupport;
    }

    public void setNfcSupport(boolean nfcSupport) {
        this.nfcSupport = nfcSupport;
    }
}
