
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

package nl.logius.digid.hsm.crypto.keys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.hsm.crypto.KeysAction;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.SignatureType;

import CryptoServerAPI.CryptoServerException;

public class Signing extends KeysAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Signing(ConnectionPool pool) {
        super(pool, false);
    }

    public byte[] sign(String group, String name, String hash, byte[] data, SignatureType type) {
        try {
            final byte[] raw = getConnection().sign(group, name, digest(hash, data));
            return type == SignatureType.ASN1 ? encode(raw) : raw;
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error(String.format("Error signing data for <%s,%s>", group, name), e);
            throw new CryptoError("Error signing data", e);
        }
    }

    private byte[] digest(String hash, byte[] data) {
        if (hash == null) {
            return data;
        }

        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(hash);
        } catch (GeneralSecurityException e) {
            logger.error(String.format("Error creating hash '%s'", hash), e);
            throw new CryptoError(String.format("Error creating hash '%s'", hash));
        }
        return digest.digest(data);
    }

    private byte[] encode(byte[] raw) {
        final EcSignature signature = new EcSignature(raw);
        final DERSequence seq = new DERSequence(new ASN1Encodable[] {
            new ASN1Integer(signature.r), new ASN1Integer(signature.s),
        });
        try {
            return seq.getEncoded();
        } catch (IOException e) {
            logger.error(String.format("Error encoding ASN1 sequence of signature"), e);
            throw new CryptoError("Error encoding ASN1 sequence of signature", e);
        }
    }
}
