
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

package nl.logius.digid.msc.service;

import https.digid_nl.schema.mu_status_controller.DocTypeType;
import https.digid_nl.schema.mu_status_controller.MUStatusType;
import https.digid_nl.schema.mu_status_controller.RegisterStatusSCRequest;
import https.digid_nl.schema.mu_status_controller.StatusType;
import nl.logius.digid.msc.exception.DocumentNotFoundException;
import nl.logius.digid.msc.model.ChangeRequest;
import nl.logius.digid.msc.model.DocumentStatus;
import nl.logius.digid.msc.model.FetchRequest;
import nl.logius.digid.msc.repository.DocumentStatusRepository;
import nl.logius.digid.sharedlib.decryptor.BsnkPseudonymDecryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.Optional;

@Service
public class DocumentStatusService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DocumentStatusRepository repository;
    private final BsnkPseudonymDecryptor bsnkPseudonymDecryptor;

    @Value("${pp-key.U}")
    private String uKey;

    @Value("${pp-key.U_ksv}")
    private String uKeyVersion;

    @Value("${pp-key.PP_P}")
    private String pppKey;

    @Value("${pp-key.PP_P_ksv}")
    private String pppKeyVersion;

    @Autowired
    public DocumentStatusService(DocumentStatusRepository repository, BsnkPseudonymDecryptor bsnkPseudonymDecryptor) {
        this.repository = repository;
        this.bsnkPseudonymDecryptor = bsnkPseudonymDecryptor;
    }

    public void updateStatus(RegisterStatusSCRequest request) throws Exception {
        final String pseudonym = decryptToPseudonymByBSNK(request);
        final DocumentStatus document = initializeDocument(pseudonym, request.getDocType(), request.getSequenceNr());

        if (request.getStatus() != StatusType.UITGEREIKT && document.getStatus() == null)
            throw new DocumentNotFoundException("Record not found or status not UITGEREIKT");

        document.setStateSource(request.getStateSource());
        document.setStatus(request.getStatus());
        document.setStatusDatetime(new Timestamp(request.getStatusDateTime().toGregorianCalendar().getTimeInMillis()));
        if (request.getMU() != null) {
            document.setStatusMu(request.getMU().getStatusMU());
            document.setStatusMuDatetime(
                new Timestamp(request.getMU().getStatusDateTimeMU().toGregorianCalendar().getTimeInMillis()));
        }
        repository.save(document);
    }

    public Status currentStatus(FetchRequest request) {
        final DocumentStatus ds = fetchStatus(request);

        if (MUStatusType.ACTIEF == ds.getStatusMu() || ds.getDocType() == DocTypeType.NI) {
            switch (ds.getStatus()) {
                case GEACTIVEERD:
                    return Status.ACTIVE;
                case UITGEREIKT:
                    return Status.ISSUED;
                case GEBLOKKEERD:
                    return Status.BLOCKED;
                default:
                    break;
            }
        }
        return Status.INACTIVE;
    }

    public void setEptl(ChangeRequest request) {
        final DocumentStatus ds = fetchStatus(request);
        ds.setEptl(request.getEptl());
        repository.save(ds);
    }

    private DocumentStatus fetchStatus(FetchRequest request) {
        final String pseudonym = bsnkPseudonymDecryptor.decryptEp(request.getEpsc(), pppKeyVersion, pppKey);
        final Optional<DocumentStatus> result = repository
            .findByPseudonymAndDocTypeAndSequenceNo(pseudonym, request.getDocType(), request.getSequenceNo());

        if (!result.isPresent()) {
            logger.info("Status <{}, {}, {}> NOT FOUND", pseudonym, request.getDocType(), request.getSequenceNo());
            throw new DocumentNotFoundException("No record found");
        }
        final DocumentStatus ds = result.get();
        logger.info("Status <{}, {}, {}> = <{}, {}>", pseudonym, request.getDocType(), request.getSequenceNo(), ds.getStatus(), ds.getStatusMu());
        return ds;
    }

    public String decryptToPseudonymByBSNK(RegisterStatusSCRequest request) {
        if (request.getEncryptedPseudonym() != null) {
            return bsnkPseudonymDecryptor.decryptEp(Base64.getEncoder().encodeToString(request.getEncryptedPseudonym()), pppKeyVersion, pppKey);
        } else {
            return bsnkPseudonymDecryptor.decryptDep(Base64.getEncoder().encodeToString(request.getDirectEncryptedPseudonymDEPSc()), uKeyVersion, uKey, request.getRequester());
        }
    }

    private DocumentStatus initializeDocument(String pseudonym, DocTypeType docType, String sequenceNo) {
        final Optional<DocumentStatus> result = repository.findByPseudonymAndDocTypeAndSequenceNo(pseudonym, docType, sequenceNo);
        if (result.isPresent())
            return result.get();
        final DocumentStatus document = new DocumentStatus();
        document.setPseudonym(pseudonym);
        document.setDocType(docType);
        document.setSequenceNo(sequenceNo);
        return document;
    }
}
