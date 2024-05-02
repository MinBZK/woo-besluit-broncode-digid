
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

import java.io.IOException;

/**
 * Interface for converting objects from and to ASN1
 *
 * Used by Asn1ObjectMapper, configurable on Asn1Entity and Ans1Property
 * @param <T> Type of object
 */
public interface Asn1Converter<T> {
    /**
     * Deserialize object out of Asn1InputStream, tag and length are already read
     *
     * It is the responsibility of this method to fully read the object, otherwise further deserialization will fail
     * @param in input stream to be read from
     * @param type the type of the object that is specified
     * @param mapper the instantiated object mapper that is being used
     * @return object
     */
    T deserialize(Asn1ObjectInputStream in, Class<? extends T> type, Asn1ObjectMapper mapper);

    /**
     * Serialize object into Asn1OutputStream, tag is already written and length will be automatically determined
     *
     * @param out output stream to write to
     * @param type type of object to serialize
     * @param obj object to serialize
     * @param mapper the instantiated object mapper that is being used
     * @throws IOException for parent output stream
     */
    void serialize(Asn1OutputStream out, Class<? extends T> type, T obj, Asn1ObjectMapper mapper) throws IOException;

    /**
     * Dummy converter to represent no converter
     */
    class None implements Asn1Converter<byte[]> {
        @Override
        public byte[] deserialize(Asn1ObjectInputStream in, Class<? extends byte[]> type, Asn1ObjectMapper mapper) {
            throw new UnsupportedOperationException("None is not a converter");
        }

        @Override
        public void serialize(Asn1OutputStream out, Class<? extends byte[]> type, byte[] obj, Asn1ObjectMapper mapper) {
            throw new UnsupportedOperationException("None is not a converter");
        }
    }
}
