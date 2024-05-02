
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

package nl.logius.digid.eid.controller;

import nl.logius.digid.eid.Application;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.rest.digid.Confirmation;
import nl.logius.digid.eid.models.rest.digid.StubRequest;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.service.ConfirmService;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { Application.class })
@ActiveProfiles({ "default", "unit-test" })
public class StubControllerTest {
    @InjectMocks
    private StubController controller;

    @Mock
    private EidSessionRepository sessionRepo;

    @Mock
    private ConfirmService confirmService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStub() {
        EidSession session = new EidSession();
        session.setReturnUrl("http://localhost");
        session.setId("id");
        session.setConfirmId("confirmId");
        session.setConfirmSecret("secret");
        Mockito.when(sessionRepo.findById("id")).thenReturn(Optional.of(session));

        StubRequest request = new StubRequest();
        request.setDocumentType("NL-Rijbewijs");
        request.setPolymorph(Base64.decode("eHl6"));
        request.setSequenceNo("SSSSSSSSSSSSS");
        request.setSessionId("id");
        request.setStatus("success");

        controller.sendPipRequest(request);
        Mockito.verify(confirmService).sendAssertion(
            Mockito.eq("http://localhost"), Mockito.eq("confirmId"), Mockito.eq("secret"),
            Mockito.eq(PolymorphType.PIP), Mockito.eq(
                new Confirmation(Base64.decode("eHl6"),  DocumentType.DL, "SSSSSSSSSSSSS")
            )
        );
    }
}
