
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

package controller;

import nl.logius.digid.ns.controller.MessageController;
import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static nl.logius.digid.ns.model.MessageType.PSH01;
import static nl.logius.digid.ns.model.MessageType.PSH02;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageControllerTest {
    @Mock
    private MessageRepository messageRepositoryMock;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void updateMessageTest() {
        Message message = new Message();
        message.setId(1L);
        message.setMessageType(PSH01);
        message.setTitleEnglish("title EN 1");
        message.setContentEnglish("content EN 1");
        message.setTitleDutch("title NL 1");
        message.setContentDutch("content NL 1");

        Map<String, String> changes = new HashMap<>();
        changes.put("message_type", "PSH02");
        changes.put("title_english", "title EN 2");
        changes.put("content_english", "content EN 2");
        changes.put("title_dutch", "title NL 2");
        changes.put("content_dutch", "content NL 2");

        when(messageRepositoryMock.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepositoryMock.save(any(Message.class))).thenReturn(message);

        messageController.update(1L, changes);

        verify(messageRepositoryMock, Mockito.times(1)).findById(1L);
        verify(messageRepositoryMock, Mockito.times(1)).save(any(Message.class));
        assertEquals(PSH02, message.getMessageType());
        assertEquals("title EN 2", message.getTitleEnglish());
        assertEquals("content EN 2", message.getContentEnglish());
        assertEquals("title NL 2", message.getTitleDutch());
        assertEquals("content NL 2", message.getContentDutch());
    }
}
