
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

package nl.logius.digid.eid.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.models.rest.converters.DocumentTypeDeserializer;
import nl.logius.digid.eid.models.rest.converters.DocumentTypeSerializer;
import nl.logius.digid.eid.models.rest.converters.RequestDeserializer;
import nl.logius.digid.eid.models.rest.converters.ResponseSerializer;
import nl.logius.digid.sharedlib.converter.CommandAPDUDeserializer;
import nl.logius.digid.sharedlib.converter.CommandAPDUSerializer;
import nl.logius.digid.sharedlib.converter.ResponseAPDUDeserializer;
import nl.logius.digid.sharedlib.converter.ResponseAPDUSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        final ObjectMapper mapper = builder.createXmlMapper(false).build();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(DocumentType.class, new DocumentTypeDeserializer());
        module.addSerializer(DocumentType.class, new DocumentTypeSerializer());

        // rest requests
        module.addDeserializer(GetCertificateRequest.class, new RequestDeserializer<>(GetCertificateRequest.class));
        module.addDeserializer(NikApduResponsesRequest.class, new RequestDeserializer<>(NikApduResponsesRequest.class));
        module.addDeserializer(PolyDataRequest.class, new RequestDeserializer<>(PolyDataRequest.class));
        module.addDeserializer(PolyInfoRequest.class, new RequestDeserializer<>(PolyInfoRequest.class));
        module.addDeserializer(PrepareEacRequest.class, new RequestDeserializer<>(PrepareEacRequest.class));
        module.addDeserializer(SecApduRequest.class, new RequestDeserializer<>(SecApduRequest.class));
        module.addDeserializer(SignatureRequest.class, new RequestDeserializer<>(SignatureRequest.class));

        module.addSerializer(AppResponse.class, new ResponseSerializer());
        module.addDeserializer(CommandAPDU.class, new CommandAPDUDeserializer());
        module.addSerializer(CommandAPDU.class, new CommandAPDUSerializer());
        module.addDeserializer(ResponseAPDU.class, new ResponseAPDUDeserializer());
        module.addSerializer(ResponseAPDU.class, new ResponseAPDUSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_INSTANT));

        mapper.registerModule(module);
        return mapper;
    }
}
