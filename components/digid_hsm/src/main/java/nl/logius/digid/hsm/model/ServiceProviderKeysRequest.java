
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

import nl.logius.digid.hsm.crypto.Utils;

public class ServiceProviderKeysRequest extends VerificationPointsRequest {
    private int closingKeyVersion = 1;
    private byte[] certificate;
    private boolean identity = false;
    private boolean pseudonym = true;

    public ServiceProviderKeysInput toInput() {
        final ServiceProviderKeysInput input = new ServiceProviderKeysInput();
        input.setSchemeVersion(getSchemeVersion());
        input.setSchemeKeyVersion(getSchemeKeyVersion());
        input.setClosingKeyVersion(closingKeyVersion);
        input.setCertificate(Utils.decodeCertificate(certificate));
        return input;
    }

    @Min(1)
    public int getClosingKeyVersion() {
        return closingKeyVersion;
    }

    public void setClosingKeyVersion(int closingKeyVersion) {
        this.closingKeyVersion = closingKeyVersion;
    }

    @NotNull
    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    public boolean isIdentity() {
        return identity;
    }

    public void setIdentity(boolean identity) {
        this.identity = identity;
    }

    public boolean isPseudonym() {
        return pseudonym;
    }

    public void setPseudonym(boolean pseudonym) {
        this.pseudonym = pseudonym;
    }
}
