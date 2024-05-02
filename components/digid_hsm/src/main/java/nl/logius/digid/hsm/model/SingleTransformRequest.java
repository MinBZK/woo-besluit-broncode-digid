
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

import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableMap;

public class SingleTransformRequest implements TransformInput {
    private byte[] polymorph;
    private String oin;
    private final TransformOptions options = new TransformOptions();
    private int targetMsgVersion = 1;

    @Override
    @NotNull
    public byte[] getPolymorph() {
        return polymorph;
    }
    public void setPolymorph(byte[] polymorph) {
        this.polymorph = polymorph;
    }

    @NotNull
    public String getOin() {
        return oin;
    }

    public void setOin(String oin) {
        this.oin = oin;
    }

    @Min(value = 1)
    public int getKsv() {
        return options.getKsv();
    }

    public void setKsv(int ksv) {
        options.setKsv(ksv);
    }

    public boolean isIdentity() {
        return options.isIdentity();
    }

    public void setIdentity(boolean identity) {
        options.setIdentity(identity);
    }

    public boolean isPseudonym() {
        return options.isPseudonym();
    }

    public void setPseudonym(boolean pseudonym) {
        options.setPseudonym(pseudonym);
    }

    @Override
    public Map<String, TransformOptions> getRequests() {
        return ImmutableMap.of(oin, options);
    }

    @Override
    public int getTargetMsgVersion() {
        return targetMsgVersion;
    }

    @Override
    public void setTargetMsgVersion(int targetMsgVersion) {
        this.targetMsgVersion = targetMsgVersion;
    }
}
