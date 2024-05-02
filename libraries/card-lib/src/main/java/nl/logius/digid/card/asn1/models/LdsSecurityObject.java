
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

package nl.logius.digid.card.asn1.models;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectInputStream;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.SimpleConverter;
import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.card.crypto.CryptoException;
import nl.logius.digid.card.crypto.VerificationException;

@Asn1Entity(tagNo = 0x30, partial = true)
public class LdsSecurityObject implements Serializable {
    public static final String OID = "2.23.136.1.1.1";
    private static final long serialVersionUID = 1L;

    private int version;
    private String algorithm;
    private Map<Integer,byte[]> digests;

    public void verify(int number, byte[] dg) {
        final byte[] compare = digests.get(number);
        if (compare == null) {
            throw new CryptoException("Could not find digest of data group " + number);
        }
        final byte[] calculated = DigestUtils.digest(algorithm).digest(dg);
        if (!CryptoUtils.compare(compare, calculated, 0)) {
            throw new VerificationException("Digest of data group " + number + " is not equal to security object");
        }
    }

    @Asn1Property(tagNo = 0x02)
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Asn1Property(tagNo = 0x30, converter = AlgorithmConverter.class)
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Asn1Property(tagNo = 0x30, converter = DigestsConverter.class)
    public Map<Integer, byte[]> getDigests() {
        return digests;
    }

    public void setDigests(Map<Integer, byte[]> digests) {
        this.digests = digests;
    }

    public static class AlgorithmConverter extends SimpleConverter<String> {
        @Override
        protected String deserialize(Asn1ObjectInputStream in) {
            final String oid;
            try (final Asn1ObjectInputStream inner = in.next()) {
                if (inner.tagNo != 0x06) {
                    throw new Asn1Exception("Expected tag 0x06, got %x", inner.tagNo);
                }
                oid = Asn1Utils.decodeObjectIdentifier(inner.buffer(), inner.position(), inner.length);
                inner.advanceToEnd();
            }
            in.advanceToEnd();
            return oid;
        }

        @Override
        protected void serialize(Asn1OutputStream out, String oid) throws IOException {
            try (final Asn1OutputStream inner = new Asn1OutputStream(out, 0x30)) {
                Asn1Utils.encodeObjectIdentifier(oid, inner);
            }
        }
    }

    public static class DigestsConverter extends SimpleConverter<Map<Integer, byte[]>> {
        @Override
        protected Map<Integer, byte[]> deserialize(Asn1ObjectInputStream in) {
            final ImmutableMap.Builder<Integer, byte[]> digests = ImmutableMap.builder();
            while (!in.atEnd()) {
                try (final Asn1ObjectInputStream seq = in.next()) {
                    if (seq.tagNo != 0x30) {
                        throw new Asn1Exception("Expected sequence, got %x", seq.tagNo);
                    }
                    final int no;
                    try (final Asn1ObjectInputStream number = seq.next()) {
                        if (number.tagNo != 0x02) {
                            throw new Asn1Exception("Expected integer, got %x", number.tagNo);
                        }
                        no = number.readInt(number.length);
                    }
                    try (final Asn1ObjectInputStream value = seq.next()) {
                        if (value.tagNo != 0x04) {
                            throw new Asn1Exception("Expected octet string, got %x", value.tagNo);
                        }
                        digests.put(no, value.readAll());
                    }
                }
            }
            return digests.build();
        }

        @Override
        protected void serialize(Asn1OutputStream out, Map<Integer, byte[]> digests) throws IOException {
            try (final Asn1OutputStream seq = new Asn1OutputStream(out, 0x30)) {
                for (final int key : Ordering.natural().sortedCopy(digests.keySet())) {
                    try (final Asn1OutputStream number = new Asn1OutputStream(seq, 0x02)) {
                        number.writeInt(key);
                    }
                    try (final Asn1OutputStream value = new Asn1OutputStream(seq, 0x04)) {
                        value.write(digests.get(key));
                    }
                }
            }
        }
    }
}
