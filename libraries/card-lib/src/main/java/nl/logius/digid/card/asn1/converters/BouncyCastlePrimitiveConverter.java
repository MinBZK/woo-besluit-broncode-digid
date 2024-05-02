
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

import org.bouncycastle.asn1.ASN1Primitive;

import nl.logius.digid.card.asn1.Asn1Converter;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.Asn1Utils;

public class BouncyCastlePrimitiveConverter implements Asn1Converter<ASN1Primitive> {
    @Override
    public ASN1Primitive deserialize(Asn1ObjectInputStream in, Class<? extends ASN1Primitive> type,
                                     Asn1ObjectMapper mapper) {
        final ASN1Primitive value;
        try {
            value = ASN1Primitive.fromByteArray(in.advanceToByteArray());
        } catch (IOException e) {
            throw new Asn1Exception("Could not decode ASN1Primitive", e);
        }
        if (!type.isInstance(value)) {
            throw new Asn1Exception("Property is a " + value.getClass(), " expected " + type);
        }
        return value;
    }

    @Override
    public void serialize(Asn1OutputStream out, Class<? extends ASN1Primitive> type, ASN1Primitive value,
                          Asn1ObjectMapper mapper) throws IOException {
        Asn1Utils.writeRawValue(value.getEncoded(), out);
    }
}
