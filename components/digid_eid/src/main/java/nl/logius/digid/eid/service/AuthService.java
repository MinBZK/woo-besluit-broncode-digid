
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
import nl.logius.digid.card.asn1.models.CardPolymorph;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.models.rest.digid.Confirmation;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.security.SecurityFactory;
import nl.logius.digid.eid.util.GetApduResponseFunction;
import nl.logius.digid.eid.validations.CardValidations;
import nl.logius.digid.eid.validations.IpValidations;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.ResponseAPDU;
import java.util.Optional;

public abstract class AuthService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final BsnkService bsnkService;
    private final IpValidations ipValidations;
    private final ConfirmService confirmService;
    protected final Asn1ObjectMapper mapper;
    protected final CmsVerifier cmsVerifier;
    protected final CVCertificateService cvCertificateService;
    protected final SignatureService signatureService;
    protected final SecurityFactory securityFactory;
    protected final EidSessionRepository sessionRepo;

    @SuppressWarnings("squid:S00107") // Maximum parameters should be ignored for autowired constructors
    public AuthService(BsnkService bsnkService,
                       IpValidations ipValidations,
                       ConfirmService confirmService,
                       Asn1ObjectMapper mapper,
                       CmsVerifier cmsVerifier,
                       CVCertificateService cvCertificateService,
                       SignatureService signatureService,
                       SecurityFactory securityFactory,
                       EidSessionRepository sessionRepo) {
        this.bsnkService = bsnkService;
        this.ipValidations = ipValidations;
        this.confirmService = confirmService;
        this.mapper = mapper;
        this.cmsVerifier = cmsVerifier;
        this.cvCertificateService = cvCertificateService;
        this.signatureService = signatureService;
        this.securityFactory = securityFactory;
        this.sessionRepo = sessionRepo;
    }

    public GetCertificateResponse getCertificateRestService(GetCertificateRequest request, String clientIp) {
        GetCertificateResponse response = new GetCertificateResponse();
        EidSession session = initSession(request, clientIp, response);
        if (session == null) return response;

        session.setUserConsentType(request.getUserConsentType());

        if (getDocumentType() == DocumentType.DL) {
            CardValidations.validateRdwAid(request.getDocumentType());

            // Set PP or PIP
            Certificate at = cvCertificateService.getAt(getDocumentType(), request.getUserConsentType());
            session.setAtReference(at.getSubject());
            response.setAtCert(CvCertificate.getAsSequence(at.getRaw()));
        } else if (getDocumentType() == DocumentType.NIK) {
            // TODO: Check aID of NIK
        }

        // Result OK
        sessionRepo.save(session);
        return response;
    }

    public PolyDataResponse getPolymorphicDataRestService(AppRequest request, PolyDataResponse response, EidSession session, GetApduResponseFunction apduFunc) {
        try {
            // decrypt and verify the response data from step4 (the apdus)
            // verify all the APDU's and if successful get the responseApdu of the last apdu
            // (this contains the pip/pp)
            final ResponseAPDU finalApduResponse;
            try {
                finalApduResponse = apduFunc.getApduResponse(request);
            } catch (ClientException e) {
                logger.error("One of the APDU responses went bad: {}", e);
                throw e;
            }

            logger.info("{}: {}", session.getUserConsentType(), Base64.toBase64String(finalApduResponse.getData()));
            final CardPolymorph card = mapper.read(finalApduResponse.getData(), CardPolymorph.class);
            logger.info("SequenceNo: {}", card.getSequenceNo());

            final Confirmation confirm = new Confirmation(
                bsnkService.convertCardToUsvE(card), getDocumentType(), card.getSequenceNo()
            );
            confirmService.sendAssertion(session.getReturnUrl(), session.getConfirmId(), session.getConfirmSecret(),
                session.getUserConsentType(), confirm);

            response.setResult("OK");

            // Result OK
            return response;
        } finally {
            // Last REST service, clean up redis cache.
            sessionRepo.delete(session);
        }
    }

    public EidSession initSession(AppRequest request, String clientIp, AppResponse response) {
        final Optional<EidSession> result = sessionRepo.findById(request.getHeader().getSessionId());
        if (result.isPresent()) {
            response.setSessionId(result.get().getId());
            if (clientIp != null) {
                verifyClient(result.get(), clientIp);
            }
            return result.get();
        } else {
            response.setStatus("CANCELLED");
            return null;
        }
    }

    protected void verifyClient(EidSession session, String clientIp) {
        try {
            ipValidations.ipCheck(session, clientIp);
        } catch (ClientException e) {
            confirmService.sendError(session.getReturnUrl(), session.getId(), session.getConfirmSecret(),
                "desktop_clients_ip_address_not_equal");
            throw e;
        }
    }

    protected abstract DocumentType getDocumentType();
}
