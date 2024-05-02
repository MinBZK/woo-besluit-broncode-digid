
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

package nl.logius.digid.eid.models.rest.app;

import javax.validation.constraints.NotNull;

public class PrepareEacRequest extends AppRequest {
    private byte[] dg14;
    private byte[] efCvca;
    private byte[] efSOd;
    private byte[] paceIcc;

    @NotNull
    public byte[] getDg14() {
        return dg14;
    }

    public void setDg14(byte[] dg14) {
        this.dg14 = dg14;
    }

    @NotNull
    public byte[] getEfCvca() {
        return efCvca;
    }

    public void setEfCvca(byte[] efCvca) {
        this.efCvca = efCvca;
    }

    @NotNull
    public byte[] getEfSOd() {
        return efSOd;
    }

    public void setEfSOd(byte[] efSOd) {
        this.efSOd = efSOd;
    }

    @NotNull
    public byte[] getPaceIcc() {
        return paceIcc;
    }

    public void setPaceIcc(byte[] paceIcc) {
        this.paceIcc = paceIcc;
    }

}
