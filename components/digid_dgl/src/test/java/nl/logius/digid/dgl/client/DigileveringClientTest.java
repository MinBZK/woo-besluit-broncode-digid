
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

package nl.logius.digid.dgl.client;

import nl.logius.digid.dgl.api.DigileveringSender;
import nl.logius.digid.dgl.controller.AfnemersberichtAanDGLFactory;
import nl.logius.digid.dgl.util.TestDglMessagesUtil;
import nl.logius.digid.digilevering.api.model.AfnemersberichtAanDGL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.IllegalStateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DigileveringClientTest {

    @Mock
    private DigileveringSender digileveringSender;

    @Mock
    private DigidXClient digidXClient;

    @InjectMocks
    private DigileveringClient classUnderTest;

    @Test
    public void testSendAp01Correct(){
        AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory = new AfnemersberichtAanDGLFactory("oin1", "oin2");
        AfnemersberichtAanDGL message = afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(TestDglMessagesUtil.createTestAp01("bsn"));
        classUnderTest.sendRequest(message);

        verify(digileveringSender, times(1)).sendMessage(any(), any());
        verify(digidXClient, times(1)).remoteLogBericht(Log.SEND_SUCCESS, message);

    }

    @Test
    public void testSendAv01Correct(){
        AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory = new AfnemersberichtAanDGLFactory("oin1", "oin2");
        AfnemersberichtAanDGL message = afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(TestDglMessagesUtil.createTestAv01("aNummer"));

        classUnderTest.sendRequest(message);

        verify(digileveringSender, times(1)).sendMessage(any(), any());
        verify(digidXClient, times(1)).remoteLogWithoutRelatingToAccount(Log.SEND_SUCCESS, "Av01");

    }

    @Test
    public void testSendFailed(){
        AfnemersberichtAanDGL message = new AfnemersberichtAanDGL();

        doThrow(IllegalStateException.class)
                .when(digileveringSender)
                .sendMessage(any(), any());

        classUnderTest.sendRequest(message);

        verify(digileveringSender, times(1)).sendMessage(any(), any());
        verify(digidXClient, times(1)).remoteLogBericht(Log.SEND_FAILURE, message);
    }

}
