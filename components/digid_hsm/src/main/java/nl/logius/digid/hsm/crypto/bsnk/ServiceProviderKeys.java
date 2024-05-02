
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
import nl.logius.digid.hsm.crypto.BsnkAction;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import org.bouncycastle.asn1.*;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServiceProviderKeys extends BsnkAction {

    private static final int TAG_SERVICE_PROVIDER = 3;
    private static final int TAG_PARTICIPANT_KEY_SET_VERSION = 0;
    private static final int TAG_PARTICIPANT_CLOSING_KEY_VERSION = 1;
    private static final int TAG_PARTICIPANT_CERTIFICATE = 2;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final static String[] KEYS = {"ID", "PD", "PC", "DRKi"};

    public ServiceProviderKeys(ConnectionPool pool) {
        super(pool);
    }

    public Map<String,byte[]> getKeys(ServiceProviderKeysInput input, boolean pseudonym) {
        final byte[] response;
        Map<String, byte[]> result;

        try {
            final byte[] request = encode(input);
            if (logger.isDebugEnabled()) {
                logger.debug("Request: {}", Hex.toHexString(request));
            }
            response = pseudonym ? getConnection().getSpPKeys(request) : getConnection().getSpIKeys(request);
            if (logger.isDebugEnabled()) {
                logger.debug("Response: {}", Hex.toHexString(request));
            }
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error fetching service provider keys", e);
            throw new CryptoError("Could not get service provider keys", e);
        }
        result = decode(response);
        try {
            result.put("DRKi", decode(getConnection().getDRKi(encode(input))).get("DRKi"));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error fetching DRKi", e);
            throw new CryptoError("Could not get DRKi", e);
        }
        return result;
    }

    private Map<String, byte[]> decode(byte[] response) {
        final Map<String, byte[]> result = new HashMap<>(2);
        ASN1InputStream parser = new ASN1InputStream(response);

        try {
            DLSequence seq = (DLSequence) parser.readObject();
            DLSequence keySeq = (DLSequence) seq.getObjectAt(4);

            for (int i = 0; i < keySeq.size(); i++) {
                ASN1TaggedObject to = (ASN1TaggedObject) keySeq.getObjectAt(i);
                final String key;
                try {
                    key = KEYS[to.getTagNo()];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CryptoError(String.format("Unknown tag %d", to.getTagNo()));
                }
                result.put(key, ((ASN1OctetString) to.getObject()).getOctets());
            }
        } catch (IOException e) {
            throw new CryptoError("Could not decode response from HSM", e);
        }
        return result;
    }

    private byte[] encode(ServiceProviderKeysInput input) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(new ASN1Integer(input.getSchemeVersion()));
            sequence.addObject(new ASN1Integer(input.getSchemeKeyVersion()));
            sequence.addObject(new DERTaggedObject(false, TAG_SERVICE_PROVIDER, participantSequence(input)));

            sequence.close();
        } catch (IOException e) {
            throw new CryptoError("Could not encode request for HSM", e);
        }

        return buffer.toByteArray();
    }

    private DERSequence participantSequence(ServiceProviderKeysInput input) throws IOException {
        final ASN1EncodableVector items = new ASN1EncodableVector();
        items.add(new DERIA5String(input.getServiceProvider()));
        items.add(new DERTaggedObject(false, TAG_PARTICIPANT_KEY_SET_VERSION, new ASN1Integer(input.getServiceProviderKeySetVersion())));
        if (input.getClosingKeyVersion() > 0) {
            items.add(new DERTaggedObject(false, TAG_PARTICIPANT_CLOSING_KEY_VERSION, new ASN1Integer(input.getClosingKeyVersion())));
        }
        items.add(new DERTaggedObject(false, TAG_PARTICIPANT_CERTIFICATE, certificateSequence(input)));

        return new DERSequence(items);
    }

    private DERSequence certificateSequence(ServiceProviderKeysInput input) throws IOException {
        return new DERSequence(new ASN1Encodable[] {
                new ASN1Integer(input.getCertificate().getSerialNumber()),
                new DEROctetString(input.getCertificate().getIssuer().getEncoded()),
                new DEROctetString(input.getCertificate().getSubjectPublicKeyInfo().parsePublicKey().getEncoded())
        });
    }
}
