
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

package nl.logius.digid.digilevering.lib.connect;

import nl.logius.digid.digilevering.api.model.ObjectFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

public abstract class Receiver<M> {

    private Class<?> messageClass;

    public Receiver(M instance) {
        messageClass = instance.getClass();
    }

    @SuppressWarnings("unchecked")
    @JmsListener(destination = "${digilevering.topic.in}", containerFactory = "jmsListenerContainerFactory")
    public void receiveMessage(@Payload String message, MessageHeaders headers) {
        try {
            JAXBContext context = JAXBContext.newInstance(messageClass, ObjectFactory.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object unmarshalledObject = unmarshaller.unmarshal(new StreamSource(new StringReader(message)));
            if (messageClass.isInstance(unmarshalledObject)) {
                M unmarshalledMessage = (M) unmarshalledObject;
                processMessage(unmarshalledMessage, headers);
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    protected abstract void processMessage(M message, MessageHeaders headers);

}
