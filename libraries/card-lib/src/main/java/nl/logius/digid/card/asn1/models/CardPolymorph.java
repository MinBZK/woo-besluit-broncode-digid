
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

import nl.logius.digid.card.asn1.converters.IdentifiedSequenceConverter;
import org.bouncycastle.math.ec.ECPoint;

import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.converters.BcdAsStringConverter;
import nl.logius.digid.card.asn1.converters.CardPointConverter;
import nl.logius.digid.card.asn1.interfaces.Identifiable;

@Asn1Entity(tagNo = 0x7c, converter = IdentifiedSequenceConverter.class, param = 0xa0)
public class CardPolymorph implements Identifiable {

    private String identifier;
    private ECPoint point0;
    private ECPoint point1;
    private ECPoint point2;

    private int schemeVersion;
    private int schemeKeyVersion;
    private String creator;
    private String recipient;
    private int recipientKeySetVersion;
    private int type;
    private String sequenceNo;

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Asn1Property(tagNo = 0x80, converter = CardPointConverter.class)
    public ECPoint getPoint0() {
        return point0;
    }

    public void setPoint0(ECPoint point0) {
        this.point0 = point0;
    }

    @Asn1Property(tagNo = 0x81, converter = CardPointConverter.class, optional = true)
    public ECPoint getPoint1() {
        return point1;
    }

    public void setPoint1(ECPoint point1) {
        this.point1 = point1;
    }

    @Asn1Property(tagNo = 0x82  , converter = CardPointConverter.class)
    public ECPoint getPoint2() {
        return point2;
    }

    public void setPoint2(ECPoint point2) {
        this.point2 = point2;
    }

    @Asn1Property(tagNo = 0x85)
    public int getSchemeVersion() {
        return schemeVersion;
    }

    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    @Asn1Property(tagNo = 0x86)
    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public void setSchemeKeyVersion(int schemeKeyVersion) {
        this.schemeKeyVersion = schemeKeyVersion;
    }

    @Asn1Property(tagNo = 0x87, converter = BcdAsStringConverter.class)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Asn1Property(tagNo = 0x88, converter = BcdAsStringConverter.class)
    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    @Asn1Property(tagNo = 0x89)
    public int getRecipientKeySetVersion() {
        return recipientKeySetVersion;
    }

    public void setRecipientKeySetVersion(int recipientKeySetVersion) {
        this.recipientKeySetVersion = recipientKeySetVersion;
    }

    @Asn1Property(tagNo = 0x8a)
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Asn1Property(tagNo = 0x8b, converter = BcdAsStringConverter.class)
    public String getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(String sequenceNo) {
        this.sequenceNo = sequenceNo;
    }
}
