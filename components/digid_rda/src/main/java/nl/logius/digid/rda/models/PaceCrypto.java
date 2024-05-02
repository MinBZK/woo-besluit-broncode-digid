
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

package nl.logius.digid.rda.models;

import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.crypto.MacProxy;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PaceCrypto {
    private PaceCrypto() { }
    private static final Logger logger = LoggerFactory.getLogger(PaceCrypto.class);
    private static final byte[] ENCODED_PROTOCOL_OID = Asn1Utils.tlv(0x06, o -> {
        try {
            Asn1Utils.encodeObjectIdentifier("0.4.0.127.0.7.2.2.4.2.4", o);
        } catch (IOException e) {
             logger.error("Encoding exception");
        }
    });

    public static byte[] decryptNonce(byte[] secret, byte[] encryptedNonce) {
        try {
            final var nonce = new byte[encryptedNonce.length];
            final var cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
            cipher.init(false, new KeyParameter(secret));
            cipher.doFinal(nonce, cipher.processBytes(encryptedNonce, 0, encryptedNonce.length, nonce, 0));

            return nonce;
        } catch (InvalidCipherTextException e) {
            logger.error("Cipher not found");
        }

        return new byte[0];
    }

    public static byte[] getTerminalTokenFromPublicKey(ECPoint publicPoint, byte[] kMac) {
        final var cardPublicKeyBytes = publicPoint.getEncoded(false);
        final var cardEllipticCoordinates = Asn1Utils.tlv(0x86, out -> out.write(cardPublicKeyBytes));
        final var cardAuthTokenInputData = getAuthTokenInputData(cardEllipticCoordinates);

        return MacProxy.calculate(new CMac(new AESEngine(), 64), new KeyParameter(kMac), m -> m.update(cardAuthTokenInputData));
    }

    private static byte[] getAuthTokenInputData(byte[] coordinatesEncrypted) {
        return Asn1Utils.tlv(0x7f49, out -> {
            out.write(ENCODED_PROTOCOL_OID);
            out.write(coordinatesEncrypted);
        });
    }

}
