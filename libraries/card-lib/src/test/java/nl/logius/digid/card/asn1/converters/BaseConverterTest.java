
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nl.logius.digid.card.asn1.Asn1Converter;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;

public abstract class BaseConverterTest {
    protected static <T> byte[] serialize(Asn1Converter<T> converter, Class<? extends T> type, T object) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (final Asn1OutputStream out = new Asn1OutputStream(bos)) {
                converter.serialize(out, type, object, new Asn1ObjectMapper());
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T> T deserialize(Asn1Converter<T> converter, Class<? extends T> type, byte[] data) {
        return deserialize(converter, type, data, false);
    }

    protected static <T> T deserialize(Asn1Converter<T> converter, Class<? extends T> type, byte[] data, boolean tlv) {
        final byte[] total = new byte[data.length + 2];
        System.arraycopy(data, 0, total, 1, data.length);
        try (final Asn1ObjectInputStream is = new Asn1ObjectInputStream(total, 1, data.length, tlv)) {
            return converter.deserialize(is, type, new Asn1ObjectMapper());
        }
    }
}
