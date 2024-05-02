
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

package nl.logius.digid.card.apdu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.card.ByteArrayUtils;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.crypto.CipherProxy;
import nl.logius.digid.card.crypto.CryptoException;
import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.card.crypto.MacProxy;

public abstract class SecureMessaging {
    public static final int MAX_COMMAND_SIZE = 223;
    public static final int MAX_RESPONSE_SIZE = 231;
    private static final Asn1ObjectMapper MAPPER = new Asn1ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeyParameter kEnc;
    private final KeyParameter kMac;
    private final byte[] ssc;

    protected SecureMessaging(byte[] kEnc, byte[] kMac, byte[] ssc) {
        this.kEnc = new KeyParameter(kEnc);
        this.kMac = new KeyParameter(kMac);
        this.ssc = ssc == null ? null : ssc.clone();
    }

    public byte[] getSsc() {
        return ssc.clone();
    }

    protected abstract int blockSize();

    protected abstract BlockCipher cipher();

    protected abstract Mac mac();

    protected abstract CipherParameters params(KeyParameter keyParam, boolean forCommand);

    public CommandAPDU encrypt(CommandAPDU cmd) {
        if (logger.isDebugEnabled()) {
            logger.debug("Plain command: {}", ByteArrayUtils.prettyHex(cmd.getBytes()));
        }
        final CommandAPDU encrypted;

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (cmd.getNc() != 0) {
                createTag87(bos, true, cmd.getData());
            }
            if (cmd.getNe() != 0) {
                createTag97(bos, cmd.getNe());
            }
            createTag8e(bos, mac((m) -> {
                m.update(commandSsc());
                m.update(paddedHeader(cmd, blockSize()));
                m.update(bos.toByteArray());
            }));

            encrypted = new CommandAPDU(
                cmd.getCLA() | 0x0C, cmd.getINS(), cmd.getP1(), cmd.getP2(), bos.toByteArray(), 0x100
            );
        } catch (IOException e) {
            throw new SecureMessagingException("Unexpected IO exception", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Encrypted command: {}", ByteArrayUtils.prettyHex(encrypted.getBytes()));
        }
        return encrypted;
    }

    public ResponseAPDU encrypt(ResponseAPDU res) {
        if (logger.isDebugEnabled()) {
            logger.debug("Plain response: {}", ByteArrayUtils.prettyHex(res.getBytes()));
        }
        final ResponseAPDU encrypted;

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (res.getData() != null) {
                createTag87(bos, false, res.getData());
            }
            createTag99(bos, res.getSW());
            createTag8e(bos, mac((m) -> {
                m.update(responseSsc());
                m.update(bos.toByteArray());
            }));

            encrypted = createResponseAPDU(bos.toByteArray(), res.getSW());
        } catch (IOException e) {
            throw new SecureMessagingException("Unexpected IO exception", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Encrypted response: {}", ByteArrayUtils.prettyHex(encrypted.getBytes()));
        }
        return encrypted;
    }

    private void createTag87(ByteArrayOutputStream bos, boolean forCommand, byte[] data) throws IOException {
        try (final Asn1OutputStream out = new Asn1OutputStream(bos, 0x87)) {
            out.write(1);
            out.write(encrypt(true, forCommand, data, 0, data.length));
        }
    }

    private void createTag97(ByteArrayOutputStream bos, int value) {
        bos.write(0x97);
        bos.write(1);
        bos.write(value);
    }

    private void createTag99(ByteArrayOutputStream bos, int value) {
        bos.write(0x99);
        bos.write(2);
        bos.write(value >>> 8);
        bos.write(value & 0xff);
    }

    private void createTag8e(ByteArrayOutputStream bos, byte[] data) throws IOException {
        try (final Asn1OutputStream out = new Asn1OutputStream(bos, 0x8e)) {
            out.write(data);
        }
    }

    public CommandAPDU decrypt(CommandAPDU cmd) {
        if (logger.isDebugEnabled()) {
            logger.debug("Encrypted response: {}", ByteArrayUtils.prettyHex(cmd.getBytes()));
        }

        final byte[] data = cmd.getData();
        if (data == null || data.length == 0) {
            throw new SecureMessagingException("Cannot decrypt, no data");
        }
        final EncryptedCommandAPDU command = MAPPER.read(data, EncryptedCommandAPDU.class);

        final byte[] cc = mac((m) -> {
            m.update(commandSsc());
            m.update(paddedHeader(cmd, blockSize()));
            m.update(data, 0, data.length - command.cc.length - 2); // Assumes cc.length < 128
        });

        if (!CryptoUtils.compare(cc, command.cc, 0)) {
            throw new SecureMessagingException("Calculated MAC not equal");
        }

        final byte[] decrypted;
        if (command.data == null) {
            decrypted = null;
        } else {
            decrypted = decrypt(true, true, command.data, 1, command.data.length - 1);
        }

        final CommandAPDU plain = new CommandAPDU(
            cmd.getCLA() & ~0x0c, cmd.getINS(), cmd.getP1(), cmd.getP2(), decrypted, command.ne);
        if (logger.isDebugEnabled()) {
            logger.debug("Plain command: {}", ByteArrayUtils.prettyHex(plain.getBytes()));
        }
        return plain;
    }

    public ResponseAPDU decrypt(ResponseAPDU res) {
        if (logger.isDebugEnabled()) {
            logger.debug("Encrypted response: {}", ByteArrayUtils.prettyHex(res.getBytes()));
        }

        final byte[] data = res.getData();
        if (data == null || data.length == 0) {
            throw new SecureMessagingException("Cannot decrypt, no data");
        }
        final EncryptedResponseAPDU response = MAPPER.read(data, EncryptedResponseAPDU.class);

        final byte[] cc = mac((m) -> {
            m.update(responseSsc());
            m.update(data, 0, data.length - response.cc.length - 2); // Assumes cc.length < 128
        });

        if (!CryptoUtils.compare(cc, response.cc, 0)) {
            throw new SecureMessagingException("Calculated MAC not equal");
        }

        final byte[] decrypted;
        if (response.data == null) {
            decrypted = null;
        } else {
            decrypted = decrypt(true, false, response.data, 1, response.data.length - 1);
        }

        final ResponseAPDU plain = createResponseAPDU(decrypted, response.sw);
        if (logger.isDebugEnabled()) {
            logger.debug("Plain response: {}", ByteArrayUtils.prettyHex(plain.getBytes()));
        }
        return plain;
    }

    public byte[] decrypt(boolean padding, boolean forCommand, byte[] data, int offset, int length) {
        return SmCipherProxy.decrypt(cipher(), params(kEnc, forCommand), padding, length, (c) -> {
            c.update(data, offset, length);
        });
    }

    public byte[] encrypt(boolean padding, boolean forCommand, byte[] data, int offset, int length) {
        return SmCipherProxy.encrypt(cipher(), params(kEnc, forCommand), padding, length, (c) -> {
            c.update(data, offset, length);
        });
    }

    public byte[] decrypt(boolean padding, boolean forCommand, byte[]... parts) {
        final int length = java.util.Arrays.stream(parts).mapToInt( (p) -> p.length ).sum();
        return SmCipherProxy.decrypt(cipher(), params(kEnc, forCommand), padding, length, (c) -> {
            for (final byte[] part : parts) {
                c.update(part);
            }
        });
    }

    public byte[] encrypt(boolean padding, boolean forCommand, byte[]... parts) {
        final int length = java.util.Arrays.stream(parts).mapToInt( (p) -> p.length ).sum();
        return SmCipherProxy.encrypt(cipher(), params(kEnc, forCommand), padding, length, (c) -> {
            for (final byte[] part : parts) {
                c.update(part);
            }
        });
    }

    public byte[] mac(Consumer<MacProxy> consumer) {
        return MacProxy.calculate(mac(), kMac, consumer);
    }

    protected byte[] commandSsc() {
        return ByteArrayUtils.plus(ssc, 1);
    }

    protected byte[] responseSsc() {
        return ByteArrayUtils.plus(ssc, 2);
    }

    public void next() {
        ByteArrayUtils.add(ssc, 2);
    }

    private static byte[] paddedHeader(CommandAPDU command, int size) {
        final byte[] result = new byte[size];
        result[0] = (byte) (command.getCLA() | 0x0C);
        result[1] = (byte) command.getINS();
        result[2] = (byte) command.getP1();
        result[3] = (byte) command.getP2();
        new ISO7816d4Padding().addPadding(result, 4);
        return result;
    }

    private static ResponseAPDU createResponseAPDU(byte[] data, int sw) {
        final int l = data == null ? 0 : data.length;
        final byte[] payload = new byte[l + 2];

        if (l > 0) {
            System.arraycopy(data, 0, payload, 0, l);
        }
        payload[l] = (byte) (sw >>> 8);
        payload[l+1] = (byte) (sw & 0xff);
        return new ResponseAPDU(payload);
    }

    private static class SmCipherProxy extends CipherProxy {
        private boolean unpadOut;

        private static byte[] decrypt(BlockCipher cipher, CipherParameters params, boolean padding, int length,
                                      Consumer<CipherProxy> consumer) {
            final BufferedBlockCipher bufferedCipher = new BufferedBlockCipher(cipher);
            bufferedCipher.init(false, params);

            final SmCipherProxy proxy = new SmCipherProxy(bufferedCipher, length, padding);
            consumer.accept(proxy);
            return proxy.finish();
        }

        private static byte[] encrypt(BlockCipher cipher, CipherParameters params, boolean padding, int length,
                                      Consumer<CipherProxy> consumer) {
            final BufferedBlockCipher bufferedCipher;
            if (padding) {
                bufferedCipher = new PaddedBufferedBlockCipher(cipher, new ISO7816d4Padding());
            } else {
                bufferedCipher = new BufferedBlockCipher(cipher);
            }
            bufferedCipher.init(true, params);

            final SmCipherProxy proxy = new SmCipherProxy(bufferedCipher, length, false);
            consumer.accept(proxy);
            return proxy.finish();
        }

        public SmCipherProxy(BufferedBlockCipher cipher, int length, boolean unpadOut) {
            super(cipher, length);
            this.unpadOut = unpadOut;
        }

        @Override
        protected byte[] finish() {
            final byte[] buffer = super.finish();
            if (unpadOut) {
                final int pc;
                try {
                    pc = new ISO7816d4Padding().padCount(buffer);
                } catch (InvalidCipherTextException e) {
                    throw new CryptoException("Invalid cipher text", e);
                }
                return Arrays.copyOfRange(buffer, 0, buffer.length - pc);
            } else {
                return buffer;
            }
        }
    }

    @Asn1Entity
    public static class EncryptedCommandAPDU {
        private byte[] data;
        private int ne;
        private byte[] cc;

        @Asn1Property(tagNo = 0x87, order = 1, optional = true)
        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Asn1Property(tagNo = 0x97, order = 2, optional = true)
        public int getNe() {
            return ne;
        }

        public void setNe(int ne) {
            this.ne = ne;
        }

        @Asn1Property(tagNo = 0x8e, order = 3)
        public byte[] getCc() {
            return cc;
        }

        public void setCc(byte[] cc) {
            this.cc = cc;
        }
    }

    @Asn1Entity
    public static class EncryptedResponseAPDU {
        private byte[] data;
        private int sw;
        private byte[] cc;

        @Asn1Property(tagNo = 0x87, order = 1, optional = true)
        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Asn1Property(tagNo = 0x99, order = 2)
        public int getSw() {
            return sw;
        }

        public void setSw(int sw) {
            this.sw = sw;
        }

        @Asn1Property(tagNo = 0x8e, order = 3)
        public byte[] getCc() {
            return cc;
        }

        public void setCc(byte[] cc) {
            this.cc = cc;
        }
    }
}
