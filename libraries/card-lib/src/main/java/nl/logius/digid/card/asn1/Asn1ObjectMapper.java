
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

package nl.logius.digid.card.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;

import org.bouncycastle.asn1.ASN1Primitive;

import nl.logius.digid.card.ObjectUtils;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.converters.BigIntegerConverter;
import nl.logius.digid.card.asn1.converters.BouncyCastlePrimitiveConverter;
import nl.logius.digid.card.asn1.converters.ByteArrayConverter;
import nl.logius.digid.card.asn1.converters.IntegerConverter;
import nl.logius.digid.card.asn1.converters.Utf8StringConverter;

/**
 * Object mapper that converts ASN1 byte arrays to Java objects and back
 */
public class Asn1ObjectMapper {
    public static final Map<Class, Asn1Converter> DEFAULT_CONVERTERS;

    private final BouncyCastlePrimitiveConverter bcPrimitiveConverter;
    private final ConcurrentHashMap<Class, List<Asn1Field>> fields;
    private final ConcurrentHashMap<Class, Asn1Converter> converters;

    static {
        final Asn1Converter<Integer> intConverter = new IntegerConverter();
        DEFAULT_CONVERTERS = new ImmutableMap.Builder<Class, Asn1Converter>()
            .put(int.class, intConverter)
            .put(Integer.class, intConverter)
            .put(byte[].class, new ByteArrayConverter())
            .put(BigInteger.class, new BigIntegerConverter())
            .put(String.class, new Utf8StringConverter())
            .build();
    }

    /**
     * Create ASN1 object mapper that can uses specified converters for types with default converters added
     *
     * @param custom map with classes to converters
     */
    public Asn1ObjectMapper(Map<Class, Asn1Converter> custom) {
        bcPrimitiveConverter = new BouncyCastlePrimitiveConverter();
        converters = new ConcurrentHashMap<>(custom);
        fields = new ConcurrentHashMap<>();
    }

    /**
     * Creates ASN1 object mapper with default converters
     */
    public Asn1ObjectMapper() {
        this(DEFAULT_CONVERTERS);
    }

    /**
     * Reads bytes to ASN1 object
     *
     * @param data byte array with data
     * @param offset offset within byte array of data to be decoded
     * @param length length of data to be decoded
     * @param type class to decode
     * @param <T> type of class to decode
     * @return decoded class of data
     */
    public <T> T read(byte[] data, int offset, int length, Class<T> type) {
        final Asn1Entity entity = type.getAnnotation(Asn1Entity.class);
        if (entity == null) {
            throw new Asn1Exception("Class should have Asn1Entity annotation");
        }

        final boolean tlv = entity.tagNo() != 0;
        try (final Asn1ObjectInputStream in = new Asn1ObjectInputStream(data, offset, length, tlv)) {
            if (tlv && in.tagNo != entity.tagNo()) {
                throw new Asn1Exception("Tag %x does not match, expected %x", in.tagNo, entity.tagNo());
            }
            return readValue(in, entity.converter(), type);
        }
    }

    /**
     * Reads bytes to ASN1 object
     *
     * @param data byte array with data
     * @param type class to decode
     * @param <T> type of class to decode
     * @return decoded class from data
     */
    public <T> T read(byte[] data, Class<T> type) {
        return read(data, 0, data.length, type);
    }

    /**
     * Writes ASN1 object to bytes
     *
     * @param instance instance to encode
     * @return encoded ASN1 object
     */
    public byte[] write(Object instance) {
        final Asn1Entity entity = instance.getClass().getAnnotation(Asn1Entity.class);
        if (entity == null) {
            throw new Asn1Exception("Class should have Asn1Entity annotation");
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final Asn1OutputStream out = new Asn1OutputStream(bos, entity.tagNo())) {
            writeValue(out, entity.converter(), instance.getClass(), instance);
        } catch (IOException e) {
            throw new Asn1Exception("Unhandled IO exception", e);
        }
        return bos.toByteArray();
    }

    /**
     * Reads object from ASN1 input stream, assumes tag and length are already red
     *
     * @param in ASN1 input stream
     * @param converter converter class to use
     * @param type class to decode to
     * @param <T> type of class to decode to
     * @return decoded class from data
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Asn1ObjectInputStream in, Class<? extends Asn1Converter> converter, Class<T> type) {
        final T instance = getConverter(converter, type).deserialize(in, type, this);
        if (instance instanceof Asn1Constructed) {
            ((Asn1Constructed) instance).constructed(this);
        }
        if (instance instanceof Asn1Raw) {
            ((Asn1Raw) instance).setRaw(in.toByteArray());
        }
        return instance;
    }

    /**
     * Writes object to ASN1 output stream
     *
     * @param out ASN1 output stream
     * @param converter converter class to use
     * @param type type of object
     * @param obj object to be encooded
     * @throws IOException only thrown by parent output stream
     */
    @SuppressWarnings("unchecked")
    public void writeValue(Asn1OutputStream out, Class<? extends Asn1Converter> converter, Class type, Object obj)
            throws IOException {
        getConverter(converter, type).serialize(out, type, obj,this);
    }

    /**
     * Get list of fields for this class
     * @param type class to introspect
     * @return list of fields
     */
    public List<Asn1Field> getFields(Class<?> type) {
        return fields.computeIfAbsent(type, (c) -> Asn1Field.of(type));
    }

    @SuppressWarnings("unchecked")
    private <T> Asn1Converter<T> getConverter(Class converter, Class<T> type) {
        if (converter == Asn1Converter.None.class) {
            final Asn1Converter<T> instance = converters.get(type);
            if (instance != null) {
                return instance;
            } else if (ASN1Primitive.class.isAssignableFrom(type)) {
                return (Asn1Converter) bcPrimitiveConverter;
            }
            throw new Asn1Exception("No converter defined for " + type, ", nor custom one specified");
        } else {
            return converters.computeIfAbsent(converter, (c) -> (Asn1Converter) ObjectUtils.newInstance(c));
        }
    }
}
