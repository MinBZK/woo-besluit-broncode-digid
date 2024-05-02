
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

package nl.logius.digid.hsm.cryptoserver;

import static CryptoServerCXI.CryptoServerCXI.FLAG_EXTERNAL;
import static CryptoServerCXI.CryptoServerCXI.KEY_ALGO_ECDSA;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import CryptoServerAPI.CryptoServerException;
import CryptoServerCXI.CryptoServerCXI;
import CryptoServerCXI.CryptoServerCXI.Key;
import CryptoServerCXI.CryptoServerCXI.KeyAttributes;
import CryptoServerCXI.CryptoServerCXI.KeyComponents;
import nl.logius.digid.hsm.exception.NotFoundError;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;

/**
 * This class represents a single connection with the HSM via CXI interface
 *
 * No cluster mode is used, that is provided by the application itself.
 * Also no keep alive is used for several reasons:
 * - CXI uses a thread per connection for keep alive
 * - We use a connection pool, which is tested for aliveness
 * - If the eviction time is used (default), we are keeping the connection alive
 * by testing
 */
public class Connection implements Closeable {
    private static final String CURVE = "brainpoolP320r1";

    private static final int BSNK_MODULE_ID = 0x110;

    private static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PI = 32;
    private static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PP = 34;
    private static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP = 36;
    private static final int BSNK_FUNC_TRANSFORM_PI = 41;
    private static final int BSNK_FUNC_TRANSFORM_PP = 43;
    private static final int BSNK_FUNC_TRANSFORM_SIGNED_PIP_EX = 48;
    private static final int BSNK_FUNC_GET_SP_DEP_RECEIVE_KEY = 51;
    private static final int BSNK_FUNC_GET_SP_I_KEYS = 54;
    private static final int BSNK_FUNC_GET_SP_P_KEYS = 55;
    private static final int BSNK_FUNC_GET_PUBLIC_KEY = 59;

    private final CryptoServerCXI cxi;
    private final String tag;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Returns true if exception is due to broken connection
     */
    public static boolean isBroken(CryptoServerException exception) {
        switch (exception.ErrorCode) {
            case 0xB0830022: // secure messaging session expired
            case 0xB9800001: // Connection terminated by remote host
                return true;
            default:
                return false;
        }
    }

    public Connection(ConnectionConfig config, String tag) throws IOException, CryptoServerException {
        this(config.getAddress(), config.getTimeout(), tag);
        if (!StringUtils.isEmpty(config.getPwdUsername())) {
            this.logonPassword(config.getPwdUsername(), config.getPwdPassword());
        }
        if (!StringUtils.isEmpty(config.getKeyUsername())) {
            this.logonKey(config.getKeyUsername(), config.getKeyPath().toString(), config.getKeyPin());
        }
    }

    public Connection(String address, int timeout, String tag) throws IOException, CryptoServerException {
        logger.info("[{}] Connecting to: {}", tag, address);
        cxi = new CryptoServerCXI(address, timeout);
        this.tag = tag;
    }

    public Connection(String address, int timeout) throws IOException, CryptoServerException {
        this(address, timeout, null);
    }

    public void logonPassword(String username, String password) throws IOException, CryptoServerException {
        logger.info("[{}] Log on with password as user: {}", tag, username);
        logger.debug("[{}] Password: {}", tag, password);
        cxi.logonPassword(username, password);
    }

    public void logonKey(String username, String path, String pin) throws IOException, CryptoServerException {
        logger.info("[{}] Log on with key as user: {}", tag, username);
        logger.debug("[{}] Key path: {}, pin: {}", tag, path, pin);
        cxi.logonSign(username, path, pin);
    }

    @Override
    public String toString() {
        return cxi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Connection))
            return false;

        Connection that = (Connection) o;

        return cxi.equals(that.cxi);
    }

    @Override
    public int hashCode() {
        return cxi.hashCode();
    }

    @Override
    public void close() throws IOException {
        cxi.close();
    }

    public String getTag() {
        return tag;
    }

    private int getAuthState() throws IOException, CryptoServerException {
        return cxi.getAuthState();
    }

    public boolean test() throws IOException, CryptoServerException {
        return (getAuthState() & 0xf) >= 2;
    }

    /**
     * Backups key in Utimaco Backup Blob format from HSM encrypted with Master
     * Backup Key (MBK)
     *
     * @param group key group
     * @param name  key name
     * @return encrypted backup
     */
    public byte[] backupKey(String group, String name) throws IOException, CryptoServerException {
        return findKey(group, name, true).getEncoded();
    }

    /**
     * Restores key from Utimaco Backup Blob format into HSM
     *
     * @param data encrypted backup
     * @return public key in 04.. format
     */
    public byte[] restoreKey(byte[] data) throws IOException, CryptoServerException {
        final Key key = cxi.restoreKey(0, new Key(data), null);
        return cxi.getKeyAttributes(key, true).getECPub();
    }

    /**
     * Imports private key to Utimaco HSM (only used for development)
     *
     * @param group      key group
     * @param name       key name
     * @param privateKey private key bytes (interpreted as signed integer)
     */
    public void importKey(String group, String name, byte[] privateKey) throws IOException, CryptoServerException {
        final KeyAttributes attr = attributes(group, name);
        final KeyComponents comp = new KeyComponents();

        final BigInteger k = new BigInteger(1, privateKey);
        final ECPoint publicKey = BrainpoolP320r1.G.multiply(k).normalize();
        comp.add(KeyComponents.TYPE_PK, publicKey.getEncoded(false));
        comp.add(KeyComponents.TYPE_SK, privateKey);

        cxi.importClearKey(0, CryptoServerCXI.KEY_BLOB_SIMPLE, attr, comp);
    }

    /**
     * Generates private key in HSM
     *
     * @param group key group
     * @param name  key name
     * @return public key in 04.. format
     */
    public byte[] generateKey(String group, String name) throws IOException, CryptoServerException {
        final Key key = cxi.generateKey(0, attributes(group, name));
        return cxi.getKeyAttributes(key, true).getECPub();
    }

    /**
     * Signs data with private key inside HSM
     *
     * @param group key group
     * @param name  key name
     * @param data  bytes to sign
     * @return signature
     */
    public byte[] sign(String group, String name, byte[] data) throws IOException, CryptoServerException {
        final Key key = findKey(group, name, false);
        return cxi.sign(key, 0, data);
    }

    /**
     * Returns corresponding public key
     *
     * @param group key group
     * @param name  key name
     * @return public key in 04.. format
     */
    public byte[] publicKey(String group, String name) throws IOException, CryptoServerException {
        final Key key = findKey(group, name, false);
        return cxi.getKeyAttributes(key, true).getECPub();
    }

    /**
     * List key names of a group
     *
     * @param group key group
     * @return list of key names
     */
    public List<String> listKeys(String group) throws IOException, CryptoServerException {
        final KeyAttributes attr = new KeyAttributes();
        attr.setGroup(group);

        final KeyAttributes[] keys = cxi.listKeys(attr);
        final List<String> result = new ArrayList<>(keys.length);
        for (final KeyAttributes key : keys) {
            result.add(key.getName());
        }
        return result;
    }

    /**
     * Delete key
     *
     * @param group key group
     * @param name  key name
     */
    public void deleteKey(String group, String name) throws IOException, CryptoServerException {
        cxi.deleteKey(findKey(group, name, false));
    }

    private static KeyAttributes attributes(String group, String name) throws CryptoServerException {
        final KeyAttributes attr = new KeyAttributes();
        attr.setAlgo(KEY_ALGO_ECDSA);
        attr.setSize(0);
        attr.setCurve(CURVE);
        attr.setGroup(group);
        attr.setName(name);
        return attr;
    }

    private Key findKey(String group, String name, boolean external) throws IOException, CryptoServerException {
        final KeyAttributes attr = new KeyAttributes();
        attr.setGroup(group);
        attr.setName(name);

        final Key key = cxi.findKey(external ? FLAG_EXTERNAL : 0, attr);
        if (key == null) {
            throw new NotFoundError(
                    String.format("Could not find key of group '%s' and name '%s'", group, name));
        }
        return key;
    }

    public byte[] activateGetSignedPi(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PI, request);
    }

    public byte[] activateGetSignedPp(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PP, request);
    }

    public byte[] activateGetSignedPip(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP, request);
    }

    public byte[] transformPi(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PI, request);
    }

    public byte[] transformPp(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PP, request);
    }

    public byte[] transformSignedPipEx(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_EX, request);
    }

    public byte[] getSpIKeys(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_I_KEYS, request);
    }

    public byte[] getSpPKeys(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_P_KEYS, request);
    }

    public byte[] getDRKi(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_DEP_RECEIVE_KEY, request);
    }

    public byte[] getPublicKey(byte[] request) throws IOException, CryptoServerException {
        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_PUBLIC_KEY, request);
    }
}
