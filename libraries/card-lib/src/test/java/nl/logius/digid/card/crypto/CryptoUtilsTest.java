
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

import static org.junit.Assert.assertArrayEquals;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

public class CryptoUtilsTest {

    @Test
    public void adjustParity() {
        assertArrayEquals(Hex.decode("010102020404070708080b0b0d0d0e0e101013131515161619191a1a1c1c1f1f"),
            CryptoUtils.adjustParity(
                Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
            )
        );
        assertArrayEquals(Hex.decode("202023232525262629292a2a2c2c2f2f313132323434373738383b3b3d3d3e3e"),
            CryptoUtils.adjustParity(
                Hex.decode("202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f")
            )
        );
        assertArrayEquals(Hex.decode("404043434545464649494a4a4c4c4f4f515152525454575758585b5b5d5d5e5e"),
            CryptoUtils.adjustParity(
                Hex.decode("404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f")
            )
        );
        assertArrayEquals(Hex.decode("616162626464676768686b6b6d6d6e6e707073737575767679797a7a7c7c7f7f"),
            CryptoUtils.adjustParity(
                Hex.decode("606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f")
            )
        );
        assertArrayEquals(Hex.decode("808083838585868689898a8a8c8c8f8f919192929494979798989b9b9d9d9e9e"),
            CryptoUtils.adjustParity(
                Hex.decode("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f")
            )
        );
        assertArrayEquals(Hex.decode("a1a1a2a2a4a4a7a7a8a8ababadadaeaeb0b0b3b3b5b5b6b6b9b9bababcbcbfbf"),
            CryptoUtils.adjustParity(
                Hex.decode("a0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebf")
            )
        );
        assertArrayEquals(Hex.decode("c1c1c2c2c4c4c7c7c8c8cbcbcdcdceced0d0d3d3d5d5d6d6d9d9dadadcdcdfdf"),
            CryptoUtils.adjustParity(
                Hex.decode("c0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedf")
            )
        );
        assertArrayEquals(Hex.decode("e0e0e3e3e5e5e6e6e9e9eaeaececefeff1f1f2f2f4f4f7f7f8f8fbfbfdfdfefe"),
            CryptoUtils.adjustParity(
                Hex.decode("e0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff")
            )
        );
    }
}
