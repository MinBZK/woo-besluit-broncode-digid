
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
import java.io.Serializable;
import java.math.BigInteger;

import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

/**
 * Serializable class that holds parameters of EC curve and private key
 *
 * Converts back and to both Java crypto API and BouncyCastle both
 */
public class EcPrivateKey extends EcParameters implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BigInteger d;
    private transient ECPoint q;

    public EcPrivateKey(ECPrivateKeyParameters params) {
        super(params.getParameters());
        d = params.getD();
        q = calculatePublicPoint(getBase(), d);
    }

    public EcPrivateKey(ECPrivateKeySpec spec) {
        super(spec.getParams());
        d = spec.getD();
        q = calculatePublicPoint(getBase(), d);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        q = calculatePublicPoint(getBase(), d);
    }

    public static ECPoint calculatePublicPoint(ECPoint base, BigInteger d) {
        return new FixedPointCombMultiplier().multiply(base, d).normalize();
    }

    public BigInteger getD() {
        return d;
    }

    public ECPoint getQ() {
        return q;
    }

    public ECPrivateKeyParameters toPrivateKeyParameters() {
        return new ECPrivateKeyParameters(d, toDomainParameters());
    }

    public ECPrivateKeySpec toPrivateKeySpec() {
        return new ECPrivateKeySpec(d, toParameterSpec());
    }

    public ECPublicKeyParameters toPublicKeyParameters() {
        return new ECPublicKeyParameters(getQ(), toDomainParameters());
    }

    public ECPublicKeySpec toPublicKeySpec() {
        return new ECPublicKeySpec(getQ(), toParameterSpec());
    }
}
