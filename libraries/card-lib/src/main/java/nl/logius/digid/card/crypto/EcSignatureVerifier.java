
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

import java.security.MessageDigest;

import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;

import nl.logius.digid.card.asn1.models.EcSignature;

public class EcSignatureVerifier implements SignatureVerifier {
    final ECDSASigner signer;

    public EcSignatureVerifier(ECPublicKeyParameters params) {
        signer = new ECDSASigner();
        signer.init(false, params);
    }

    @Override
    public void verify(byte[] data, byte[] signature, MessageDigest digest) {
        verify(data, new EcSignature(signature), digest);
    }

    public void verify(byte[] data, EcSignature signature, MessageDigest digest) {
        final byte[] message;
        if (digest == null) {
            message = data;
        } else {
            message = digest.digest(data);
        }
        final boolean result = signer.verifySignature(message, signature.r, signature.s);
        if (!result) {
            throw new VerificationException("Invalid signature");
        }
    }
}
