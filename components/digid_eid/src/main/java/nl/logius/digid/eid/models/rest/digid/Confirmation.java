
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

package nl.logius.digid.eid.models.rest.digid;

import nl.logius.digid.eid.models.DocumentType;

import java.util.Arrays;
import java.util.Objects;

public class Confirmation {
    public final byte[] polymorph;
    public final DocumentType documentType;
    public final String sequenceNo;

    public Confirmation(byte[] polymorph, DocumentType documentType, String sequenceNo) {
        this.polymorph = polymorph;
        this.documentType = documentType;
        this.sequenceNo = sequenceNo;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Confirmation)) return false;
        Confirmation that = (Confirmation) o;
        return Arrays.equals(polymorph, that.polymorph) &&
            documentType == that.documentType &&
            Objects.equals(sequenceNo, that.sequenceNo);
    }

    @Override
    public final int hashCode() {
        int result = Objects.hash(documentType, sequenceNo);
        result = 31 * result + Arrays.hashCode(polymorph);
        return result;
    }
}
