
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

package nl.logius.digid.card.asn1.converters;

import java.util.List;

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1Field;

/**
 * Collection of fields that needs to be read in sequence
 */
public class FieldSequence extends Fields {
    private final boolean partial;
    private final Asn1Field[] fields;
    private int pos;

    public FieldSequence(boolean partial, List<Asn1Field> fields) {
        this.partial = partial;
        this.fields = fields.toArray(new Asn1Field[0]);
        pos = 0;
    }

    @Override
    public Asn1Field get(int tagNo) {
        final int current = pos;
        while (pos < fields.length) {
            final Asn1Field field = fields[pos++];
            if (field.tagNo == tagNo) {
                return field;
            }
            if (!field.property.optional()) {
                break;
            }
        }
        if (!partial) {
            throw new Asn1Exception("Unexpected tag %x encountered", tagNo);
        }
        pos = current;
        return null;
    }

    @Override
    public boolean done() {
        for (int i = pos; i < fields.length; i++) {
            if (!fields[i].property.optional()) {
                return false;
            }
        }
        return true;
    }
}
