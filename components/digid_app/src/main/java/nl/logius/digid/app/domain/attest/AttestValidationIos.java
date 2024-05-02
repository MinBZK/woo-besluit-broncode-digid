
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

package nl.logius.digid.app.domain.attest;

import com.google.common.primitives.Bytes;
import com.google.iot.cbor.*;
import nl.logius.digid.app.shared.ChallengeService;
import nl.logius.digid.app.shared.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttestValidationIos implements AttestValidator<String> {

    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");
    private static final String CERTIFICATE_EXTENSION_OID = "SSSSSSSSSSSSSSSSSSSSSS";
    private static final int CERTIFICATE_CHALLENGE_POSITION = 0;

    @Override
    public X509Certificate validate(String result, String rootCertificate, String challenge) throws CborParseException, CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, IOException {
        var decodedAttest = getCborObject(result);
        var authDataBytes = ((CborByteString) decodedAttest.get("authData")).byteArrayValue();

        var certificates = ((CborArray)((CborMap) decodedAttest.get("attStmt")).get("x5c")).listValue().stream().map(cbor -> Base64.getEncoder().encodeToString(((CborByteString) cbor).byteArrayValue())).toList();
        verifyCertificateChain(certificates, rootCertificate);

        var appCertificate = getX509Certificate(certificates.get(0));

        var clientDataHash = Bytes.concat(authDataBytes, Util.toSHA256(challenge));
        var nonce = DigestUtils.sha256(clientDataHash);

        var envelope = (DEROctetString) new ASN1InputStream(appCertificate.getExtensionValue(CERTIFICATE_EXTENSION_OID)).readObject();
        var sequence = (DLSequence) new ASN1InputStream(envelope.getOctetStream()).readObject();
        var taggedObject = (DLTaggedObject) sequence.getObjectAt(CERTIFICATE_CHALLENGE_POSITION);
        var taggedObjectOctet = (DEROctetString) taggedObject.getObject();

        if (!Arrays.equals(nonce, taggedObjectOctet.getOctets())) {
            throw new RuntimeException("Challenge not equal");
        }

        return appCertificate;
    }

    @Override
    public boolean validateAssertion(String assertion, String challenge, String appPublicKey)  {
        try {
            var decodedAssertion = getCborObject(assertion);
            var authenticatorDataBytes = ((CborByteString) decodedAssertion.get("authenticatorData")).byteArrayValue();
            var signatureBytes = ((CborByteString) decodedAssertion.get("signature")).byteArrayValue();

            var clientDataHash = Bytes.concat(authenticatorDataBytes, Util.toSHA256((challenge)));
            var nonce = DigestUtils.sha256(clientDataHash);

            return ChallengeService.verifySignature(nonce, signatureBytes, appPublicKey);
        } catch (Exception e) {
            return false;
        }
    }

    private CborMap getCborObject(String assertion) throws CborParseException {
        var decodedByteArray = isHexadecimal(assertion) ? Util.fromHex(assertion) : Base64.getDecoder().decode(assertion);

        return CborMap.createFromCborByteArray(decodedByteArray);
    }

    private boolean isHexadecimal(String input) {
        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(input);
        return matcher.matches();
    }
}
