
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

import java.security.GeneralSecurityException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class CertificateService implements CertificateVerifier {
    private Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract Collection<X509Certificate> getTrusted();
    protected abstract Collection<X509Certificate> getIntermediates();
    protected abstract Collection<X509CRL> getCRLs();
    protected abstract boolean checkRevocation();

    @Override
    public void verify(X509Certificate certificate, Date date) {
        logger.debug("Verifying {} issued by {}", certificate.getSubjectX500Principal(),
            certificate.getIssuerX500Principal());

        // Create trustAnchors
        final Set<TrustAnchor> trustAnchors = getTrusted().stream().map(
            c -> new TrustAnchor(c, null)
        ).collect(Collectors.toSet());
        if (trustAnchors.isEmpty()) {
            throw new VerificationException("No trust anchors available");
        }

        // Create the selector that specifies the starting certificate
        final X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(certificate);

        // Configure the PKIX certificate builder algorithm parameters
        try {
            final PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

            // Set assume date
            if (date != null) {
                pkixParams.setDate(date);
            }

            // Add cert store with certificate to check
            pkixParams.addCertStore(CertStore.getInstance(
                "Collection", new CollectionCertStoreParameters(ImmutableList.of(certificate)), "BC"));

            // Add cert store with intermediates
            pkixParams.addCertStore(CertStore.getInstance(
                "Collection", new CollectionCertStoreParameters(getIntermediates()), "BC"));

            // Add cert store with CRLs
            pkixParams.addCertStore(CertStore.getInstance(
                "Collection", new CollectionCertStoreParameters(getCRLs()), "BC"));

            // Toggle to check revocation list
            pkixParams.setRevocationEnabled(checkRevocation());

            // Build and verify the certification chain
            final CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
            builder.build(pkixParams);
        } catch (CertPathBuilderException e) {
            throw new VerificationException(
                String.format("Invalid certificate %s issued by %s",
                    certificate.getSubjectX500Principal(), certificate.getIssuerX500Principal()
                ), e
            );
        } catch (GeneralSecurityException e) {
            throw new CryptoException(
                String.format("Could not verify certificate %s issued by %s",
                    certificate.getSubjectX500Principal(), certificate.getIssuerX500Principal()
                ), e
            );
        }
    }
}
