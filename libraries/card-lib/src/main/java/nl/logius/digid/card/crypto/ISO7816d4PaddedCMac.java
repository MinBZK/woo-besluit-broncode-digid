
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

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;

/**
 * CMac that pads with ISO7816d4
 *
 * BouncyCastle CMac also does ISO7816d4 padding, but with a different final XOR
 */
public class ISO7816d4PaddedCMac implements Mac {
    private final CMac proxy;
    private final int blockSize;
    private int size;

    public ISO7816d4PaddedCMac(BlockCipher cipher, int macSizeInBits) {
        proxy = new CMac(cipher, macSizeInBits);
        blockSize = cipher.getBlockSize();
    }

    @Override
    public String getAlgorithmName() {
        return proxy.getAlgorithmName();
    }

    @Override
    public void init(CipherParameters params) {
        proxy.init(params);
    }

    @Override
    public int getMacSize() {
        return proxy.getMacSize();
    }

    @Override
    public void update(byte in) {
        proxy.update(in);
        size++;
    }

    @Override
    public void update(byte[] in, int inOff, int len) {
        proxy.update(in, inOff, len);
        size += len;
    }

    @Override
    public int doFinal(byte[] out, int outOff) {
        addPadding();
        return proxy.doFinal(out, outOff);
    }

    @Override
    public void reset() {
        proxy.reset();
        size = 0;
    }

    /**
     * Add padding to a full block size
     */
    private void addPadding() {
        final byte[] padded = new byte[blockSize];
        final int offset = size % blockSize;
        new ISO7816d4Padding().addPadding(padded, offset);
        proxy.update(padded, offset, padded.length - offset);
    }
}
