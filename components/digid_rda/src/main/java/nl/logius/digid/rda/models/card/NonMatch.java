
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

package nl.logius.digid.rda.models.card;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.rda.utils.MrzDataObject;

import java.nio.charset.StandardCharsets;

@Asn1Entity(tagNo = 0x71)
public class NonMatch {
    private byte[] bapMrz;
    private MrzDataObject mrzDataObject;
    private String sai;

    public String getInputMrz() {
        return new String(bapMrz, 1, bapMrz.length - 1, StandardCharsets.US_ASCII);
    }

    @Asn1Property(tagNo = 0x82, order = 1)
    public byte[] getBapMrz() {
        return bapMrz;
    }

    public void setBapMrz(byte[] inputMrz) {
        this.bapMrz = inputMrz;
        this.mrzDataObject = new MrzDataObject("D" + getInputMrz());
    }

    @Asn1Property(tagNo = 0x81, order = 2)
    public String getSai() {
        return sai;
    }

    public void setSai(String sai) {
        this.sai = sai;
    }

    public String getDocumentNumber() {
        return mrzDataObject.getDocumentNumber();
    }
}
