
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
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import nl.logius.digid.card.ObjectUtils;

public class RequestDeserializer<T> extends JsonDeserializer<T> {
    private static final String EMBEDDED_FIELD_NAME = "messageData";
    private static final String UNEXPECTED_TOKEN = "unexpected token ";

    private final Class<T> type;
    private final Map<String, PropertyDescriptor> properties;

    public RequestDeserializer(Class<T> type) {
        this.type = type;
        final BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type, Object.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Could not introspect " + type, e);
        }
        final ImmutableMap.Builder<String, PropertyDescriptor> builder = ImmutableMap.builder();
        for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
            if (pd.getWriteMethod() != null) {
                builder.put(pd.getName(), pd);
            }
        }
        properties = builder.build();
    }

    public static <T> RequestDeserializer<T> create(Class<T> type) {
        return new RequestDeserializer<>(type);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        assertToken(JsonToken.START_OBJECT, parser.currentToken());

        final T obj = ObjectUtils.newInstance(type);
        boolean nested = false;
        boolean valid = true;
        while (valid) {
            final String name;
            switch(parser.nextToken()) {
                case FIELD_NAME:
                    name = parser.currentName();

                    if (!nested && EMBEDDED_FIELD_NAME.equals(name)) {
                        nested = true;
                        assertToken(JsonToken.START_OBJECT, parser.nextToken());
                        break;
                    }

                    final PropertyDescriptor pd = properties.get(name);

                    switch (parser.nextToken()) {
                        case VALUE_NULL:
                            break;
                        case VALUE_NUMBER_INT:
                            ObjectUtils.setProperty(pd, obj, parser.getIntValue());
                            break;
                        case START_ARRAY:
                            ObjectUtils.setProperty(pd, obj, readArray(parser, getTypeOfArray(pd)));
                            break;
                        case START_OBJECT:
                        case VALUE_STRING:
                            ObjectUtils.setProperty(pd, obj, parser.readValueAs(pd.getPropertyType()));
                            break;
                        default:
                            throw new IOException(UNEXPECTED_TOKEN + parser.currentToken());
                    }
                    break;
                case END_OBJECT:
                    if (nested) {
                        nested = false;
                    } else {
                        valid = false;
                    }
                    break;
                default:
                    valid = false;
            }
        }

        assertToken(JsonToken.END_OBJECT, parser.currentToken());
        if (nested) {
            throw new IOException("Still inside " + EMBEDDED_FIELD_NAME);
        }

        return obj;
    }

    private static <T> List<T> readArray(JsonParser parser, Class<T> type) throws IOException {
        final ImmutableList.Builder<T> builder = ImmutableList.builder();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            switch (parser.currentToken()) {
                case START_OBJECT:
                case VALUE_STRING:
                    builder.add(parser.readValueAs(type));
                    break;
                default:
                throw new IOException(UNEXPECTED_TOKEN + parser.currentToken());
            }
        }
        return builder.build();
    }

    private static Class<?> getTypeOfArray(PropertyDescriptor pd) {
        final ParameterizedType type = (ParameterizedType) pd.getReadMethod().getGenericReturnType();
        return (Class<?>) type.getActualTypeArguments()[0];
    }

    private static void assertToken(JsonToken expected, JsonToken actual) throws IOException {
        if (expected != actual) {
            throw new IOException(UNEXPECTED_TOKEN + actual.asString() + ", expected " + expected.asString());
        }
    }
}
