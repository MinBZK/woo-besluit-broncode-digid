
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

package service;

import nl.logius.digid.ns.Application;
import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.model.MessageType;
import nl.logius.digid.ns.repository.MessageRepository;
import nl.logius.digid.ns.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class MessageServiceTest {
    @Mock
    private MessageRepository messageRepositoryMock;

    @Autowired
    private MessageRepository messageRepository;

    @InjectMocks
    @Spy
    private MessageService messageService;

    private ZonedDateTime dateTime = ZonedDateTime.of(2021, 01, 01, 0, 0, 0, 0, ZoneId.of("UTC"));

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void addMessagesTest() {
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        List<Message> messagesDB = generateMessages();
        // Remove so DB and yaml file are not identical
        messagesDB.remove(2);

        when(messageRepositoryMock.findAll()).thenReturn(messagesDB);

        messageService.onApplicationEvent(null);

        verify(messageRepositoryMock, Mockito.times(1)).save(any(Message.class));
        verify(messageRepositoryMock).save(argument.capture());
        assertEquals(Long.valueOf(3L), argument.getValue().getId());
    }

    @Test
    public void removeMessagesTest() {
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        List<Message> messagesDB = generateMessages();

        Message message = new Message();
        message.setId(4L);
        message.setMessageType(MessageType.PSH04);
        message.setTitleEnglish("title EN 4");
        message.setContentEnglish("content EN 4");
        message.setTitleDutch("title NL 4");
        message.setContentDutch("content NL 4");
        messagesDB.add(message);

        when(messageRepositoryMock.findAll()).thenReturn(messagesDB);

        messageService.onApplicationEvent(null);

        verify(messageRepositoryMock, Mockito.times(1)).delete(any(Message.class));
        verify(messageRepositoryMock).delete(argument.capture());
        assertEquals(Long.valueOf(4L), argument.getValue().getId());
    }

    @Test
    public void updateMessagesTest() {
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        List<Message> messagesDB = generateMessages();

        Message message = messagesDB.get(0);
        message.setTitleEnglish("Old title");

        when(messageRepositoryMock.findAll()).thenReturn(messagesDB);

        messageService.onApplicationEvent(null);

        verify(messageRepositoryMock, Mockito.times(1)).save(any(Message.class));
        verify(messageRepositoryMock).save(argument.capture());
        assertEquals("title EN 1", argument.getValue().getTitleEnglish());
        assertNotEquals(dateTime, argument.getValue().getUpdatedAt());
    }

    @Test
    @Transactional
    public void expectRollbackWhenYamlInvalidDataTest() throws NoSuchFieldException, IllegalAccessException {
        Field repositoryField = MessageService.class.getSuperclass().getDeclaredField("repository");
        repositoryField.setAccessible(true);
        repositoryField.set(messageService, messageRepository);

        doReturn("data/messages-invalid.yml").when(messageService).getFileLocation();

        // Inside transaction so data will be modified -> see expectDataToBeRolledBack() for post transaction
        messageService.onApplicationEvent(null);
        assertEquals(1, messageRepository.findAll().size());
    }

    @AfterTransaction
    public void expectDataToBeRolledBack() {
        assertEquals(3, messageRepository.findAll().size());
    }

    @Test
    public void doNothingIfNoMessagesChangedTest() {
        List<Message> messagesDB = generateMessages();

        when(messageRepositoryMock.findAll()).thenReturn(messagesDB);

        messageService.onApplicationEvent(null);

        verify(messageRepositoryMock, Mockito.times(0)).save(any(Message.class));
        verify(messageRepositoryMock, Mockito.times(0)).delete(any(Message.class));
        assertEquals(dateTime, messagesDB.get(0).getUpdatedAt());
    }

    private List<Message> generateMessages() {
        List<Message> objs = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Message message = new Message();
            message.setId(Long.valueOf(i));
            message.setMessageType(MessageType.valueOf("PSH0" + i));
            message.setTitleEnglish("title EN " + i);
            message.setContentEnglish("content EN " + i);
            message.setTitleDutch("title NL " + i);
            message.setContentDutch("content NL " + i);
            message.setCreatedAt(dateTime);
            message.setUpdatedAt(dateTime);

            objs.add(message);
        }

        return objs;
    }
}
