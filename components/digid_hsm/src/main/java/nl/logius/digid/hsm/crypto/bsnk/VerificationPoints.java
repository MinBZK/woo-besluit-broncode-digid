
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

import CryptoServerAPI.CryptoServerException;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.hsm.crypto.BsnkAction;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.pp.PolyPseudoException;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.pp.parser.Asn1Parser;
import org.bouncycastle.asn1.*;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class VerificationPoints extends BsnkAction {

    private final static String PUBLIC_KEY_OID = "1.2.840.10045.2.1";
    private static final int TAG_KEY_LABEL = 7;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public VerificationPoints(ConnectionPool pool) {
        super(pool);
    }

    public Map<String,byte[]> getPoints(int schemeVersion, int schemeKeyVersion) {
        final byte[] responseIP;
        final byte[] responsePP;
        try {
            responseIP = getConnection().getPublicKey(encode(schemeVersion, schemeKeyVersion, "IPM_IPP"));
            responsePP = getConnection().getPublicKey(encode(schemeVersion, schemeKeyVersion, "PPM_PPP"));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error fetching service provider keys", e);
            throw new CryptoError("Could not get service provider keys", e);
        }
        return decode(responseIP, responsePP);
    }

    private Map<String,byte[]> decode(byte[] responseIP, byte[] responsePP) {
        try {
            Asn1Parser parser = new Asn1Parser(responseIP);
            parser.readObject(DLSequenceParser.class);
            final ECPoint y = decodePoint(parser);

            parser = new Asn1Parser(responsePP);
            parser.readObject(DLSequenceParser.class);
            final ECPoint z = decodePoint(parser);

            return ImmutableMap.of("IPp", y.getEncoded(true), "PPp", z.getEncoded(true));
        } catch (IOException e) {
            logger.error("Error decoding response form HSM", e);
            throw new CryptoError("Could not decode response from HSM", e);
        }
    }

    private ECPoint decodePoint(Asn1Parser parser) throws IOException {
        parser.readObject(DLSequenceParser.class);
        final ASN1ObjectIdentifier oid = parser.readObject(ASN1ObjectIdentifier.class);
        if (!PUBLIC_KEY_OID.equals(oid.toString())) {
            throw new CryptoError(String.format("Expect object identifier %s, got %s", PUBLIC_KEY_OID, oid));
        }
        parser.readObject(ASN1ObjectIdentifier.class);
        try {
            return BrainpoolP320r1.CURVE.decodePoint(parser.readObject(DERBitString.class).getBytes()).normalize();
        } catch (PolyPseudoException e) {
            logger.error("Error decoding point on curve", e);
            throw new CryptoError("Could not decode point on curve", e);
        }
    }

    private byte[] encode(int schemeVersion, int schemeKeyVersion, String label) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(new ASN1Integer(schemeVersion));
            sequence.addObject(new ASN1Integer(schemeKeyVersion));

            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(new DERIA5String(label).getEncoded()))) {
                sequence.addObject(new DLTaggedObject(false, TAG_KEY_LABEL, asn1Stream.readObject()));
            }

            sequence.close();
        } catch (IOException e) {
            throw new CryptoError("Could not encode request for HSM", e);
        }

        return buffer.toByteArray();
    }
}
