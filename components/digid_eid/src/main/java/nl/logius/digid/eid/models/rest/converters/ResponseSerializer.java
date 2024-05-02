
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

package nl.logius.digid.eid.models.rest.converters;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import nl.logius.digid.eid.models.rest.ResponseDataField;
import nl.logius.digid.eid.models.rest.app.AppResponse;

public class ResponseSerializer extends JsonSerializer<AppResponse> {
    private static final String EMBEDDED_FIELD_NAME = "responseData";

    @Override
    public void serialize(AppResponse value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try {
            final BeanInfo info = Introspector.getBeanInfo(value.getClass(), Object.class);

            gen.writeStartObject();
            final Map<String, Object> responseData = new HashMap<>();
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                if (pd.getReadMethod().getAnnotation(ResponseDataField.class) != null) {
                    responseData.put(pd.getName(), pd.getReadMethod().invoke(value));
                } else {
                    gen.writeObjectField(pd.getName(), pd.getReadMethod().invoke(value));
                }
            }
            gen.writeObjectFieldStart(EMBEDDED_FIELD_NAME);
            for (Map.Entry<String, Object> entry : responseData.entrySet()) {
                gen.writeObjectField(entry.getKey(), entry.getValue());
            }
            gen.writeEndObject();
            gen.writeEndObject();
        } catch (Exception e) {
            throw new IOException("Could not serialize ResponseObject", e);
        }
    }
}
