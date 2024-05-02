
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

package nl.logius.digid.ns.service;

import nl.logius.digid.ns.model.Message;
import nl.logius.digid.ns.model.MessageType;
import nl.logius.digid.ns.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MessageService extends DatabaseService<Message> implements ApplicationListener<ContextRefreshedEvent> {
    private final static String OBJECT_TYPE = "message";
    private final static String FILE_LOCATION = "data/messages.yml";
    private final static Class MAPPING_CLASS = Message.class;

    public Optional<Message> retrieveMessageByType(MessageType type){
        return ((MessageRepository) repository).findByMessageType(type);
    }

    @Autowired
    public MessageService(MessageRepository repository) {
        super(repository);
    }

    @Override
    protected Map<String, String> compareObjects(Message oldMsg, Message updatedMsg) {
        final Map<String, String> changes = new HashMap<>();

        if (!oldMsg.getMessageType().equals(updatedMsg.getMessageType())) {
            oldMsg.setMessageType(updatedMsg.getMessageType());
            changes.put("message_type", oldMsg.getMessageType().toString());
        }
        if (!oldMsg.getTitleEnglish().equals(updatedMsg.getTitleEnglish())) {
            oldMsg.setTitleEnglish(updatedMsg.getTitleEnglish());
            changes.put("title_english", oldMsg.getTitleEnglish());
        }
        if (!oldMsg.getContentEnglish().equals(updatedMsg.getContentEnglish())) {
            oldMsg.setContentEnglish(updatedMsg.getContentEnglish());
            changes.put("content_english", oldMsg.getContentEnglish());
        }
        if (!oldMsg.getTitleDutch().equals(updatedMsg.getTitleDutch())) {
            oldMsg.setTitleDutch(updatedMsg.getTitleDutch());
            changes.put("title_dutch", oldMsg.getTitleDutch());
        }
        if (!oldMsg.getContentDutch().equals(updatedMsg.getContentDutch())) {
            oldMsg.setContentDutch(updatedMsg.getContentDutch());
            changes.put("content_dutch", oldMsg.getContentDutch());
        }

        if (!changes.isEmpty()) {
            oldMsg.setDates();
        }

        return changes;
    }

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }

    @Override
    public String getFileLocation() {
        return FILE_LOCATION;
    }

    @Override
    public Class getMappingClass() {
        return MAPPING_CLASS;
    }
}
