
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

import java.util.Arrays;

import org.bouncycastle.asn1.BERTags;

import nl.logius.digid.card.asn1.Asn1Raw;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.EcSignable;

@Asn1Entity(tagNo = 0x7f21)
public class CvCertificate implements Asn1Raw, EcSignable {
    @Asn1Entity(tagNo = 0x65)
    public static class Extensions {
        private DataTemplate ddt;

        @Asn1Property(tagNo=0x73)
        public DataTemplate getDdt() {
            return ddt;
        }

        public void setDdt(DataTemplate ddt) {
            this.ddt = ddt;
        }
    }

    @Asn1Entity(tagNo = 0x7f4e)
    public static class Body implements Asn1Raw {
        private byte[] raw;

        private int identifier;
        private String car;
        private PublicKeyInfo publicKey;
        private String chr;
        private DataTemplate chat;
        private Integer effectiveDate;
        private Integer expirationDate;
        private Extensions extensions;

        @Override
        public byte[] getRaw() {
            return raw;
        }

        @Override
        public void setRaw(byte[] raw) {
            this.raw = raw;
        }

        @Asn1Property(order = 0, tagNo = 0x5f29)
        public int getIdentifier() {
            return identifier;
        }

        public void setIdentifier(int identifier) {
            this.identifier = identifier;
        }

        @Asn1Property(order = 1, tagNo = 0x42)
        public String getCar() {
            return car;
        }

        public void setCar(String car) {
            this.car = car;
        }

        @Asn1Property(order = 2, tagNo = 0x7f49)
        public PublicKeyInfo getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(PublicKeyInfo publicKey) {
            this.publicKey = publicKey;
        }

        @Asn1Property(order = 3, tagNo = 0x5f20)
        public String getChr() {
            return chr;
        }

        public void setChr(String chr) {
            this.chr = chr;
        }

        @Asn1Property(order = 4, tagNo = 0x7f4c, optional = true)
        public DataTemplate getChat() {
            return chat;
        }

        public void setChat(DataTemplate chat) {
            this.chat = chat;
        }

        @Asn1Property(order = 5, tagNo = 0x5f25, converter = DateConverter.class, optional = true)
        public Integer getEffectiveDate() {
            return effectiveDate;
        }

        public void setEffectiveDate(Integer effectiveDate) {
            this.effectiveDate = effectiveDate;
        }

        @Asn1Property(order = 6, tagNo = 0x5f24, converter = DateConverter.class, optional = true)
        public Integer getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(Integer expirationDate) {
            this.expirationDate = expirationDate;
        }

        @Asn1Property(order = 7, tagNo = 0x65, optional = true)
        public Extensions getExtensions() {
            return extensions;
        }

        public void setExtensions(Extensions extensions) {
            this.extensions = extensions;
        }

        public PolymorphType getAuthorization() {
            if (extensions == null || extensions.ddt == null) {
                return null;
            }
            final byte a = extensions.ddt.getData()[0];
            switch (a) {
                case 3:
                    return PolymorphType.PIP;
                case 1:
                    return PolymorphType.PP;
                default:
                    throw new IllegalArgumentException(String.format("Unknown authorization %02x", a));
            }
        }

        public void setAuthorization(PolymorphType authorization) {
            if (authorization == null) {
                this.extensions = null;
                return;
            }
            final DataTemplate ddt = new DataTemplate();
            ddt.setOid(ObjectIdentifiers.id_PCA_AT);
            switch (authorization) {
                case PIP:
                    ddt.setData(new byte[] { 3 });
                    break;
                case PP:
                    ddt.setData(new byte[] { 1 });
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported authorization " + authorization);
            }
            this.extensions = new Extensions();
            this.extensions.ddt = ddt;
        }
    }

    private byte[] raw;
    private Body body;
    private EcSignature signature;

    @Override
    public byte[] getRaw() {
        return raw;
    }

    @Override
    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    @Override
    public byte[] getTBS() {
        return body.raw;
    }

    @Asn1Property(order=0)
    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    @Asn1Property(order=1)
    @Override
    public EcSignature getSignature() {
        return signature;
    }

    public void setSignature(EcSignature signature) {
        this.signature = signature;
    }

    public byte[] getAsSequence() {
        return getAsSequence(raw);
    }

    public static byte[] getAsSequence(byte[] raw) {
        // We need a sequence tag instead of a CvCertificate tag
        final byte[] seq = Arrays.copyOfRange(raw, 1, raw.length);
        seq[0] = BERTags.CONSTRUCTED | BERTags.SEQUENCE;
        return seq;
    }
}
