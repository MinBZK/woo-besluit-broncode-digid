
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

package nl.logius.digid.rda.service;

import nl.logius.digid.card.apdu.SecureMessaging;
import nl.logius.digid.card.apdu.TDEASecureMessaging;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.LdsSecurityObject;
import nl.logius.digid.card.asn1.models.SOd;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.models.MrzInfo;
import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.models.card.AaPublicKey;
import nl.logius.digid.rda.models.card.COM;
import nl.logius.digid.rda.models.card.DataGroup1;
import nl.logius.digid.rda.models.card.NonMatch;

import java.security.MessageDigest;
import java.util.Date;

public class CardVerifier {
    private final Asn1ObjectMapper mapper;
    private final CmsVerifier cmsVerifier;
    private Date date = null;

    public CardVerifier(Asn1ObjectMapper mapper, CmsVerifier cmsVerifier) {
        this.mapper = mapper;
        this.cmsVerifier = cmsVerifier;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public byte[] verifyAuthenticate(byte[] seed, byte[] result) throws RdaException {
        final SecureMessaging sm = new TDEASecureMessaging(seed, 0, 16, null);

        final byte[] calculatedMac = sm.mac( m -> m.update(result, 0, 32));
        if (!CryptoUtils.compare(calculatedMac, result, 32)) {
            throw new RdaException(RdaError.AUTHENTICATE, "Invalid MAC");
        }

        return sm.decrypt(false, false, result, 0, 32);
    }

    public COM verifyCom(byte[] data, Class<? extends COM> type) throws RdaException {
        final COM com = read(data, type);
        if (!com.getDataGroups().containsAll(com.getRdaDataGroups())) {
            throw new RdaException(
                    RdaError.COM, String.format("Not all data groups are available: %s", com.getDataGroups())
            );
        }
        return com;
    }

    public LdsSecurityObject verifySOd(COM com, byte[] data) throws RdaException {
        final SOd sod = read(data, SOd.class);
        final LdsSecurityObject lso;
        try {
            final byte[] msg = cmsVerifier.verifyMessage(sod.getContentInfo(), date, LdsSecurityObject.OID);
            lso = read(msg, LdsSecurityObject.class);
        } catch (VerificationException e) {
            throw new RdaException(RdaError.PASSIVE_AUTHENTICATION, "Could not verify signed message", e);
        }
        if (!com.getDataGroups().equals(lso.getDigests().keySet())) {
            throw new RdaException(RdaError.PASSIVE_AUTHENTICATION,
                    "COM data groups not equal to SOd LDS security object");
        }
        return lso;
    }

    public void verifyDataGroup(COM com, LdsSecurityObject lso, byte[] dg) throws RdaException {
        final int no = com.getDataGroupOfTag(dg[0]);
        try {
            lso.verify(no, dg);
        } catch (VerificationException e) {
            throw new RdaException(
                    RdaError.PASSIVE_AUTHENTICATION, "Digest of data group " + no + " does not match", e
            );
        }
    }

    public <T> T verifyDataGroup(COM com, LdsSecurityObject lso, byte[] dg, Class<T> type) throws RdaException {
        verifyDataGroup(com, lso, dg);
        return read(dg, type);
    }

    public void verifyActiveAuthentication(AaPublicKey publicKey, MessageDigest digest, byte[] challenge,
                                           byte[] signature) throws RdaException{
        try {
            publicKey.verifier().verify(challenge, signature, digest);
        } catch (VerificationException e) {
            throw new RdaException(RdaError.ACTIVE_AUTHENTICATION, "Active authentication failed", e);
        }
    }

    public void verifyMrz(DataGroup1 dg1, MrzInfo info) throws RdaException {
        if (!dg1.getDocumentNumber().equals(info.getDocumentNumber())) {
            throw new RdaException(RdaError.MRZ_CHECK, "Input document number not equal to data group 1");
        }
        if (!dg1.getDateOfBirth().equals(info.getDateOfBirth())) {
            throw new RdaException(RdaError.MRZ_CHECK, "Input date of birth not equal to data group 1");
        }
        if (!dg1.getDateOfExpiry().equals(info.getDateOfExpiry())) {
            throw new RdaException(RdaError.MRZ_CHECK, "Input date of expiry not equal to data group 1");
        }
    }

    public void verifyMrz(NonMatch nonMatch, String inputMrz) throws RdaException {
        if (!nonMatch.getInputMrz().equals(inputMrz)) {
            throw new RdaException(RdaError.NON_MATCH, "Input MRZ not equal to non match");
        }
    }

    private <T> T read(byte[] data, Class<T> type) throws RdaException {
        try {
            return mapper.read(data, type);
        } catch (Asn1Exception e) {
            throw new RdaException(RdaError.PARSE_FILE, "ASN1 parsing error: " + e.getMessage(), e);
        }
    }
}
