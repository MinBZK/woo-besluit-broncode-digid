
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

import nl.logius.digid.app.shared.Util;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class AttestValidationAndroid implements AttestValidator<List<String>> {

    public static final String CERTIFICATE_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17";
    public static final int CERTIFICATE_CHALLENGE_POSITION = 4;
    private final String[] allowedAppIds = new String[]{ "SSSSSSSSSSSSSSSSSSSSSSSSSS", "SSSSSSSSSSSSSSSSSSSSSS" };

    @Override
    public X509Certificate validate(List<String> certificates, String rootCertificate, String challenge) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException, IOException {
        verifyCertificateChain(certificates, rootCertificate);

        var appCertificate = getX509Certificate(certificates.get(0));

        var envelope = (DEROctetString) new ASN1InputStream(appCertificate.getExtensionValue(CERTIFICATE_EXTENSION_OID)).readObject();
        var sequence = (DLSequence) new ASN1InputStream(envelope.getOctetStream()).readObject();
        var value = (DEROctetString) sequence.getObjectAt(CERTIFICATE_CHALLENGE_POSITION);

        validateAppId(sequence);

        if (!Util.toHexUpper(Util.toSHA256(challenge)).equals(Util.toHexUpper(value.getOctets()))) {
            throw new RuntimeException("Challenge not equal");
        }

        return appCertificate;
    }

    private void validateAppId(DLSequence sequence) throws IOException {
        var dlSequence = (DLSequence) sequence.getObjectAt(6);
        var taggedObject = (DLTaggedObject) dlSequence.getObjectAt(1);
        var octetString = (DEROctetString) taggedObject.getObject();
        var dlSequence2 = (DLSequence) new ASN1InputStream(octetString.getOctets()).readObject();
        var dlSet = (DLSet)dlSequence2.getObjectAt(0);
        var dlSequence3 = (DLSequence) (dlSet).getObjectAt(0);
        var octetString2 = (DEROctetString) dlSequence3.getObjectAt(0);
        var appId = new String(octetString2.getOctets());

        if (!Arrays.asList(allowedAppIds).contains(appId)) {
            throw new RuntimeException("AppId not allowed");
        }
    }

    @Override
    public boolean validateAssertion(String assertion, String challenge,  String appPublicKe) {
        return true;
    }
}
