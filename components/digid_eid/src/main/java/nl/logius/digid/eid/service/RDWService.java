
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

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.card.crypto.EcPrivateKey;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.asn1.PcaSecurityInfos;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.security.SecurityFactory;
import nl.logius.digid.eid.util.GetApduResponseFunction;
import nl.logius.digid.eid.util.KeyUtils;
import nl.logius.digid.eid.validations.CardValidations;
import nl.logius.digid.eid.validations.IpValidations;
import nl.logius.digid.sharedlib.model.ByteArray;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class RDWService extends AuthService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final DocumentType DOCUMENT_TYPE = DocumentType.DL;

    @Autowired
    @SuppressWarnings("squid:S00107") // Maximum parameters should be ignored for autowired constructors
    public RDWService(BsnkService bsnkService,
                      IpValidations ipValidations,
                      ConfirmService confirmService,
                      Asn1ObjectMapper mapper,
                      CmsVerifier cmsVerifier,
                      CVCertificateService cvCertificateService,
                      SignatureService signatureService,
                      SecurityFactory securityFactory,
                      EidSessionRepository sessionRepo) {
        super(bsnkService,
            ipValidations,
            confirmService,
            mapper,
            cmsVerifier,
            cvCertificateService,
            signatureService,
            securityFactory,
            sessionRepo);
    }

    public PolyInfoResponse validatePolymorphInfoRestService(PolyInfoRequest request, String clientIp) {
        PolyInfoResponse response = new PolyInfoResponse();
        EidSession session = initSession(request, clientIp, response);
        if (session == null) return response;

        PcaSecurityInfos cardAccess = mapper.read(request.getEfCardAccess(), PcaSecurityInfos.class);

        // 1. Validate the EFCardAccess file.
        // In EF.cardaccess moet bij polymorphic info versie 1 staan.
        // (ASN1 parsing: pcaVersion=1, flag randomizedPIP aan, flag compressedEncoding aan)
        CardValidations.validatePolymorhpicInfo(cardAccess.getPolymorphicInfo());

        // 2. The CARs received match with the CVCA under which eID-Server terminal
        // certificates reside. (see RDW specifications)
        Certificate at = cvCertificateService.getBySubject(session.getAtReference());
        Certificate dvca = cvCertificateService.getIssuer(at);

        if (!dvca.getIssuer().equals(new String(request.getCar(), StandardCharsets.UTF_8))) {
            // TODO: Send link CVCA
            logger.error("Could not find chain with CVCA {}", request.getCar());
            throw new ClientException("Could not find CVCA");
        }

        // 3. Select an appropriate TA certificate chain for the polymorphic
        // authentication the user has consented to,
        // --> DVCA cert + CVCA Public Key + Terminal Certificate
        response.setDvCert(CvCertificate.getAsSequence(dvca.getRaw()));

        // 4. Generates a random ephemeral key K in participation of the coming CA
        // protocol.
        EcPrivateKey ephemeralKey = securityFactory.generateKey(cardAccess.getCaEcParameters().getDomainParameters());
        response.setEphemeralKey(KeyUtils.getEncodedPublicPoint(ephemeralKey.toPublicKeySpec()));

        // set session data that is used in later steps
        session.setIdpicc(new ByteArray(request.getIdpicc()));
        session.setEphemeralKey(ephemeralKey);
        session.setKeyReference(cardAccess.getCaKeyId());
        session.setTaVersion(cardAccess.getTaVersion());
        session.setPaceVersion(cardAccess.getPaceVersion());

        // Result OK
        sessionRepo.save(session);
        return response;
    }

    public SignatureResponse getDigitalSignatureRestService(SignatureRequest request, String clientIp) {
        SignatureResponse response = new SignatureResponse();
        EidSession session = initSession(request, clientIp, response);
        if (session == null) return response;

        // 1. create the digital signature to send back
        // use the private key to create the signature made out of the challenge, the
        // iccpace ephemeral public key and the ephemeral we created in the polymorphic
        // info
        byte[] toSign = KeyUtils.calcDataToSign(request.getChallenge(), session.getEphemeralKey().getQ(),
            session.getIdpicc().data);
        byte[] signature = signatureService.sign(toSign, session.getAtReference(), false);
        response.setSignature(signature);

        // Result OK
        return response;
    }

    public SecApduResponse generateSecureAPDUsRestService(SecApduRequest request, String clientIp) {
        SecApduResponse response = new SecApduResponse();
        EidSession session = initSession(request, clientIp, response);
        if (session == null) return response;

        // 1a. check that the content of ef.cardsecurity is authentic with the PA
        // (passive authentication) is correct...
        final PcaSecurityInfos efCardSecurity = mapper.read(cmsVerifier.verifyMessage(
            ContentInfo.getInstance(request.getEfCardSecurity()), "0.4.0.127.0.7.3.2.1"), PcaSecurityInfos.class
        );

        CardValidations.validateCardSecurityVsCardAccess(efCardSecurity, session.getKeyReference(),
            session.getPaceVersion(), session.getTaVersion());

        // 2. generate 2 key pairs (known as Kenc(message encryption) and Kmac (message
        // authentication) based on the public key created in step1 (on session)
        // the nonce(rpicc) we received and the ca public key we got received
        // calculate the secret key
        byte[] rpicc = request.getRpicc();
        KeyUtils.generateSecretKeys(session, efCardSecurity.getEcPublicKey().getPublicParameters(), rpicc);

        // do the tpicc check with the tpicc from the request and the terminal token
        CardValidations.validateTerminalTokenVsTpicc(session, request.getTpicc());

        // PMA=polymorphic authenticate, GA= general authenticate
        // we need 3 apdus
        response.setApdus(new ApduService(session).createSecureApdusForRDW(
            session.getUserConsentType().getTag80(), request.getPcaApplicationId())
        );

        // Result OK
        sessionRepo.save(session);
        return response;
    }

    public PolyDataResponse getPolymorphicDataRestService(PolyDataRequest request, String clientIp) {
        PolyDataResponse response = new PolyDataResponse();
        EidSession session = initSession(request, clientIp, response);
        if (session == null) return response;
        GetApduResponseFunction<PolyDataRequest> func = (PolyDataRequest r) -> new ApduService(session).verify(r.getApduResponses());

        return super.getPolymorphicDataRestService(request, response, session, func);
    }

    @Override
    protected DocumentType getDocumentType() {
        return DOCUMENT_TYPE;
    }
}
