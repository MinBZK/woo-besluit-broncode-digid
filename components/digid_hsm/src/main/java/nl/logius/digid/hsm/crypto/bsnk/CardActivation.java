
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

package nl.logius.digid.hsm.crypto.bsnk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.*;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CryptoServerAPI.CryptoServerException;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.CardPolymorph;
import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.CardActivateRequest;
import nl.logius.digid.hsm.model.CardActivateResponse;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.pp.parser.Asn1Parser;

public class CardActivation extends Activation {
    private static final String OID_CARD_PIP = "2.16.528.1.1003.10.9.3.3";
    private static final String OID_CARD_PP = "2.16.528.1.1003.10.9.4.3";
    private static final Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CardActivation(ConnectionPool pool) {
        super(pool);
    }

    public CardActivateResponse activate(CardActivateRequest request) {
        final byte[] pip, pp;
        try {
            final Connection connection = getConnection();
            final byte[] input = encode(request);
            pip = unsign(connection.activateGetSignedPip(input));
            pp = unsign(connection.activateGetSignedPp(input));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error activate pip/pp for card", e);
            throw new CryptoError("Error activate pip/pp for card", e);
        }

        final CardActivateResponse response = new CardActivateResponse();

        try {
            response.setPip(convert(pip, OID_CARD_PIP, request.getSequenceNo()));
        } catch (IOException e) {
            logger.error("Error transforming pip for card", e);
            throw new CryptoError("Error transforming pip for card", e);
        }

        try {
            response.setPp(convert(pp, OID_CARD_PP, request.getSequenceNo()));
        } catch (IOException e) {
            logger.error("Error transforming pp for card", e);
            throw new CryptoError("Error transforming pp for card", e);
        }

        return response;
    }

    private byte[] convert(byte[] input, String oid, String sequenceNo) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        final Asn1Parser parser = new Asn1Parser(input);
        parser.readObject(DLSequenceParser.class);
        parser.readObject(ASN1ObjectIdentifier.class);

        final CardPolymorph card = new CardPolymorph();
        card.setIdentifier(oid);

        card.setSchemeVersion(parser.readObject(ASN1Integer.class).getPositiveValue().intValue());
        card.setSchemeKeyVersion(parser.readObject(ASN1Integer.class).getPositiveValue().intValue());
        card.setCreator(parser.readObject(DERIA5String.class).getString());
        card.setRecipient(parser.readObject(DERIA5String.class).getString());
        card.setRecipientKeySetVersion(parser.readObject(ASN1Integer.class).getPositiveValue().intValue());
        card.setType(parser.readObject(ASN1Integer.class).getPositiveValue().intValue());
        card.setSequenceNo(sequenceNo);

        final DLSequence sequence = ((DLSequence) parser.readObject(DLSequenceParser.class).getLoadedObject());
        final ECPoint[] points = readPoints(sequence);
        card.setPoint0(points[0]);
        if (oid == OID_CARD_PIP) {
            card.setPoint1(points[1]);
            card.setPoint2(points[2]);
        } else {
            card.setPoint2(points[1]);
        }

        return mapper.write(card);
    }

    private ECPoint[] readPoints(DLSequence sequence) throws IOException {
        final ECPoint[] points = new ECPoint[sequence.size()];
        for (int i = 0; i < points.length; i++) {
            final DEROctetString octetString =
                (DEROctetString) sequence.getObjectAt(i).toASN1Primitive();
            points[i] = BrainpoolP320r1.CURVE.decodePoint(octetString.getOctets());
        }
        return points;
    }
}
