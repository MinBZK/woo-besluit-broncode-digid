
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
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import nl.logius.digid.card.asn1.Asn1Exception;

public class CmsVerifier {

    private final CertificateVerifier certificateVerifier;
    private final DigestCalculatorProvider digestProvider;
    private final Provider bcProvider = new BouncyCastleProvider();

    public CmsVerifier(CertificateVerifier certificateVerifier) {
        this.certificateVerifier = certificateVerifier;
        try {
            digestProvider = new JcaDigestCalculatorProviderBuilder().setProvider(bcProvider).build();
        } catch (OperatorCreationException e) {
            throw new CryptoException("Unexpected operator creation exception", e);
        }
    }

    public ContentInfo verify(ContentInfo signedMessage, Date date) {
        final SignedData signedData = SignedData.getInstance(signedMessage.getContent());
        final X509Certificate cert = certificate(signedData);
        certificateVerifier.verify(cert, date);

        final X500Name name = X500Name.getInstance(cert.getIssuerX500Principal().getEncoded());
        try {
            final CMSSignedData cms = new CMSSignedData(signedMessage);
            cms.verifySignatures(signerId -> {
                if (!name.equals(signerId.getIssuer())) {
                    throw new VerificationException("Issuer does not match certificate");
                }
                if (!cert.getSerialNumber().equals(signerId.getSerialNumber())) {
                    throw new VerificationException("Serial number does not match certificate");
                }
                return new JcaSignerInfoVerifierBuilder(digestProvider).setProvider(bcProvider).build(cert);
            });
        } catch (CMSException e) {
            throw new VerificationException("Could not verify CMS", e);
        }
        return signedData.getEncapContentInfo();
    }

    public ContentInfo verify(ContentInfo signedMessage) {
        return verify(signedMessage, null);
    }

    public byte[] verifyMessage(ContentInfo signedMessage, Date date, String oid) {
        return encapsulatedData(verify(signedMessage, date), oid);
    }

    public byte[] verifyMessage(ContentInfo signedMessage, String oid) {
        return encapsulatedData(verify(signedMessage, null), oid);
    }

    public static byte[] message(ContentInfo signedMessage, String oid) {
        return encapsulatedData(
            SignedData.getInstance(signedMessage.getContent()).getEncapContentInfo(),
            oid);
    }

    public static X509Certificate certificate(ContentInfo signedMessage) {
        return certificate(SignedData.getInstance(signedMessage.getContent()));
    }

    private static X509Certificate certificate(SignedData signedData) {
        if (signedData.getCertificates().size() != 1) {
            throw new VerificationException("Only single signed messages are supported");
        }
        final X509Certificate cert;
        try {
            return X509Factory.toCertificate(
                signedData.getCertificates().getObjectAt(0).toASN1Primitive().getEncoded()
            );
        } catch (IOException e) {
            throw new CryptoException("Could not read certificate", e);
        }
    }

    private static byte[] encapsulatedData(ContentInfo message, String oid) {
        if (!oid.equals(message.getContentType().getId())) {
            throw new Asn1Exception("Unexpected content type " + message.getContentType());
        }
        if (!(message.getContent() instanceof ASN1OctetString)) {
            throw new Asn1Exception("Encapsulated content info should be octet string");
        }
        return ((ASN1OctetString) message.getContent()).getOctets();
    }
}
