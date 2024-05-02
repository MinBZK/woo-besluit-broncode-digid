
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

package nl.logius.digid.sharedlib.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bouncycastle.util.encoders.Base64;

public class JsonTest {
    @SuppressWarnings("unchecked")
    protected static <T> byte[] serializeBinary(JsonSerializer<T> serializer, T obj) throws JsonProcessingException {
        final SimpleModule module = new SimpleModule();
        module.addSerializer((Class<T>) obj.getClass(), serializer);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        final String result = mapper.writeValueAsString(obj);
        assertEquals('"', result.charAt(0));
        assertEquals('"', result.charAt(result.length() - 1));

        return Base64.decode(result.substring(1, result.length() - 1));
    }

    protected static <T> T deserializeBinary(JsonDeserializer<T> deserializer, Class<T> type, byte[] value) throws IOException {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(type, deserializer);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        return mapper.readValue("\"" + Base64.toBase64String(value) + "\"", type);
    }
}
