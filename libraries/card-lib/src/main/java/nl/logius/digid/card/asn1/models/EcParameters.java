
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import nl.logius.digid.card.asn1.Asn1Constructed;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.IdentifiedSequenceConverter;
import nl.logius.digid.card.asn1.converters.ObjectIdentifierConverter;
import nl.logius.digid.card.asn1.interfaces.Identifiable;

/*
 * the info of this part can be found here:
 * https://www.ietf.org/rfc/rfc3279.txt?number=3279 chapter 2.3.5 page 13
 */
@Asn1Entity(tagNo = 0x30, converter = IdentifiedSequenceConverter.class)
public class EcParameters implements Asn1Constructed, Identifiable, Serializable {
    private static final long serialVersionUID = 1L;

    private String identifier;
    private int version;
    private FieldId fieldId;
    private Curve curve;
    private byte[] base;
    private BigInteger order;
    private BigInteger cofactor;

    private transient ECCurve ecCurve;
    private transient ECPoint basePoint;

    @Override
    public void constructed(Asn1ObjectMapper mapper) {
        initialize();
    }

    public void initialize() {
        ecCurve = new ECCurve.Fp(fieldId.primeP, curve.getA(), curve.getB(), order, cofactor);
        basePoint = ecCurve.decodePoint(base);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initialize();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Asn1Property(tagNo = 0x02, order = 1)
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Asn1Property(tagNo = 0x30, order = 2)
    public FieldId getFieldId() {
        return fieldId;
    }

    public void setFieldId(FieldId fieldId) {
        this.fieldId = fieldId;
    }

    @Asn1Property(tagNo = 0x30, order = 3)
    public Curve getCurve() {
        return curve;
    }

    public void setCurve(Curve curve) {
        this.curve = curve;
    }

    @Asn1Property(tagNo = 0x04, order = 4)
    public byte[] getBase() {
        return base;
    }

    public void setBase(byte[] base) {
        this.base = base;
    }

    @Asn1Property(tagNo = 0x02, order = 5)
    public BigInteger getOrder() {
        return order;
    }

    public void setOrder(BigInteger order) {
        this.order = order;
    }

    @Asn1Property(tagNo = 0x02, order = 6, optional = true)
    public BigInteger getCofactor() {
        return cofactor;
    }

    public void setCofactor(BigInteger cofactor) {
        this.cofactor = cofactor;
    }

    /*
     * the info of this part can be found here:
     * https://www.ietf.org/rfc/rfc3279.txt?number=3279 chapter 2.3.5 page 14/15
     */
    @Asn1Entity
    public static class FieldId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String identifier;
        private BigInteger primeP;

        @Asn1Property(tagNo = 0x06, converter = ObjectIdentifierConverter.class, order = 1)
        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        @Asn1Property(tagNo = 0x02, order = 2)
        public BigInteger getPrimeP() {
            return primeP;
        }

        public void setPrimeP(BigInteger primeP) {
            this.primeP = primeP;
        }

    }

    /*
     * the info of this part can be found here:
     * https://www.ietf.org/rfc/rfc3279.txt?number=3279 chapter 2.3.5 page 13
     */
    @Asn1Entity
    public static class Curve implements Serializable {
        private static final long serialVersionUID = 1L;

        private BigInteger a;
        private BigInteger b;
        private byte[] seed;

        @Asn1Property(tagNo = 0x04, order = 1)
        public BigInteger getA() {
            return a;
        }

        public void setA(BigInteger a) {
            this.a = a;
        }

        @Asn1Property(tagNo = 0x04, order = 2)
        public BigInteger getB() {
            return b;
        }

        public void setB(BigInteger b) {
            this.b = b;
        }

        @Asn1Property(tagNo = 0x03, order = 3, optional = true)
        public byte[] getSeed() {
            return seed;
        }

        public void setSeed(byte[] seed) {
            this.seed = seed;
        }

    }

    public ECCurve getEcCurve() {
        return ecCurve;
    }

    public ECPoint getBasePoint() {
        return basePoint;
    }

    /**
     * Returns Bouncy Castle EC parameters
     * @return EC parameters
     */
    public ECDomainParameters getDomainParameters() {
        return new ECDomainParameters(ecCurve, basePoint, order, cofactor, curve.seed);
    }

    /**
     * Returns Java security parameters spec
     * @return parameter spec
     */
    public ECParameterSpec getParameterSpec() {
        return new ECParameterSpec(ecCurve, basePoint, order, cofactor, curve.seed);
    }
}
