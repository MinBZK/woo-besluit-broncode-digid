
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
import https.digid_nl.schema.mu_status_controller.StatusType;
import nl.logius.digid.msc.exception.DocumentNotFoundException;
import nl.logius.digid.msc.model.ChangeRequest;
import nl.logius.digid.msc.model.DocumentStatus;
import nl.logius.digid.msc.model.FetchRequest;
import nl.logius.digid.msc.repository.DocumentStatusRepository;
import nl.logius.digid.sharedlib.decryptor.BsnkPseudonymDecryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({"default", "unit-test"})
public class DocumentStatusServiceTest {

    private final String pseudonym = "PSEUDONYM";
    private final String encrypted = "ENCRYPTED";
    private final String PP = "";
    private DocumentStatusService documentStatusService;

    @Mock
    private DocumentStatusRepository documentStatusRepositoryMock;

    @Mock
    private BsnkPseudonymDecryptor bsnkPseudonymDecryptorMock;

    @BeforeEach
    public void init() throws Exception {
        documentStatusService = new DocumentStatusService(documentStatusRepositoryMock, bsnkPseudonymDecryptorMock);
        ReflectionTestUtils.setField(documentStatusService, "pppKey", PP);
        ReflectionTestUtils.setField(documentStatusService, "pppKeyVersion", "1");
        ReflectionTestUtils.setField(documentStatusService, "uKey", "");
        ReflectionTestUtils.setField(documentStatusService, "uKeyVersion", "1");
    }

    @Test
    public void getInactiveStatusWithSuccessTest() {
        final DocumentStatus dummyDocumentStatus = new DocumentStatus();
        dummyDocumentStatus.setId(1L);
        dummyDocumentStatus.setSequenceNo("SSSSSSSSSSSSS");
        dummyDocumentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        dummyDocumentStatus.setStatus(StatusType.TIJDELIJK_GEBLOKKEERD);
        dummyDocumentStatus.setStatusMu(MUStatusType.ACTIEF);

        FetchRequest request = new FetchRequest();
        request.setDocType(dummyDocumentStatus.getDocType());
        request.setEpsc(encrypted);
        request.setSequenceNo(dummyDocumentStatus.getSequenceNo());

        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.of(dummyDocumentStatus));

        Status result = documentStatusService.currentStatus(request);

        assertNotNull(result);
        assertEquals(Status.INACTIVE, result);
    }

    @Test
    public void getActiveStatusWithSuccessTest() {
        final DocumentStatus dummyDocumentStatus = new DocumentStatus();
        dummyDocumentStatus.setId(1L);
        dummyDocumentStatus.setSequenceNo("SSSSSSSSSSSSS");
        dummyDocumentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        dummyDocumentStatus.setStatus(StatusType.GEACTIVEERD);
        dummyDocumentStatus.setStatusMu(MUStatusType.ACTIEF);

        FetchRequest request = new FetchRequest();
        request.setDocType(dummyDocumentStatus.getDocType());
        request.setEpsc(encrypted);
        request.setSequenceNo(dummyDocumentStatus.getSequenceNo());

        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.of(dummyDocumentStatus));

        Status result = documentStatusService.currentStatus(request);

        assertNotNull(result);
        assertEquals(Status.ACTIVE, result);
    }

    @Test
    public void getIssuedStatusWithSuccesTest() throws Exception {
        final DocumentStatus dummyDocumentStatus = new DocumentStatus();
        dummyDocumentStatus.setId(1L);
        dummyDocumentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        dummyDocumentStatus.setPseudonym(pseudonym);
        dummyDocumentStatus.setSequenceNo("SSSSSSSSSSSSS");
        dummyDocumentStatus.setStatus(StatusType.UITGEREIKT);
        dummyDocumentStatus.setStatusMu(MUStatusType.ACTIEF);

        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.of(dummyDocumentStatus));

        FetchRequest request = new FetchRequest();
        request.setDocType(dummyDocumentStatus.getDocType());
        request.setEpsc(encrypted);
        request.setSequenceNo(dummyDocumentStatus.getSequenceNo());

        Status status = documentStatusService.currentStatus(request);

        assertEquals(Status.ISSUED, status);
    }

    @Test
    public void getBlockedStatusWithSuccesTest() throws Exception {
        final DocumentStatus dummyDocumentStatus = new DocumentStatus();
        dummyDocumentStatus.setId(1L);
        dummyDocumentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        dummyDocumentStatus.setPseudonym(pseudonym);
        dummyDocumentStatus.setSequenceNo("SSSSSSSSSSSSS");
        dummyDocumentStatus.setStatus(StatusType.GEBLOKKEERD);
        dummyDocumentStatus.setStatusMu(MUStatusType.ACTIEF);

        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.of(dummyDocumentStatus));

        FetchRequest request = new FetchRequest();
        request.setDocType(dummyDocumentStatus.getDocType());
        request.setEpsc(encrypted);
        request.setSequenceNo(dummyDocumentStatus.getSequenceNo());

        Status status = documentStatusService.currentStatus(request);

        assertEquals(Status.BLOCKED, status);
    }

    @Test
    public void getDocumentStatusNotFoundExceptionTest() throws Exception {
        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.empty());

        FetchRequest request = new FetchRequest();
        request.setEpsc(encrypted);

        assertThrows(DocumentNotFoundException.class, () -> {
            documentStatusService.currentStatus(request);
        });
    }

    @Test
    public void setEptl() throws Exception {
        final DocumentStatus dummyDocumentStatus = new DocumentStatus();
        dummyDocumentStatus.setId(1L);
        dummyDocumentStatus.setDocType(DocTypeType.NL_RIJBEWIJS);
        dummyDocumentStatus.setPseudonym(pseudonym);
        dummyDocumentStatus.setSequenceNo("SSSSSSSSSSSSS");
        dummyDocumentStatus.setStatus(StatusType.GEACTIVEERD);
        dummyDocumentStatus.setStatusMu(MUStatusType.ACTIEF);

        when(bsnkPseudonymDecryptorMock.decryptEp(anyString(), anyString(), anyString())).thenReturn(pseudonym);
        when(documentStatusRepositoryMock.findByPseudonymAndDocTypeAndSequenceNo(anyString(), any(DocTypeType.class), anyString())).thenReturn(Optional.of(dummyDocumentStatus));

        final String eptl = "TL-ENCRYPTED";
        final ChangeRequest request = new ChangeRequest();
        request.setDocType(dummyDocumentStatus.getDocType());
        request.setEpsc(encrypted);
        request.setSequenceNo(dummyDocumentStatus.getSequenceNo());
        request.setEptl(eptl);

        documentStatusService.setEptl(request);

        ArgumentCaptor<DocumentStatus> argument = ArgumentCaptor.forClass(DocumentStatus.class);
        Mockito.verify(documentStatusRepositoryMock).save(argument.capture());

        assertEquals(eptl, argument.getValue().getEptl());
    }
}
