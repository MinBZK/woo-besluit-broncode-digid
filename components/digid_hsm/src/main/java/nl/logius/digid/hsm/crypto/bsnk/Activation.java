
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CryptoServerAPI.CryptoServerException;

import nl.logius.digid.hsm.crypto.BsnkAction;
import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.ActivateRequest;
import nl.logius.digid.hsm.model.BaseActivateRequest;

public class Activation extends BsnkAction {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Activation(ConnectionPool pool) {
        super(pool);
    }

    public byte[] activate(ActivateRequest request) {
        try {
            final Connection connection = getConnection();
            final byte[] input = encode(request);;
            final byte[] output;
            switch (request.getType()) {
            case PI:
                output = connection.activateGetSignedPi(input);
                break;
            case PP:
                output = connection.activateGetSignedPp(input);
                break;
            case PIP:
                output = connection.activateGetSignedPip(input);
                break;
            default:
                throw new CryptoError(String.format("Unknown type: %s", request.getType()));
            }
            return request.isSigned() ? output : unsign(output);
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error activate", e);
            throw new CryptoError("Error activate", e);
        }
    }

    protected byte[] encode(BaseActivateRequest request) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(new ASN1Integer(request.getSchemeKeyVersion()));
            sequence.addObject(new DERIA5String(request.getIdentifier()));
            sequence.addObject(new ASN1Integer(request.getIdentifierType()));
            sequence.addObject(new ASN1Integer(request.getSignKeyVersion()));

            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getIdentityProvider(request)))) {
                sequence.addObject(new DLTaggedObject(false, 0, asn1Stream.readObject()));
            }

            sequence.close();
        } catch (IOException e) {
            logger.error("Could not encode request for HSM", e);
            throw new CryptoError("Could not encode request for HSM", e);
        }

        return buffer.toByteArray();
    }

    private byte[] getIdentityProvider(BaseActivateRequest request) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new DERIA5String(request.getIdentityProvider()));
        derSequencer.addObject(new DLTaggedObject(false, 0, new ASN1Integer(request.getIdentityProviderKeySetVersion())));

        derSequencer.close();
        return buffer.toByteArray();
    }
}
