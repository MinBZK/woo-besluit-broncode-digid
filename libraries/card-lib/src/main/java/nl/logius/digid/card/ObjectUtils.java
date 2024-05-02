
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

package nl.logius.digid.card;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ObjectUtils {
    private ObjectUtils() {}

    public static <T> T newInstance(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate " + type);
        }
    }

    public static Object getProperty(PropertyDescriptor pd, Object instance) {
        final Method method = pd.getReadMethod();
        if (method == null) {
            throw new RuntimeException("No getter for " + pd.getName() + " on " + instance.getClass());
        }
        try {
            return method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not get " + pd.getName() + " on " + instance.getClass());
        }
    }

    public static void setProperty(PropertyDescriptor pd, Object instance, Object value) {
        final Method method = pd.getWriteMethod();
        if (method == null) {
            throw new RuntimeException("No setter for " + pd.getName() + " on " + instance.getClass());
        }
        try {
            method.invoke(instance, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not set " + pd.getName() + " on " + instance.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, ClassLoader loader, Class<T> type) {
        return (T) deserialize(data, loader);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, Class<T> type) {
        return (T) deserialize(data);
    }

    public static Object deserialize(byte[] data, ClassLoader loader) {
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            try (final ObjectInputStream ois = new CustomObjectInputStream(bis, loader)) {
                return deserialize(ois);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO exception", e);
        }
    }

    public static Object deserialize(byte[] data) {
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            try (final ObjectInputStream ois = new ObjectInputStream(bis)) {
                return deserialize(ois);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO exception", e);
        }
    }

    private static Object deserialize(ObjectInputStream ois) throws IOException {
        final Object obj;
        try {
            obj = ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found", e);
        }
        return obj;
    }

    public static byte[] serialize(Object obj) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(obj);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO exception", e);
        }
    }

    private static class CustomObjectInputStream extends ObjectInputStream {
        private final ClassLoader loader;

        public CustomObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
            super(in);
            this.loader = loader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            return Class.forName(desc.getName(), false, loader);
        }
    }
}
