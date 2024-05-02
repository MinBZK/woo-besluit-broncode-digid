
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
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.logius.digid.card.ObjectUtils;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1Field;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;

public class SetOfIdentifiedConverter extends StructureConverter<Object> {
    @Override
    public Object deserialize(Asn1ObjectInputStream in, Class<? extends Object> type,
                                      Asn1ObjectMapper mapper) {
        final Asn1Entity entity = type.getAnnotation(Asn1Entity.class);
        final Object instance = ObjectUtils.newInstance(type);

        final Map<String, List<Asn1Field>> fields = fieldsMap(mapper.getFields(type));
        while (!in.atEnd()) {
            try (final Asn1ObjectInputStream seq = in.next()) {
                if (seq.tagNo != getNestedTagNo(type)) {
                    throw new Asn1Exception("Expected tag %x, got %x", getNestedTagNo(type), seq.tagNo);
                }

                final String id = readIdentifier(seq);
                final List<Asn1Field> fieldsOfId = fields.remove(id);
                if (fieldsOfId == null) {
                    if (!entity.partial()) throw new Asn1Exception("Found unknown identifier " + id);
                    seq.advanceToEnd();
                    continue;
                }

                final FieldSequence fieldsSeq = new FieldSequence(false, fieldsOfId);
                readFields(mapper, seq, fieldsSeq, instance);
                while (!seq.atEnd()) {
                    try (final Asn1ObjectInputStream obj = seq.next()) {
                        final Asn1Field field = fieldsSeq.get(obj.tagNo);
                        final Object attr = mapper.readValue(obj, field.converter(), field.type());
                        ObjectUtils.setProperty(field.pd, instance, attr);
                    }
                }
                if (!fieldsSeq.done()) {
                    throw new Asn1Exception("At end of data, but still non optional fields");
                }
            }
        }
        return instance;
    }

    @Override
    public void serialize(Asn1OutputStream out, Class<? extends Object> type, Object instance, Asn1ObjectMapper mapper)
            throws IOException {
        for (final Map.Entry<String, List<Asn1Field>> entry : fieldsMap(mapper.getFields(type)).entrySet()) {
            final List<Asn1Field> fields = entry.getValue();
            final Object[] values = new Object[fields.size()];
            if (!fetchValues(fields, instance, values)) continue;

            try (final Asn1OutputStream seqOut = new Asn1OutputStream(out, getNestedTagNo(type))) {
                try (final Asn1OutputStream oidOut = new Asn1OutputStream(seqOut, 0x06)) {
                    Asn1Utils.encodeObjectIdentifier(entry.getKey(), oidOut);
                }

                int i = 0;
                for (final Asn1Field field : fields) {
                    try (final Asn1OutputStream propOut = new Asn1OutputStream(seqOut, field.tagNo)) {
                        mapper.writeValue(propOut, field.converter(), field.type(), values[i++]);
                    }
                }
            }
        }
    }

    private Map<String, List<Asn1Field>> fieldsMap(List<Asn1Field> fields) {
        final Map<String, List<Asn1Field>> result = new LinkedHashMap<>(fields.size());
        for (final Asn1Field field : fields) {
            if (field.identifier == null) {
                throw new Asn1Exception("Property without Asn1ObjectIdentifier annotation");
            }
            result.computeIfAbsent(field.identifier.value(), (id) -> new ArrayList<>()).add(field);
        }
        return result;
    }


}
