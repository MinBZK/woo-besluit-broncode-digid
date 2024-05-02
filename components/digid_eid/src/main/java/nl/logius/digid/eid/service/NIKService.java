
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

import com.google.common.collect.ImmutableList;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.DataGroup14;
import nl.logius.digid.card.asn1.models.LdsSecurityObject;
import nl.logius.digid.card.asn1.models.SOd;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.card.crypto.EcPrivateKey;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.security.SecurityFactory;
import nl.logius.digid.eid.util.GetApduResponseFunction;
import nl.logius.digid.eid.util.KeyUtils;
import nl.logius.digid.eid.validations.IpValidations;
import nl.logius.digid.sharedlib.model.ByteArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class NIKService extends AuthService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final DocumentType DOCUMENT_TYPE = DocumentType.NIK;

    @Autowired
    @SuppressWarnings("squid:S00107") // Maximum parameters should be ignored for autowired constructors
    public NIKService(BsnkService bsnkService,
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

    public PrepareEacResponse prepareEacRequestRestService(PrepareEacRequest request) {
        PrepareEacResponse response = new PrepareEacResponse();
        EidSession session = initSession(request, null, response);
        if (session == null) return response;

        // 1.8 PA
        SOd sod = mapper.read(request.getEfSOd(), SOd.class);
        LdsSecurityObject ldsSecurityObject = sod.toLdsSecurityObject(mapper, cmsVerifier);
        ldsSecurityObject.verify(14, request.getDg14());
        DataGroup14 dg14 = mapper.read(request.getDg14(), DataGroup14.class);

        // 1.11 generate PKca.pcd / SKca.pcd1 2 840 10045 2 1
        EcPrivateKey ephemeralKey = securityFactory.generateKey(
            dg14.getSecurityInfos().getEcPublicKey().getParameters().getDomainParameters()
        );
        session.setIdpicc(new ByteArray(request.getPaceIcc()));
        session.setEphemeralKey(ephemeralKey);
        response.setEphemeralPKey(KeyUtils.getEncodedPublicPoint(ephemeralKey.toPublicKeySpec()));

        KeyUtils.generateSecretKeys(session, dg14.getSecurityInfos().getEcPublicKey().getPublicParameters(), null);
        Certificate at = cvCertificateService.getAt(DocumentType.NIK, session.getUserConsentType());
        session.setAtReference(at.getSubject());
        Certificate dvca = cvCertificateService.getIssuer(at);

        byte[] efCvca = request.getEfCvca();
        String cvcaCar = new String(efCvca, 2, efCvca[1], StandardCharsets.US_ASCII);

        List<Certificate> certificates = new ArrayList<>();
        Certificate cvca = cvCertificateService.getIssuer(dvca);

        if (!cvca.getSubject().equals(cvcaCar) && !cvca.isTrusted()) {
            var chainSize = 0;
            do {
              certificates.add(0, cvca);
              logger.warn("Certificate: added {} as link certificate in chain", cvca.getSubject());
              cvca = cvCertificateService.getIssuer(cvca);
              chainSize++;
            } while(!cvca.isTrusted() && !cvca.getSubject().equals(cvcaCar) && chainSize < 5);
        }

        certificates.add(dvca);
        certificates.add(at);

        response.setApdus(new ApduService(session).createPrepareEacNIKApdus(certificates, at.getSubject()));

        sessionRepo.save(session);
        return response;
    }

    public PreparePcaResponse preparePcaRequestRestService(NikApduResponsesRequest request) {
        PreparePcaResponse response = new PreparePcaResponse();
        EidSession session = initSession(request, null, response);
        if (session == null) return response;

        // 1.28 validate
        ApduService apduService = new ApduService(session, request.getCounter());
        ResponseAPDU responseAPDU = apduService.verify(request.getApdu());
        byte[] challenge = responseAPDU.getData();

        // 1.29 signData
        byte[] toSign = KeyUtils.calcDataToSign(challenge, session.getEphemeralKey().getQ(), session.getIdpicc().data);
        byte[] signature = signatureService.sign(toSign, session.getAtReference(), true);

        // 1.30 APDU > external authenticate
        ImmutableList.Builder<CommandAPDU> chain = ImmutableList.builder();
        chain.add(apduService.getExternalAuthenticate(signature));
        chain.addAll(apduService.createSecureNikPcaApdus(session.getUserConsentType().getTag80()));
        response.setApdus(chain.build());

        sessionRepo.save(session);
        return response;
    }

    public PolyDataResponse getPolymorphicDataRestService(NikApduResponsesRequest request) {
        PolyDataResponse response = new PolyDataResponse();
        EidSession session = initSession(request, null, response);
        if (session == null) return response;
        GetApduResponseFunction<NikApduResponsesRequest> func = (NikApduResponsesRequest r) -> new ApduService(session, request.getCounter()).verify(request.getApdu());

        return super.getPolymorphicDataRestService(request, response, session, func);
    }

    @Override
    protected DocumentType getDocumentType() {
        return DOCUMENT_TYPE;
    }
}
