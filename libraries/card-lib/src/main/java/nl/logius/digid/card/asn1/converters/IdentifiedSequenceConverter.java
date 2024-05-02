
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

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.asn1.interfaces.Identifiable;

public class IdentifiedSequenceConverter extends StructureConverter<Identifiable> {
    @Override
    public Identifiable deserialize(Asn1ObjectInputStream in, Class<? extends Identifiable> type,
                                    Asn1ObjectMapper mapper) {
        final Identifiable instance;
        final String id = readIdentifier(in);
        try (final Asn1ObjectInputStream inner = in.next()) {
            if (inner.tagNo != getNestedTagNo(type)) {
                throw new Asn1Exception("Expected tag %x, got %x", getNestedTagNo(type), inner.tagNo);
            }
            instance = mapper.readValue(inner, SequenceConverter.class, type);
        }
        instance.setIdentifier(id);
        return instance;
    }

    @Override
    public void serialize(Asn1OutputStream out, Class<? extends Identifiable> type, Identifiable instance,
                          Asn1ObjectMapper mapper) throws IOException {
        try (final Asn1OutputStream oid = new Asn1OutputStream(out, 0x06)) {
            Asn1Utils.encodeObjectIdentifier(instance.getIdentifier(), oid);
        }
        try (final Asn1OutputStream inner = new Asn1OutputStream(out, getNestedTagNo(type))) {
            mapper.writeValue(inner, SequenceConverter.class, type, instance);
        }
    }
}
