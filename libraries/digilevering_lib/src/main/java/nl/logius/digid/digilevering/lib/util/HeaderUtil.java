
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

package nl.logius.digid.digilevering.lib.util;

import nl.logius.digid.digilevering.lib.model.Headers;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeaderUtil {

    public static MessageHeaders createVerstrekkingAanAfnemerHeaders(Map<String, Object> additionalHeaders) {
        validateHeaders(additionalHeaders);
        Map<String, Object> headersMap = createBasicHeaderMap();

        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_ACTION, "verstrekkingAanAfnemer");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_ACTIVITY, "dgl:ontvangen:1.0");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROCESS_TYPE, "dgl:ontvangen:1.0");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROCESS_VERSION, "1.0");
        headersMap.putAll(additionalHeaders);

        MessageHeaders headers = new MessageHeaders(headersMap);
        return headers;
    }

    public static MessageHeaders createAfnemersberichtAanDGLHeaders(Map<String, Object> additionalHeaders) {
        validateHeaders(additionalHeaders);
        Map<String, Object> headersMap = createBasicHeaderMap();

        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_ACTION, "BRPAfnemersberichtAanDGL");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_ACTIVITY, "dgl:objecten:1.0");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROCESS_TYPE, "dgl:objecten:1.0");
        headersMap.put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROCESS_VERSION, "1.0");
        headersMap.putAll(additionalHeaders);

        MessageHeaders headers = new MessageHeaders(headersMap);
        return headers;
    }

    private static void validateHeaders(Map<String, Object> additionalHeaders) {
        if(!additionalHeaders.containsKey(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_RECEIVER_ID)) {
            throw new IllegalArgumentException(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_RECEIVER_ID + " receiver header is mandatory");
        }
        if(!additionalHeaders.containsKey(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_SENDER_ID)) {
            throw new IllegalArgumentException(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_SENDER_ID + " sender header is mandatory");
        }
    }

    private static Map<String, Object> createBasicHeaderMap() {
        return new HashMap<>() {{
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PRODUCTION, "Test");
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROTOCOL, "ebMS");
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROTOCOL_VERSION, "2.0");
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_SYSTEM_MSG_ID, UUID.randomUUID().toString());
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_PROCESS_INSTANCE_ID, UUID.randomUUID().toString());
            put(nl.logius.digid.digilevering.lib.model.Headers.X_AUX_SEQ_NUMBER, "0");
            put(Headers.X_AUX_MSG_ORDER, "false");
        }};
    }


}
