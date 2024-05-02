
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

package nl.logius.digid.ns.model;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.ZonedDateTime;
import java.util.Map;

@Entity
@Table(name = "messages")
public class Message extends SQLObject {

    @Enumerated(EnumType.STRING)
    private MessageType messageType;
    private String titleEnglish;
    private String contentEnglish;
    private String titleDutch;
    private String contentDutch;

    public Message() {}

    public Message(Long id, MessageType messageType, String titleEnglish, String contentEnglish, String titleDutch, String contentDutch, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.messageType = messageType;
        this.titleEnglish = titleEnglish;
        this.contentEnglish = contentEnglish;
        this.titleDutch = titleDutch;
        this.contentDutch = contentDutch;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public void setTitleEnglish(String title) {
        this.titleEnglish = title;
    }

    public String getContentEnglish() {
        return contentEnglish;
    }

    public void setContentEnglish(String content) {
        this.contentEnglish = content;
    }

    public String getTitleDutch() {
        return titleDutch;
    }

    public void setTitleDutch(String titleDutch) {
        this.titleDutch = titleDutch;
    }

    public String getContentDutch() {
        return contentDutch;
    }

    public void setContentDutch(String contentDutch) {
        this.contentDutch = contentDutch;
    }

    public void updateMap(Map<String, String> valuesMap) {
        if (valuesMap.containsKey("message_type")) {
            messageType = MessageType.valueOf(valuesMap.get("message_type"));
        }
        if (valuesMap.containsKey("title_english")) {
            titleEnglish = valuesMap.get("title_english");
        }
        if (valuesMap.containsKey("content_english")) {
            contentEnglish = valuesMap.get("content_english");
        }
        if (valuesMap.containsKey("title_dutch")) {
            titleDutch = valuesMap.get("title_dutch");
        }
        if (valuesMap.containsKey("content_dutch")) {
            contentDutch = valuesMap.get("content_dutch");
        }

        setDates();
    }
}
