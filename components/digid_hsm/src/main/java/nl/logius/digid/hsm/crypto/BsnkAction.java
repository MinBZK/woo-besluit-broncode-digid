
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

package nl.logius.digid.hsm.crypto;

import java.io.IOException;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DLSequenceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.pp.PolyPseudoException;
import nl.logius.digid.pp.parser.Asn1Parser;

public abstract class BsnkAction extends Action {

    protected static final String OID_PI = "2.16.528.1.1003.10.1.1.1";
    protected static final String OID_PP = "2.16.528.1.1003.10.1.1.2";
    protected static final String OID_SIGNED_PI = "2.16.528.1.1003.10.1.1.3";
    protected static final String OID_SIGNED_PP = "2.16.528.1.1003.10.1.1.4";
    protected static final String OID_PIP = "2.16.528.1.1003.10.1.1.5";
    protected static final String OID_SIGNED_PIP = "2.16.528.1.1003.10.1.1.6";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BsnkAction(ConnectionPool pool) {
        super(pool, false);
    }

    public static ASN1ObjectIdentifier checkHeader(Asn1Parser parser, String oid) throws IOException, PolyPseudoException {
        parser.readObject(DLSequenceParser.class);
        final ASN1ObjectIdentifier header = parser.readObject(ASN1ObjectIdentifier.class);
        if (oid != null && oid.equals(header.getId())) {
            throw new CryptoError(String.format("Unexpected object identifier, expected %s, got %s", oid, header));
        }
        return header;
    }

    protected byte[] unsign(byte[] signedPolymorph) {
        try {
            final Asn1Parser parser = new Asn1Parser(signedPolymorph);
            checkHeader(parser, null);
            parser.readObject();
            return parser.readObject().toASN1Primitive().getEncoded();
        } catch (IOException | PolyPseudoException e) {
            logger.error("Error unsigning message", e);
            throw new CryptoError("Could not unsign signed messsage", e);
        }
    }
}
