
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

import com.google.common.io.BaseEncoding;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import nl.logius.digid.pp.crypto.CMS;
import nl.logius.digid.pp.key.DecryptKey;
import nl.logius.digid.sharedlib.client.BsnkClient;
import nl.logius.digid.sharedlib.client.HsmClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class BsnkPseudonymDecryptor {
    private final String pseudonymDecryptKey;
    private final String pseudonymClosingKey;
    private final String directDecryptKey;
    private final BsnkClient bsnkClient;
    private final String targetClosingKey;

    public BsnkPseudonymDecryptor(BsnkClient bsnkClient, String pseudonymDecryptKey, String pseudonymClosingKey, String directDecryptKey, String targetClosingKey) {
        this.bsnkClient = bsnkClient;
        this.pseudonymDecryptKey = pseudonymDecryptKey;
        this.pseudonymClosingKey = pseudonymClosingKey;
        this.directDecryptKey = directDecryptKey;
        this.targetClosingKey = targetClosingKey;
    }

    public static BsnkPseudonymDecryptor createBSNKPseudonymDecryptor(HsmClient hsmClient, BsnkClient bsnkClient,
                                                                      PrivateKey privateKey,
                                                                      X509Certificate certificate,
                                                                      int closingKeyVersion) throws IOException {

        final HsmClient.ServiceProviderKeys keys = hsmClient.fetchDecryptKeys(
            certificate, closingKeyVersion, true, false);
        final String decryptKey;
        final String closingKey;
        final String depDecryptKey;

        try (final InputStream is = new ByteArrayInputStream(keys.getPseudonymDecrypt())) {
            decryptKey = CMS.read(privateKey, is);
        }

        try (final InputStream is = new ByteArrayInputStream(keys.getPseudonymClosing())) {
            closingKey = CMS.read(privateKey, is);
        }

        try (final InputStream is = new ByteArrayInputStream(keys.getDirectPseudonymDecrypt())) {
            depDecryptKey = CMS.read(privateKey, is);
        }
        final String targetClosingKey = String.valueOf(DecryptKey.fromPem(closingKey).getRecipientKeySetVersion());
        return new BsnkPseudonymDecryptor(bsnkClient, decryptKey, closingKey, depDecryptKey, targetClosingKey);
    }


    public String decryptDep(String directEncryptedPseudonym, String schemeKeyVersion, String schemeKey, String authorizedParty) {
        return pseudonymFormat(bsnkClient.decryptDep(directEncryptedPseudonym,
            List.of(pseudonymDecryptKey, pseudonymClosingKey, directDecryptKey),
            schemeKeyVersion, schemeKey, targetClosingKey, authorizedParty)
        );
    }

    public String decryptEp(String encryptedPseudonym, String schemeKeyVersion, String schemeKey) {
        return pseudonymFormat(bsnkClient.decryptEp(
            encryptedPseudonym,
            List.of(pseudonymDecryptKey, pseudonymClosingKey),
            schemeKeyVersion, schemeKey,targetClosingKey)
        );
    }

    private String pseudonymFormat(String pseudonym) {
        var point = BrainpoolP320r1.CURVE.decodePoint(Base64.getDecoder().decode(pseudonym));
        return String.format("%s|%s", targetClosingKey, BaseEncoding.base64().encode(point.getEncoded(true)));
    }
}
