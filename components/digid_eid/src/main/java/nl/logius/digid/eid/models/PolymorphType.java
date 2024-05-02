
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

package nl.logius.digid.eid.models;

import java.io.IOException;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import nl.logius.digid.eid.models.asn1.ObjectIdentifiers;

public enum PolymorphType {
    PIP(ObjectIdentifiers.id_PCA_PIP), PP(ObjectIdentifiers.id_PCA_PP);

    private final ASN1ObjectIdentifier oid;
    private final byte[] tag80;

    PolymorphType(ASN1ObjectIdentifier oid) {
        this.oid = oid;
        try {
            tag80 = this.oid.getEncoded();
        } catch (IOException e) {
            throw new RuntimeException("Could not get encoded oid of " + oid, e);
        }
        tag80[0] = (byte) 0x80;
    }

    public ASN1ObjectIdentifier getOid() {
        return oid;
    }

    public byte[] getTag80() {
        return tag80;
    }

}
