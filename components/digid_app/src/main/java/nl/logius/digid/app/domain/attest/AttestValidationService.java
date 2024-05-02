
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

import nl.logius.digid.app.domain.authentication.request.AuthenticateRequest;
import nl.logius.digid.app.domain.authentication.request.WidUpgradeRequest;
import nl.logius.digid.app.shared.Util;
import org.bouncycastle.asn1.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;


@Service
public class AttestValidationService {
    private final String androidRootCertificate;
    private final String iosRootCertificate;

    enum Platform {
        IOS, ANDROID
    }

    public AttestValidationService(@Value("${attestation.root_certificate.android}") String androidRootCertificate, @Value("${attestation.root_certificate.ios}") String iosRootCertificate) {
        this.androidRootCertificate = androidRootCertificate;
        this.iosRootCertificate = iosRootCertificate;
    }

    public AttestValidator getValidator(Platform platform) {
         return switch(platform) {
             case ANDROID -> new AttestValidationAndroid();
             case IOS -> new AttestValidationIos();
        };
    }

    public String validate(WidUpgradeRequest request, String challenge) throws IOException {
        X509Certificate appCertificate;
        AttestValidator validator = getValidator(request.getIosResult() == null ? Platform.ANDROID : Platform.IOS );

        try {
            if (request.getAndroidCertificates() != null) {
                appCertificate = validator.validate(request.getAndroidCertificates(), androidRootCertificate, challenge);
            } else {
                appCertificate = validator.validate(request.getIosResult(), iosRootCertificate, challenge);
            }
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong", e);
        }

        return Util.toHexLower(transformRawPublicKey(appCertificate.getPublicKey().getEncoded()));
    }

    public boolean validateAssertion(AuthenticateRequest request, String challenge, String appPublicKey) {
        try {
            return getValidator(Platform.IOS).validateAssertion(request.getSignedChallenge(), challenge, appPublicKey);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static byte[] transformRawPublicKey(byte[] rawPublicKey) throws IOException {
        if (rawPublicKey.length == 91) {
            var sequence = (DLSequence) new ASN1InputStream(rawPublicKey).readObject();
            return ((DERBitString) sequence.getObjectAt(1)).getOctets();
        }

        return rawPublicKey;
    }
}




