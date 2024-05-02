
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

package nl.logius.digid.eid.util;

import java.io.IOException;
import java.math.BigInteger;

import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

import nl.logius.digid.card.apdu.AESSecureMessaging;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.crypto.MacProxy;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.sharedlib.model.ByteArray;

/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
public final class KeyUtils {
    private KeyUtils() {
    }

    private static final byte[] ENCODED_CA_OID = Asn1Utils.tlv(0x06, o -> {
        try {
            Asn1Utils.encodeObjectIdentifier("0.4.0.127.0.7.2.2.3.2.4", o);
        } catch (IOException e) {
            throw new RuntimeException("Could not encode OID", e);
        }
    });

    public static byte[] getEncodedPublicPoint(ECPublicKeySpec publicSpec) {
        return new BCECPublicKey("EC", publicSpec, BouncyCastleProvider.CONFIGURATION).getEncoded();
    }

    public static byte[] calcDataToSign(byte[] challenge, ECPoint publicKey, byte[] idpicc) {
        return Arrays.concatenate(idpicc, challenge, publicKey.getAffineXCoord().getEncoded());
    }

    /**
     * Create the terminal token from the ca public key that we use to check with
     * the tpicc. This is a small snippet from code in nlcard, rest was not needed.
     *
     * @param publicPoint
     *            public point from ca
     * @param kMac
     *            mac key
     * @return returns a byte[] with the terminaltoken
     */
    public static byte[] getTerminalTokenFromCaPublicKey(ECPoint publicPoint, byte[] kMac) {
        final byte[] cardPublicKeyBytes = publicPoint.getEncoded(false);
        final byte[] cardEllipticCoordinates = Asn1Utils.tlv(0x86, out -> out.write(cardPublicKeyBytes));
        final byte[] cardAuthTokenInputData = getAuthTokenInputData(cardEllipticCoordinates);

        // TODO: Mac should be calculated from CA info
        return MacProxy.calculate(new CMac(new AESEngine(), 64), new KeyParameter(kMac), m -> m.update(cardAuthTokenInputData));
    }

    /**
     * get an auth token input data from ellpitic coordinates and the caoid
     *
     * @param coordinatesEncrypted
     *            cardEllipticCoordinates
     * @return
     */
    private static byte[] getAuthTokenInputData(byte[] coordinatesEncrypted) {
        return Asn1Utils.tlv(0x7f49, out -> {
            out.write(ENCODED_CA_OID);
            out.write(coordinatesEncrypted);
        });
    }

    public static void generateSecretKeys(EidSession session, ECPublicKeyParameters pkCaIcc, byte[] rpicc) {
        final BasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(session.getEphemeralKey().toPrivateKeyParameters());
        final BigInteger x = basicAgreement.calculateAgreement(pkCaIcc);
        // Copied from org.bouncycastle.math.ec.ECFieldElement.getEncoded
        final byte[] xEncoded = BigIntegers.asUnsignedByteArray((pkCaIcc.getParameters().getCurve().getFieldSize() + 7) / 8, x);
        // Calculate KEnc and KMac from shared X coord secret
        session.setkEnc(new ByteArray(AESSecureMessaging.deriveEnc(xEncoded, rpicc)));
        session.setkMac(new ByteArray(AESSecureMessaging.deriveMac(xEncoded, rpicc)));
    }
}
