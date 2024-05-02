
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

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.eid.models.EcPublicParams;

@Asn1Entity(tagNo = 0x7f49)
public class PublicKeyInfo implements EcPublicParams {
    private ASN1ObjectIdentifier oid;

    /**
     * Prime modulus
     */
    private BigInteger p;

    /**
     * First coefficient
     */
    private BigInteger a;

    /**
     * Second coefficient
     */
    private BigInteger b;

    /**
     * Base point
     */
    private byte[] g;

    /**
     * Order of base points
     */
    private BigInteger q;

    /**
     * Public point
     */
    private byte[] key;

    /**
     * Cofactor
     */
    private BigInteger h;

    @Asn1Property(tagNo = 0x06)
    public ASN1ObjectIdentifier getOid() {
        return oid;
    }

    public void setOid(ASN1ObjectIdentifier oid) {
        this.oid = oid;
    }

    @Asn1Property(tagNo = 0x81, optional = true)
    public BigInteger getP() {
        return p;
    }

    public void setP(BigInteger p) {
        this.p = p;
    }

    @Asn1Property(tagNo = 0x82, optional = true)
    public BigInteger getA() {
        return a;
    }

    public void setA(BigInteger a) {
        this.a = a;
    }

    @Asn1Property(tagNo = 0x83, optional = true)
    public BigInteger getB() {
        return b;
    }

    public void setB(BigInteger b) {
        this.b = b;
    }

    @Asn1Property(tagNo = 0x84, optional = true)
    public byte[] getG() {
        return g;
    }

    public void setG(byte[] g) {
        this.g = g;
    }

    @Asn1Property(tagNo = 0x85, optional = true)
    public BigInteger getQ() {
        return q;
    }

    public void setQ(BigInteger q) {
        this.q = q;
    }

    @Asn1Property(tagNo = 0x86)
    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    @Asn1Property(tagNo = 0x87, optional = true)
    public BigInteger getH() {
        return h;
    }

    public void setH(BigInteger h) {
        this.h = h;
    }

    public ECCurve getCurve() {
        return new ECCurve.Fp(p, a, b, q, h);
    }

    public ECDomainParameters getParams() {
        final ECCurve curve = getCurve();
        return new ECDomainParameters(curve, curve.decodePoint(g), q, h);
    }

    public void setParams(ECDomainParameters domainParams) {
        p = domainParams.getCurve().getField().getCharacteristic();
        a = domainParams.getCurve().getA().toBigInteger();
        b = domainParams.getCurve().getB().toBigInteger();
        g = domainParams.getG().getEncoded(false);
        q = domainParams.getN();
        h = domainParams.getH();
    }

    @Override
    public AlgorithmIdentifier getDigestAlgorithm() {
        return new AlgorithmIdentifier(oid);
    }

    @Override
    public ECPoint getPoint(ECCurve curve) {
        return curve.decodePoint(key);
    }
}
