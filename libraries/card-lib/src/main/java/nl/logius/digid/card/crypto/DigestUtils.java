
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

package nl.logius.digid.card.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.AlgorithmNameFinder;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;

public final class DigestUtils {
    private static final AlgorithmNameFinder ALGORITHM_FINDER = new DefaultAlgorithmNameFinder();
    private static final Pattern WITH_PATTERN = Pattern.compile(
        "([a-zA-Z_0-9-]+)with[a-zA-Z_0-9-]+", Pattern.CASE_INSENSITIVE);

    private DigestUtils() {}

    /**
     * Returns message digest object for given algorithm
     *
     * @param algorithm name or object identifier of the algorithm
     * @return message digest object
     */
    public static MessageDigest digest(String algorithm) {
        final Matcher matcher = WITH_PATTERN.matcher(algorithm);
        final String digestAlgorithm = matcher.matches() ? matcher.group(1) : algorithm;
        try {
            return MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("Invalid algorithm", e);
        }
    }

    /**
     * Returns message digest object for given algorithm
     *
     * @param oid object identifier of hash
     * @return message digest object
     */
    public static MessageDigest digest(ASN1ObjectIdentifier oid) {
        return digest(ALGORITHM_FINDER.getAlgorithmName(oid));
    }

    /**
     * Returns message digest object for given algorithm
     *
     * @param id algorithm identifier
     * @return message digest object
     */
    public static MessageDigest digest(AlgorithmIdentifier id) {
        return digest(ALGORITHM_FINDER.getAlgorithmName(id));
    }
}

