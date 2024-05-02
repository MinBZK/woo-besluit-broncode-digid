
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

import java.util.*;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.asn1.CvCertificateRequest;
import nl.logius.digid.eid.models.asn1.PublicKeyInfo;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.repository.CertificateRepository;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;

@Service
public class CVCertificateService {
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile("[A-Z0-9]{2}[0-9]{3}$");
    private static final Pattern DL_TO_AT_PATTERN = Pattern.compile("^NLD[0-9]{3}");
    private static final Pattern NIK_TO_AT_PATTERN = Pattern.compile("^NLDV");
    private static final String DL_AT_SUBSTITUTE = "SSSSSS";
    private static final String NIK_AT_SUBSTITUTE = "SSSS";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Asn1ObjectMapper mapper;

    @Autowired
    private CertificateRepository repository;

    @Autowired
    private SignatureService signatureService;

    public List<String> getAtKeyList() {
       return signatureService.keyList();
    }

    public Certificate getAt(DocumentType documentType, PolymorphType authorization) {
        return single(repository.findFirstByDocumentTypeAndTypeAndAuthorizationOrderByNotAfterDesc(
            documentType, Certificate.Type.AT, authorization));
    }

    public Certificate getDvca(DocumentType documentType) {
        return single(repository.findFirstByDocumentTypeAndTypeAndAuthorizationOrderByNotAfterDesc(
            documentType, Certificate.Type.DVCA, null));
    }

    public Certificate getBySubject(String subject) {
        return single(repository.findFirstBySubject(subject));
    }

    public Certificate getIssuer(Certificate certificate) {
        return single(repository.findFirstBySubject(certificate.getIssuer()));
    }

    public void verify(CvCertificate cert) {
        final Deque<CvCertificate> chain = getTrustChain(cert);

        // Only CVCA has domain parameters
        final ECDomainParameters params = chain.getLast().getBody().getPublicKey().getParams();

        while (!chain.isEmpty()) {
            final CvCertificate signer = chain.pop();
            signatureService.verify(cert, signer.getBody().getPublicKey(), params);
            cert = signer;
        }
    }

    public void verifyPublicKey(CvCertificate cert) {
        final byte[] hsmPublicKey = signatureService.publicKey(cert.getBody().getChr());
        if (hsmPublicKey == null) {
            throw new ClientException("Private key of certificate is not inside hsm");
        }
        if (!Arrays.equals(hsmPublicKey, cert.getBody().getPublicKey().getKey())) {
            throw new ClientException(
                "Private key of certificate inside hsm does not correspond to public key in certificate");
        }
    }

    public Certificate add(CvCertificate cert) {
        final Certificate db = Certificate.from(cert);

        if (repository.countByIssuerAndSubject(db.getIssuer(), db.getSubject()) > 0) {
            throw new ClientException(String.format(
                "Certificate of subject %s and issuer %s already exists", db.getSubject(), db.getIssuer()));
        }
        // Special case for first CVCA certificate for this document type
        if (db.getType() == Certificate.Type.CVCA
                && repository.countByDocumentTypeAndType(db.getDocumentType(), db.getType()) == 0) {
            signatureService.verify(cert, cert.getBody().getPublicKey(), cert.getBody().getPublicKey().getParams());
            logger.warn("Added first CVCA certificate for {}, set trusted flag manually", db.getDocumentType());
        } else {
            verify(cert);
            if (db.getType() == Certificate.Type.AT) {
                verifyPublicKey(cert);
            }
        }
        return repository.saveAndFlush(db);
    }

    public byte[] generateAtRequest(DocumentType documentType, PolymorphType authorization, String sequenceNo,
                                    String reference) {
        final Certificate dvca = getDvca(documentType);
        final String subject = getAtSubject(documentType, dvca.getSubject(), sequenceNo);
        if (repository.countBySubject(subject) != 0) {
            throw new ClientException("AT certificate of " + subject + " already present");
        }

        final PublicKeyInfo keyInfo = new PublicKeyInfo();
        keyInfo.setOid(EACObjectIdentifiers.id_TA_ECDSA_SHA_384);
        keyInfo.setParams(BrainpoolP320r1.DOMAIN_PARAMS);
        keyInfo.setKey(signatureService.getOrGenerateKey(subject));

        final CvCertificate.Body body = new CvCertificate.Body();
        body.setCar(dvca.getSubject());
        body.setPublicKey(keyInfo);
        body.setChr(subject);
        if (documentType == DocumentType.DL) // use EACv2 for DL only
            body.setAuthorization(authorization);

        final CvCertificate cv = new CvCertificate();
        body.setRaw(mapper.write(body));
        cv.setBody(body);

        final EcSignature inner = new EcSignature(signatureService.sign(cv, subject, true));
        cv.setSignature(inner);

        if (reference == null) {
            return mapper.write(cv);
        }

        CvCertificateRequest req = new CvCertificateRequest();
        cv.setRaw(mapper.write(cv));
        req.setCertificate(cv);
        req.setCar(reference);

        final EcSignature outer = new EcSignature(signatureService.sign(req, reference, true));
        req.setSignature(outer);

        return mapper.write(req);
    }

    private Deque<CvCertificate> getTrustChain(CvCertificate cert) {
        final Deque<CvCertificate> chain = new LinkedList<>();
        Optional<Certificate> result = repository.findFirstBySubject(cert.getBody().getCar());
        while (result.isPresent()) {
            final CvCertificate item = mapper.read(result.get().getRaw(), CvCertificate.class);
            chain.add(item);
            if (result.get().isTrusted())
                return chain;
            if (item.getBody().getCar().equals(item.getBody().getChr())) {
                logger.error("Root certificate is not trusted: {}", cert.getBody().getChr());
                throw new ClientException("Could not find trust chain");
            }
            result = repository.findFirstBySubject(item.getBody().getCar());
        }
        logger.error("Trust chains halts for {}", cert.getBody().getChr());
        throw new ClientException("Could not find trust chain");
    }

    private static String getAtSubject(DocumentType type, String issuer, String sequenceNo) {
        if (!SEQUENCE_PATTERN.matcher(sequenceNo).matches()) {
            throw new IllegalArgumentException("Illegal sequence number " + sequenceNo);
        }

        String subject;
        switch (type) {
            case DL:
                subject = DL_TO_AT_PATTERN.matcher(issuer).replaceFirst(DL_AT_SUBSTITUTE);
                if (issuer.equals(subject)) {
                    throw new IllegalArgumentException("Unknown format RDW issuer " + issuer);
                }
                break;
            case NIK:
                subject = NIK_TO_AT_PATTERN.matcher(issuer).replaceFirst(NIK_AT_SUBSTITUTE);
                if (issuer.equals(subject)) {
                    throw new IllegalArgumentException("Unknown format RViG issuer " + issuer);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported document type " + type);
        }
        subject = SEQUENCE_PATTERN.matcher(subject).replaceFirst(sequenceNo);
        return subject;
    }

    private Certificate single(Optional<Certificate> result) {
        if (!result.isPresent()) {
            throw new ClientException("Certificate not found");
        }
        return result.get();
    }
}
