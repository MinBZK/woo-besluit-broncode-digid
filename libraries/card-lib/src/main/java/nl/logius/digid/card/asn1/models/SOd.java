
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

package nl.logius.digid.card.asn1.models;

import org.bouncycastle.asn1.cms.ContentInfo;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.ContentInfoConverter;
import nl.logius.digid.card.crypto.CmsVerifier;

import java.util.Date;

@Asn1Entity(tagNo = 0x77)
public class SOd {
    private ContentInfo contentInfo;

    @Asn1Property(tagNo = 0x30, converter = ContentInfoConverter.class)
    public ContentInfo getContentInfo() {
        return contentInfo;
    }

    public void setContentInfo(ContentInfo contentInfo) {
        this.contentInfo = contentInfo;
    }

    public LdsSecurityObject toLdsSecurityObject(Asn1ObjectMapper mapper, CmsVerifier verifier, Date date) {
        final byte[] data = verifier.verifyMessage(contentInfo, date, LdsSecurityObject.OID);
        return mapper.read(data, LdsSecurityObject.class);
    }

    public LdsSecurityObject toLdsSecurityObject(Asn1ObjectMapper mapper, CmsVerifier verifier) {
        return toLdsSecurityObject(mapper, verifier, null);
    }

}
