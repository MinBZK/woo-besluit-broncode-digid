
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
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HeaderUtilTest {

    private static final String UUID_REGEX = "[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";

    @Test
    public void testReceiverHeaderPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put(Headers.X_AUX_SENDER_ID, "senderId");

        assertThrows(IllegalArgumentException.class, () -> HeaderUtil.createAfnemersberichtAanDGLHeaders(map), "x_aux_receiver_id receiver header is mandatory");
    }

    @Test
    public void testSenderHeaderPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put(Headers.X_AUX_RECEIVER_ID, "receiverId");

        assertThrows(IllegalArgumentException.class, () -> HeaderUtil.createAfnemersberichtAanDGLHeaders(map), "x_aux_sender_id sender header is mandatory");
    }

    private Map<String, Object> validHeaders(){
        Map<String, Object> map = new HashMap<>();
        map.put(Headers.X_AUX_SENDER_ID, "senderId");
        map.put(Headers.X_AUX_RECEIVER_ID, "receiverId");
        return map;
    }

    @Test
    public void testAfnemersBerichtAanDGLHeaders() {
        MessageHeaders afnemersberichtAanDGLHeaders = HeaderUtil.createAfnemersberichtAanDGLHeaders(validHeaders());

        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PRODUCTION), is("Test"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PROTOCOL), is("ebMS"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PROTOCOL_VERSION), is("2.0"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_SYSTEM_MSG_ID).toString(), matchesPattern(UUID_REGEX));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PROCESS_INSTANCE_ID).toString(), matchesPattern(UUID_REGEX));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_SEQ_NUMBER), is("0"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_MSG_ORDER), is("false"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_ACTION), is("BRPAfnemersberichtAanDGL"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_ACTIVITY), is("dgl:objecten:1.0"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PROCESS_TYPE), is("dgl:objecten:1.0"));
        assertThat(afnemersberichtAanDGLHeaders.get(Headers.X_AUX_PROCESS_VERSION), is("1.0"));
    }

    @Test
    public void testVerstrekkingAanAfnemerHeaders() {
        MessageHeaders verstrekkingAanAfnemerHeaders = HeaderUtil.createVerstrekkingAanAfnemerHeaders(validHeaders());

        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PRODUCTION), is("Test"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PROTOCOL), is("ebMS"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PROTOCOL_VERSION), is("2.0"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_SYSTEM_MSG_ID).toString(), matchesPattern(UUID_REGEX));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PROCESS_INSTANCE_ID).toString(), matchesPattern(UUID_REGEX));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_SEQ_NUMBER), is("0"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_MSG_ORDER), is("false"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_ACTION), is("verstrekkingAanAfnemer"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_ACTIVITY), is("dgl:ontvangen:1.0"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PROCESS_TYPE), is("dgl:ontvangen:1.0"));
        assertThat(verstrekkingAanAfnemerHeaders.get(Headers.X_AUX_PROCESS_VERSION), is("1.0"));
    }
}
