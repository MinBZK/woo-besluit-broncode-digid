
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

package nl.logius.digid.eid.service;

import nl.logius.digid.card.asn1.models.CardPolymorph;
import nl.logius.digid.eid.exceptions.ServerException;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.asn1.ObjectIdentifiers;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.sharedlib.client.HsmClient;
import nl.logius.digid.sharedlib.client.HsmClient.VerificationPoints;
import org.bouncycastle.asn1.*;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

@Service
public class BsnkService {
    private final SecureRandom secureRandom = new SecureRandom();
    private static final BigInteger Q_MINUS_1 = BrainpoolP320r1.Q.subtract(BigInteger.ONE);
    private static ECPoint identityPublicPoint;
    private static ECPoint pseudonymPublicPoint;

    @Autowired
    @Qualifier("hsm-bsnk")
    private HsmClient hsmClient;

    @PostConstruct
    public void init() {
        final VerificationPoints points = hsmClient.fetchVerificationPoints();
        identityPublicPoint = BrainpoolP320r1.CURVE.decodePoint(Base64.decode(points.getIdentity()));
        pseudonymPublicPoint = BrainpoolP320r1.CURVE.decodePoint(Base64.decode(points.getPseudonym()));
    }

    public byte[] convertCardToUsvE(CardPolymorph card) {
        final ASN1ObjectIdentifier oid;
        final ECPoint[] points;
        if (PolymorphType.PIP.getOid().getId().equals(card.getIdentifier())) {
            oid = ObjectIdentifiers.id_BSNK_PIP;
            points = randomizePip(new ECPoint[]{
                card.getPoint0(), card.getPoint1(), card.getPoint2(), identityPublicPoint, pseudonymPublicPoint
            });
        } else if (PolymorphType.PP.getOid().getId().equals(card.getIdentifier())) {
            oid = ObjectIdentifiers.id_BSNK_PP;
            points = randomizePp(new ECPoint[] {
                card.getPoint0(), card.getPoint2(), pseudonymPublicPoint
            });
        } else {
            throw new IllegalArgumentException("Unexpected identifier " + card.getIdentifier());
        }

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(oid);
            sequence.addObject(new ASN1Integer(card.getSchemeVersion()));
            sequence.addObject(new ASN1Integer(card.getSchemeKeyVersion()));
            sequence.addObject(new DERIA5String(card.getCreator()));
            sequence.addObject(new DERIA5String(card.getRecipient()));
            sequence.addObject(new ASN1Integer(card.getRecipientKeySetVersion()));
            sequence.addObject(new ASN1Integer(card.getType()));
            sequence.addObject(toDerSequence(points));
            sequence.close();
        } catch (IOException e) {
            throw new ServerException("Could not transform PIP");
        }

        return buffer.toByteArray();
    }

    private ECPoint[] randomizePip(ECPoint[] points) {
        final BigInteger random = randomElement();
        return new ECPoint[] {
            points[0].add(BrainpoolP320r1.G.multiply(random)),
            points[1].add(points[3].multiply(random)),
            points[2].add(points[4].multiply(random)),
            points[3], points[4]
        };
    }

    private ECPoint[] randomizePp(ECPoint[] points) {
        final BigInteger random = randomElement();
        return new ECPoint[] {
            points[0].add(BrainpoolP320r1.G.multiply(random)),
            points[1].add(points[2].multiply(random)),
            points[2]
        };
    }

    private static DERSequence toDerSequence(ECPoint[] points) {
        final ASN1EncodableVector vector = new ASN1EncodableVector();
        for (final ECPoint point : points) {
            vector.add(new DEROctetString(point.getEncoded(false)));
        }
        return new DERSequence(vector);
    }

    public BigInteger randomElement() {
        /*
         * generate (pseudo) random non-zero number modulo qB in line with BSI Technical
         * Guideline TR-03111, Section 4.1.1.
         */
        byte[] temp = new byte[48]; // get 8 bytes more than what you need
        secureRandom.nextBytes(temp);
        BigInteger macAsNumber = new BigInteger(1, temp); /* Always interpret as non-negative number */
        macAsNumber = macAsNumber.mod(Q_MINUS_1);
        return macAsNumber.add(BigInteger.ONE);
    }
}
