
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

package nl.logius.digid.eid.service;

import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.logius.digid.card.crypto.CertificateService;
import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.db.CRL;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.repository.CRLRepository;
import nl.logius.digid.eid.repository.CertificateRepository;

@Service
public class CSCACertificateService extends CertificateService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CertificateRepository repository;

    @Autowired
    private CRLRepository crlRepository;

    @Value("${check_revocation_list}")
    private boolean checkRevocationList;

    @Value("${allow_adding_expired:false}")
    private boolean allowAddingExpired;

    @Override
    protected Collection<X509Certificate> getTrusted() {
        return repository.findTrustedCscaCertificates().stream().map(
                c -> X509Factory.toCertificate(c.getRaw())
        ).collect(Collectors.toList());
    }

    @Override
    protected Collection<X509Certificate> getIntermediates() {
        return Collections.emptyList();
    }

    @Override
    protected Collection<X509CRL> getCRLs() {
        return crlRepository.findAll().stream().map(
                c -> X509Factory.toCRL(c.getRaw())
        ).collect(Collectors.toList());
    }

    @Override
    protected boolean checkRevocation() {
        return checkRevocationList;
    }

    public Certificate add(X509Certificate cert) {
        final Certificate db;
        try {
            db = Certificate.from(cert);
        } catch (CertificateEncodingException e) {
            logger.error("Encoding error in certificate", e);
            throw new ClientException("Encoding error in certificate", e);
        }

        try {
            // Special case for first CSCA certificate for this document type
            if (repository.countByDocumentTypeAndType(db.getDocumentType(), db.getType()) == 0) {
                cert.verify(cert.getPublicKey());
                logger.warn("Added first CSCA certificate for {}, set trusted flag manually", db.getDocumentType());
            } else {
                verify(cert, allowAddingExpired ? cert.getNotAfter() : null);
            }
        } catch (GeneralSecurityException | VerificationException e) {
            logger.error(
                String.format("Could not verify certificate of %s issued by %s",
                    cert.getSubjectX500Principal(), cert.getIssuerX500Principal()
                ), e
            );
            throw new ClientException("Could not verify certificate", e);
        }
        return repository.saveAndFlush(db);
    }


    public CRL add(X509CRL crl) {
        final CRL db = CRL.from(crl);

        final Optional<CRL> old = crlRepository.findByIssuer(db.getIssuer());
        if (old.isPresent()) {
            if (!db.getThisUpdate().isAfter(old.get().getThisUpdate())) {
                throw new ClientException("CRL is not newer, refuse to update");
            }
            db.setId(old.get().getId());
        }

        final String issuer = X509Factory.toCanonical(crl.getIssuerX500Principal());
        final Optional<Certificate> dbCert = repository.findFirstBySubject(issuer);
        if (!dbCert.isPresent()) {
            logger.error("Could not get certificate to to verify {}", crl.getIssuerX500Principal());
            throw new ClientException("Could not get certificate to verify");
        }

        try {
            final X509Certificate cert = X509Factory.toCertificate(dbCert.get().getRaw());
            crl.verify(cert.getPublicKey());
        } catch (GeneralSecurityException e) {
            logger.error("Could not verify crl of " + crl.getIssuerX500Principal(), e);
            throw new ClientException("Could not verify CRL", e);
        }
        return crlRepository.saveAndFlush(db);
    }

}
