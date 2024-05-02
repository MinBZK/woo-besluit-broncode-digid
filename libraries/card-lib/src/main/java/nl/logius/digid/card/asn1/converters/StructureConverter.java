
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

package nl.logius.digid.card.asn1.converters;

import java.io.IOException;
import java.util.List;

import nl.logius.digid.card.ObjectUtils;
import nl.logius.digid.card.asn1.Asn1Converter;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1Field;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;

public abstract class StructureConverter<T> implements Asn1Converter<T> {
    protected int getNestedTagNo(Class<? extends T> klass) {
        final int tagNo = klass.getAnnotation(Asn1Entity.class).param();
        return tagNo == 0 ? 0x30 : tagNo;
    }

    protected String readIdentifier(Asn1ObjectInputStream in) {
        if (in.readTag() != 0x06) {
            throw new Asn1Exception("Expected object identifier");
        }
        final int length = in.readLength();
        final String id = Asn1Utils.decodeObjectIdentifier(in.buffer(), in.position(), length);
        in.skip(length);
        return id;
    }

    protected Object readFields(Asn1ObjectMapper mapper, Asn1ObjectInputStream in, Fields fields, Object instance) {
        while (!in.atEnd()) {
            try (final Asn1ObjectInputStream propIn = in.next()) {
                final Asn1Field field = fields.get(propIn.tagNo);
                if (field == null) {
                    propIn.advanceToEnd();
                    continue;
                }
                final Object value = mapper.readValue(propIn, field.converter(), field.type());
                ObjectUtils.setProperty(field.pd, instance, value);
            }
        }
        if (!fields.done()) {
            throw new Asn1Exception("At end of data, but still non optional fields");
        }
        return instance;
    }

    protected boolean fetchValues(List<Asn1Field> fields, Object instance, Object[] values) {
        boolean empty = true;
        for (int i = 0; i < values.length; i++) {
            final Asn1Field field = fields.get(i);
            values[i] = ObjectUtils.getProperty(field.pd, instance);
            if (values[i] == null) {
                if (!field.property.optional()) {
                    throw new Asn1Exception("Missing value for non optional property " + field.pd.getName());
                }
            } else {
                empty = false;
            }
        }
        return !empty;
    }

    protected void writeFields(Asn1ObjectMapper mapper, Asn1OutputStream out, Class<?> type,  Object instance)
            throws IOException  {
        for (final Asn1Field field : mapper.getFields(type)) {
            final Object value = ObjectUtils.getProperty(field.pd, instance);
            if (value == null) {
                if (field.property.optional()) continue;
                throw new Asn1Exception("Missing value for non optional property " + field.pd.getName());
            }
            try (final Asn1OutputStream propOut = new Asn1OutputStream(out, field.tagNo)) {
                mapper.writeValue(propOut, field.converter(), field.type(), value);
            }
        }
    }
}
