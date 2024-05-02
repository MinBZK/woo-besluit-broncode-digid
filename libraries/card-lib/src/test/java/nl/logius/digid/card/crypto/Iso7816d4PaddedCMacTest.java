
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

import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Test;

public class Iso7816d4PaddedCMacTest {
    @Test
    public void shouldCreateSameOutcomeAsPaddedNormalCMac() {
        final byte[] key = CryptoUtils.random(32);
        for (int i = 16; i < 32; i++) {
            final byte[] data = CryptoUtils.random(i);
            final byte[] padded = new byte[32];

            System.arraycopy(data, 0, padded, 0, data.length);
            new ISO7816d4Padding().addPadding(padded, data.length);

            assertArrayEquals(
                String.format("CMac not equal for size %d", i),
                doMac(new CMac(new AESEngine(), 64), key, padded),
                doMac(new ISO7816d4PaddedCMac(new AESEngine(), 64), key, data)
            );
        }
    }

    private static byte[] doMac(Mac mac, byte[] key, byte[] data) {
        return MacProxy.calculate(mac, new KeyParameter(key), (m) -> m.update(data));
    }
}
