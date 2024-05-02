
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

package nl.logius.digid.eid.models.asn1;

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.converters.SimpleConverter;

public class DateConverter extends SimpleConverter<Integer> {
    @Override
    public Integer deserialize(Asn1ObjectInputStream in) {
        if (in.length != 6) {
            throw new Asn1Exception("Length of date should be 6");
        }
        int value = 0;
        for (int i = 0; i < in.length; i++) {
            value = (value * 10) + in.read();
        }
        return value;
    }

    @Override
    public void serialize(Asn1OutputStream out, Integer obj) {
        int value = obj;
        int exp = 100000;
        for (int i = 0; i < 6; i++) {
            int b = value / exp;
            out.write(b);
            value -= b * exp;
            exp /= 10;
        }
    }
}
