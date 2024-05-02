
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


import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.*;
import java.util.Base64;
import java.util.List;

public interface AttestValidator<T> {

    X509Certificate validate(T body, String rootCertificate, String challenge) throws Exception;
    boolean validateAssertion(String assertion, String challenge, String appPublicKe) throws Exception;

    default X509Certificate getX509Certificate(String cert) throws CertificateException {
        byte [] certDecoded = Base64.getDecoder().decode(cert);
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certDecoded));
    }

    default void verifyCertificateChain(List<String> certificates, String rootCertificate) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        var parent = getX509Certificate(rootCertificate);

        for (int i = certificates.size() - 1; i >= 0; i--) {
            X509Certificate cert = getX509Certificate(certificates.get(i));
            cert.checkValidity();
            cert.verify(parent.getPublicKey());
            parent = cert;
        }
    }
}
