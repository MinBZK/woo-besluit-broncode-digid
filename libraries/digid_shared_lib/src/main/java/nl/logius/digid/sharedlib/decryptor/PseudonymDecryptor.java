
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

package nl.logius.digid.sharedlib.decryptor;

import nl.logius.digid.pp.crypto.CMS;
import nl.logius.digid.pp.entity.DirectEncryptedPseudonym;
import nl.logius.digid.pp.entity.EncryptedEntity;
import nl.logius.digid.pp.entity.EncryptedPseudonym;
import nl.logius.digid.pp.entity.Pseudonym;
import nl.logius.digid.pp.key.DecryptKey;
import nl.logius.digid.pp.key.DirectPseudonymDecryptKey;
import nl.logius.digid.pp.key.EncryptedVerifiers;
import nl.logius.digid.pp.key.PseudonymClosingKey;
import nl.logius.digid.pp.key.PseudonymDecryptKey;
import nl.logius.digid.sharedlib.client.HsmClient;
import nl.logius.digid.sharedlib.exception.DecryptException;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class PseudonymDecryptor {
    private final PseudonymDecryptKey decryptKey;
    private final PseudonymClosingKey closingKey;
    private final DirectPseudonymDecryptKey depDecryptKey;
    private final EncryptedVerifiers epVerifiers;
    private final EncryptedVerifiers depVerifiers;

    private final Logger logger;

    public PseudonymDecryptor(PseudonymDecryptKey decryptKey, PseudonymClosingKey closingKey, DirectPseudonymDecryptKey depDecryptKey, String verificationPoint, String U) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this.decryptKey = decryptKey;
        this.closingKey = closingKey;
        this.depDecryptKey = depDecryptKey;
        this.epVerifiers = decryptKey.toVerifiers(verificationPoint);
        this.depVerifiers = depDecryptKey.toVerifiers(U);
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public static PseudonymDecryptor fromHsm(HsmClient hsmClient, PrivateKey privateKey, X509Certificate certificate,
                                             int closingKeyVersion, String U) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        final HsmClient.ServiceProviderKeys keys = hsmClient.fetchDecryptKeys(
            certificate, closingKeyVersion, true, false);
        final PseudonymDecryptKey decryptKey;
        final PseudonymClosingKey closingKey;
        final DirectPseudonymDecryptKey depDecryptKey;

        try (final InputStream is = new ByteArrayInputStream(keys.getPseudonymDecrypt())) {
            decryptKey = DecryptKey.fromPem(CMS.read(privateKey, is), PseudonymDecryptKey.class);
        }

        try (final InputStream is = new ByteArrayInputStream(keys.getPseudonymClosing())) {
            closingKey = DecryptKey.fromPem(CMS.read(privateKey, is), PseudonymClosingKey.class);
        }

        try (final InputStream is = new ByteArrayInputStream(keys.getDirectPseudonymDecrypt())) {
            depDecryptKey = DecryptKey.fromPem(CMS.read(privateKey, is), DirectPseudonymDecryptKey.class);
        }


        return new PseudonymDecryptor(decryptKey, closingKey, depDecryptKey, hsmClient.fetchVerificationPoints().getPseudonym(), U);
    }

    public Pseudonym decrypt(byte[] encrypted) {
        try {
            final EncryptedPseudonym ep = EncryptedEntity.fromBytes(encrypted, epVerifiers, EncryptedPseudonym.class);
            return ep.decrypt(decryptKey, closingKey);
        } catch (Exception e) {
            final String msg = String.format("Could not decrypt %s", Base64.toBase64String(encrypted));
            logger.warn(msg, e);
            throw new DecryptException(msg, e);
        }
    }

    public Pseudonym decrypt(String encrypted) {
        try {
            final EncryptedPseudonym ep = EncryptedEntity.fromBase64(encrypted, epVerifiers, EncryptedPseudonym.class);
            return ep.decrypt(decryptKey, closingKey);
        } catch (Exception e) {
            logger.warn(String.format("Could not decrypt %s", encrypted), e);
            throw new DecryptException(String.format("Could not decrypt %s", encrypted), e);
        }
    }

    public Pseudonym decryptDep(String encrypted) {
        try {
            final DirectEncryptedPseudonym ep = EncryptedEntity.fromBase64(encrypted, depVerifiers, DirectEncryptedPseudonym.class);
            return ep.decrypt(decryptKey, closingKey, depDecryptKey);
        } catch (Exception e) {
            logger.warn(String.format("Could not decrypt %s", encrypted), e);
            throw new DecryptException(String.format("Could not decrypt %s", encrypted), e);
        }
    }

    public Pseudonym decryptDep(byte[] encrypted) {
        try {
            final DirectEncryptedPseudonym ep = EncryptedEntity.fromBytes(encrypted, depVerifiers, DirectEncryptedPseudonym.class);
            return ep.decrypt(decryptKey, closingKey, depDecryptKey);
        } catch (Exception e) {
            final String msg = String.format("Could not decrypt %s", Base64.toBase64String(encrypted));
            logger.warn(msg, e);
            throw new DecryptException(msg, e);
        }
    }
}
