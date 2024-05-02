
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

package nl.logius.digid.card.crypto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

/**
 * Serializable class that holds parameters of EC curve
 *
 * Converts back and to both Java crypto API and BouncyCastle* both
 */
public class EcParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient ECCurve curve;
    private transient ECPoint base;
    private final byte[] seed;

    public EcParameters(ECDomainParameters params) {
        curve = params.getCurve();
        seed = params.getSeed();
        base = params.getG();
    }

    public EcParameters(ECParameterSpec spec) {
        curve = spec.getCurve();
        seed = spec.getSeed();
        base = spec.getG();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        final BigInteger p = (BigInteger) ois.readObject();
        final BigInteger a = (BigInteger) ois.readObject();
        final BigInteger b = (BigInteger) ois.readObject();
        final BigInteger n = (BigInteger) ois.readObject();
        final BigInteger h = (BigInteger) ois.readObject();
        curve = new ECCurve.Fp(p, a, b, n, h);
        base = curve.decodePoint((byte[]) ois.readObject());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(curve.getField().getCharacteristic());
        oos.writeObject(curve.getA().toBigInteger());
        oos.writeObject(curve.getB().toBigInteger());
        oos.writeObject(curve.getOrder());
        oos.writeObject(curve.getCofactor());
        oos.writeObject(base.getEncoded(true));
    }

    public ECDomainParameters toDomainParameters() {
        return new ECDomainParameters(curve, base, curve.getOrder(), curve.getCofactor(), seed);
    }

    public ECParameterSpec toParameterSpec() {
        return new ECParameterSpec(curve, base, curve.getOrder(), curve.getCofactor(), seed);
    }

    protected ECCurve getCurve() {
        return curve;
    }

    protected ECPoint getBase() {
        return base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EcParameters)) return false;
        EcParameters that = (EcParameters) o;
        return Objects.equals(getCurve(), that.getCurve()) &&
            Objects.equals(getBase(), that.getBase()) &&
            Arrays.equals(seed, that.seed);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getCurve(), getBase());
        result = 31 * result + Arrays.hashCode(seed);
        return result;
    }
}
