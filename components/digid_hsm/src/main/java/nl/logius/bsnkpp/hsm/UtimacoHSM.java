
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

package nl.logius.bsnkpp.hsm;

import CryptoServerAPI.CryptoServerConfig;
import CryptoServerAPI.CryptoServerException;
import CryptoServerAPI.CryptoServerUtil;
import CryptoServerCXI.CryptoServerCXI;

import nl.logius.bsnkpp.kmp.KMPActivateRequest;
import nl.logius.bsnkpp.kmp.KMPKeyListEx;
import nl.logius.bsnkpp.kmp.KMPModuleInfo;
import nl.logius.bsnkpp.kmp.KMPModuleSetup;
import nl.logius.bsnkpp.kmp.KMPTransformRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import nl.logius.bsnkpp.kmp.KMPAuditElementDecryptRequest;
import nl.logius.bsnkpp.kmp.KMPException;
import nl.logius.bsnkpp.kmp.KMPKeyDef;
import nl.logius.bsnkpp.kmp.KMPKeyInfoEx;
import nl.logius.bsnkpp.kmp.KMPKeyList;
import nl.logius.bsnkpp.kmp.KMPKeyListRequest;
import nl.logius.bsnkpp.kmp.KMPKeyObjectValue;
import nl.logius.bsnkpp.kmp.KMPKeyRequestAttributes;
import nl.logius.bsnkpp.kmp.KMPKeyValue;
import nl.logius.bsnkpp.kmp.KMPMultiTransformRequest;
import nl.logius.bsnkpp.kmp.KMPParticipant;
import nl.logius.bsnkpp.kmp.KMPRecipientTransformInfo;
import nl.logius.bsnkpp.kmp.KMPUpdateFlagRequest;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DLSequence;

public class UtimacoHSM implements AutoCloseable {
    //private static final Logger LOGGER = Logger.getLogger(UtimacoHSM.class);

    public static final String SIG_OID_BSI_ECSCHNORR_SHA384 = "0.4.0.127.0.7.1.1.4.3.3";
    public static final String SIG_OID_BSI_ECSDSA_SHA384 = "0.4.0.127.0.7.1.1.4.4.3";

    static final int MDL_CMDS = 0x83;
    static final int BSNK_MODULE_ID = 0x110;

    public static char P_TYPE_BSN = 'B';      // See USvE
    public static char P_TYPE_EIDAS = 'E';    // See USvE

    //=================================================
    // FUNCTIONS
    //=================================================
    static final int BSNK_FUNC_RUN = 0;
    static final int BSNK_FUNC_MODULE_INIT = 1;
    static final int BSNK_FUNC_UPDATE_MODULE_FLAG = 2;

    static final int BSNK_FUNC_MODULE_INFO = 4;
    static final int BSNK_FUNC_LIST_KEYS = 5;
    static final int BSNK_FUNC_LIST_KEYS_EX = 6;

    static final int BSNK_FUNC_GENERATE_MASTER_KEYS = 10;
    static final int BSNK_FUNC_GENERATE_ACTIVATION_MASTER_KEYS = 11;
    static final int BSNK_FUNC_GENERATE_SIGNING_KEY_U = 12;
    static final int BSNK_FUNC_GENERATE_KEY = 13;
    static final int BSNK_FUNC_COPY_KEY = 14;

    static final int BSNK_FUNC_EXPORT_MASTER_KEYS = 16;
    static final int BSNK_FUNC_EXPORT_ACTIVATE_KEYS = 17;
    static final int BSNK_FUNC_EXPORT_TRANSFORM_KEYS = 18;
    static final int BSNK_FUNC_EXPORT_ACTIVATION_MASTER_KEYS = 19;
    static final int BSNK_FUNC_EXPORT_SIGNING_KEY_U = 20;
    static final int BSNK_FUNC_EXPORT_SP_PUBLIC_KEY = 21;
    static final int BSNK_FUNC_EXPORT_MSG_VERIFICATION_KEY = 22;
    static final int BSNK_FUNC_EXPORT_VPIP_VERIFICATION_KEY = 23;

    static final int BSNK_FUNC_IMPORT_MASTER_KEYS = 24;
    static final int BSNK_FUNC_IMPORT_ACTIVATION_MASTER_KEYS = 25;
    static final int BSNK_FUNC_IMPORT_ACTIVATE_KEYS = 26;
    static final int BSNK_FUNC_IMPORT_TRANSFORM_KEYS = 27;
    static final int BSNK_FUNC_IMPORT_SIGNING_KEY_U = 28;
    static final int BSNK_FUNC_IMPORT_SP_PUBLIC_KEY = 29;

    static final int BSNK_FUNC_ACTIVATE_GET_PI = 31;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PI = 32;
    static final int BSNK_FUNC_ACTIVATE_GET_PP = 33;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PP = 34;
    static final int BSNK_FUNC_ACTIVATE_GET_PIP = 35;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP = 36;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PI_PP_DEP = 37;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP_DEP = 38;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP_DEP_PP = 39;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_VPIP = 40;

    static final int BSNK_FUNC_TRANSFORM_PI = 41;
    static final int BSNK_FUNC_TRANSFORM_SIGNED_PI = 42;
    static final int BSNK_FUNC_TRANSFORM_PP = 43;
    static final int BSNK_FUNC_TRANSFORM_SIGNED_PP = 44;
    static final int BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PI = 46;
    static final int BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PP = 47;
    static final int BSNK_FUNC_TRANSFORM_SIGNED_PIP_EX = 48;

    //  static final int BSNK_FUNC_GET_SP_KEYS = 50;
    static final int BSNK_FUNC_GET_SP_DEP_RECEIVE_KEY = 51;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_DEI = 52;
    static final int BSNK_FUNC_ACTIVATE_GET_SIGNED_DEP = 53;
    static final int BSNK_FUNC_GET_SP_I_KEYS = 54;
    static final int BSNK_FUNC_GET_SP_P_KEYS = 55;
    static final int BSNK_FUNC_GET_PSEUDONYM_CONVERSION_KEY = 57;
    static final int BSNK_FUNC_GENERATE_TEST_KEYS = 58;
    static final int BSNK_FUNC_GET_PUBLIC_KEY = 59;

    static final int BSNK_FUNC_DELETE_KEY = 60;
    static final int BSNK_FUNC_DECRYPT_AUDIT_ELEMENT = 61;

    static final int BSNK_FUNC_EXPORT_POLYMORPH_MAPPING_KEY = 62;
    static final int BSNK_FUNC_IMPORT_POLYMORPH_MAPPING_KEY = 63;

    //=================================================
    // FLAGS
    //=================================================
    public static final int FLAG_KMA_FUNC = 0x01;
    public static final int FLAG_ACTIVATE_FUNC = 0x02;
    public static final int FLAG_TRANSFORM_FUNC = 0x04;
    public static final int FLAG_USE_V2_MESSAGES = 0x20; // Version 2 messages 

    //=================================================
    // ERROR CODES
    //=================================================
    static final int E_DB_EXISTS = 0xB0880005;

    static final int E_BSNK_GENERAL_ERROR = 0xB1100001;
    static final int E_BSNK_PERMISSION_DENIED = 0xB1100002;      // permission denied
    static final int E_BSNK_PARAM = 0xB1100003; // invalid parameter
    static final int E_BSNK_PARAM_LEN = 0xB1100004; // invalid parameter length
    static final int E_BSNK_MALLOC = 0xB1100005; // memory allocation failed
    static final int E_BSNK_MODE = 0xB1100006; // invalid mode
    static final int E_BSNK_ITEM_NOT_FOUND = 0xB1100007; // item not found
    static final int E_BSNK_MODULE_DEP = 0xB1100008; // unresolved module dependency
    static final int E_BSNK_FILE_IO = 0xB1100009; // file I/O error
    static final int E_BSNK_KEYS_NOT_FOUND = 0xB110000A; // file I/O error
    static final int E_BSNK_DATA_CORRUPT = 0xB110000B; // Something wrong with the data
    static final int E_BSNK_BUFFER_OVERFLOW = 0xB110000C; // Too much data for buffer
    static final int E_BSNK_MODULE_NOT_RUNNING = 0xB110000D;
    static final int E_BSNK_ECSCHNORR_BAD_SIGN_FMT = 0xB110000E;
    static final int E_BSNK_INVALID_MESSAGE_FORMAT = 0xB110000F; // (ASN1)message format invalid (can be content related)
    static final int E_BSNK_MODULE_NOT_INITIALIZED = 0xB1100010;
    static final int E_BSNK_INVALID_SCHEME_VERSION = 0xB1100011;
    static final int E_BSNK_INVALID_SCHEME_KEYSET_VERSION = 0xB1100012;
    static final int E_BSNK_MODULE_FUNCTION_LOCKED = 0xB1100013;
    static final int E_BSNK_FUNC_NOT_IMPLEMENTED = 0xB1100014;
    static final int E_BSNK_INVALID_MBK = 0xB1100015;
    static final int E_BSNK_KEYSTORE_NOT_PRESENT = 0xB1100016;
    static final int E_BSNK_IDENTITY_TO_ECPOINT = 0xB1100017;
    static final int E_BSNK_ECDSA_VERIFY_FAILED = 0xB1100018;
    static final int E_BSNK_AUTO_MIGRATION_FAILURE = 0xB1100019;

    static final int CMDS_LISTUSERS = 4;    // Utimaco function (see mdl_CMDS.pdf)
    static final int CMDS_GET_USER_INFO = 15;    // Utimaco function (see mdl_CMDS.pdf)

//    static final String SCHNORR_SIG_OID = SIG_OID_BSI_ECSCHNORR_SHA384;
//    static final String SCHNORR_SIG_OID = SIG_OID_BSI_ECSDSA_SHA384;
    private String activeSignature = null; //SIG_OID_BSI_ECSDSA_SHA384;

    private CryptoServerCXI cxi;

    public class UtimacoAuthState {

        public int user_manager = 0;
        public int hsm_manager = 0;
        public int crypto_user = 0;
    }

    public class User {

        String m_userName = "";
        int m_permission = 0;
        int m_authMethod = -1;   // 0 is keyfile, 4 is password

        /*
        00H:   RSA Signature Authentication 
        01H:   
        Clear Password Authentication 02H:   SHA-1 Hashed Password Authentication 03H:   RSA Smartcard Authentication 04H:   HMAC Password Authentication 05H:   ECDSA Signature Authentication 
        
         */
        public void setUsername(String name) {
            m_userName = name;
        }

        public String getUsername() {
            return m_userName;
        }

        public void setPermission(int perm) {
            m_permission = perm;
        }

        public int getPermission() {
            return m_permission;
        }

        public void setAuthenticationMechCode(int meth) {
            m_authMethod = meth;
        }

        public int getAuthenticationMechCode() {
            return m_authMethod;
        }

        public String getAuthenticationMech() {
            switch (m_authMethod) {
                case 0:
                    return "RSA Signature Authentication";
                case 1:
                    return "Clear Password Authentication";
                case 2:
                    return "SHA-1 Hashed Password Authentication";
                case 3:
                    return "RSA Smartcard Authentication";
                case 4:
                    return "HMAC Password Authentication";
                case 5:
                    return "ECDSA Signature Authentication";
            }

            return "Unknown";
        }
    }

    public UtimacoHSM() {
        cxi = null;
    }

    public UtimacoHSM(String device_address, int timeout) throws IOException, NumberFormatException, CryptoServerException {
        // create instance of CryptoServerCXI (opens connection to CryptoServer)
        cxi = new CryptoServerCXI(device_address, timeout);
    }

    @Override
    public void close() {
        disconnectFromHSM();
    }

    @Override
    public void finalize() throws Throwable {
        try {
            disconnectFromHSM();
        } finally {
            super.finalize();
        }
    }

    public static String getErrorMessageForId(int id) {
        String default_msg = "CryptoServer error";

        switch (id) {
            case E_DB_EXISTS:
                default_msg = "Keys or HSM data already exist in database";
                break;
            case E_BSNK_GENERAL_ERROR:
                default_msg = "Onbekende fout in BSNk PP module.";
                break;
            case E_BSNK_PERMISSION_DENIED:
                default_msg = "Unsufficient rights to execute function";
                break;
            case E_BSNK_PARAM:
                default_msg = "Invalid parameter";
                break;
            case E_BSNK_PARAM_LEN:
                default_msg = "Invalid parameter length";
                break;
            case E_BSNK_MALLOC:
                default_msg = "Unsufficient memory or array limit reached";
                break;
            case E_BSNK_MODE:
                default_msg = "Invalid mode";
                break;
            case E_BSNK_ITEM_NOT_FOUND:
                default_msg = "Item not found";
                break;
            case E_BSNK_MODULE_DEP:
                default_msg = "Module dependency not found";
                break;
            case E_BSNK_FILE_IO:
                default_msg = "Error reading/writing file";
                break;
            case E_BSNK_KEYS_NOT_FOUND:
                default_msg = "Keys or key set not found";
                break;
            case E_BSNK_DATA_CORRUPT:
                default_msg = "Error in data format";
                break;
            case E_BSNK_BUFFER_OVERFLOW:
                default_msg = "Buffer too small";
                break;
            case E_BSNK_MODULE_NOT_RUNNING:
                default_msg = "Module does not run";
                break;
            case E_BSNK_ECSCHNORR_BAD_SIGN_FMT:
                default_msg = "Invalid Schnorr signature";
                break;
            case E_BSNK_INVALID_MESSAGE_FORMAT:
                default_msg = "Error in message (format and/or content)";
                break;
            case E_BSNK_MODULE_NOT_INITIALIZED:
                default_msg = "BSNk-PP module not initialized for this HSM";
                break;
            case E_BSNK_INVALID_SCHEME_VERSION:
                default_msg = "Invalid scheme version";
                break;
            case E_BSNK_INVALID_SCHEME_KEYSET_VERSION:
                default_msg = "Invalid scheme keyset version.";
                break;
            case E_BSNK_MODULE_FUNCTION_LOCKED:
                default_msg = "The module functionality (KMA, Activate, Transform) needed for this operation is locked";
                break;
            case E_BSNK_FUNC_NOT_IMPLEMENTED:
                default_msg = "The requested function is not implemented or obsolete";
                break;
            case E_BSNK_INVALID_MBK:
                default_msg = "The MBK is invalid for this operation";
                break;
            case E_BSNK_KEYSTORE_NOT_PRESENT:
                default_msg = "The keystore could not be found. (no keys created or imported?)";
                break;
            case E_BSNK_IDENTITY_TO_ECPOINT:
                default_msg = "The provided identity could not be represented (embedded) as an elliptic curve point";
                break;
            case E_BSNK_ECDSA_VERIFY_FAILED:
                default_msg = "Verification of ECDSA signature on PI/PP/PIP/DEP failed.";
                break;
            case E_BSNK_AUTO_MIGRATION_FAILURE:
                default_msg = "Automatic migration of data and or keys during first run of update failed.";
                break;
        }
        String hex_code = Integer.toHexString(id);
        return default_msg + " (" + hex_code.toUpperCase() + ")";
    }

    @Deprecated
    public void setActiveSignature(String oid) {
        if (!oid.equalsIgnoreCase(SIG_OID_BSI_ECSCHNORR_SHA384) && !oid.equalsIgnoreCase(SIG_OID_BSI_ECSDSA_SHA384)) {
            throw new IllegalArgumentException("[UtimacoHSM::setActiveSignature] Invalid signature OID");
        }

        activeSignature = oid;
    }

    //==========================================================
    // HSM Connectivity and (Key) Management
    //==========================================================
    public void disconnectFromHSM() {
        if (cxi != null) {
            cxi.close();
        }
        cxi = null;
    }

    public void connectToHSM(String device_address, int timeout) throws IOException, CryptoServerException {
        if (cxi != null) {
            cxi.close();
        }

           // create instance of CryptoServerCXI (opens connection to CryptoServer)
        cxi = new CryptoServerCXI(device_address, timeout);
    }

    public void connectToHSM(String server_ip, int port, int timeout) throws IOException, CryptoServerException {
     connectToHSM(port + "@" + server_ip, timeout);
    }

    @Deprecated
    public void connectToHSMCluster(String configfile) throws CryptoServerException, IOException {

        CryptoServerConfig config = new CryptoServerConfig(configfile);

        if (cxi != null) {
            cxi.close();
        }

        // create instance of CryptoServerCXI (opens connection to CryptoServer)
        cxi = new CryptoServerCXI(config);
    }

    public boolean isConnected() {
        return cxi != null;
    }

    // Is not LAN but HSM serial
    public String getHSMSerial() throws IOException, CryptoServerException {
        byte[] mode = {6};
        byte[] answer = cxi.exec(0x83, 22, mode);
        int spec_len = answer[2];
        int key_len_1 = ((int) answer[3 + spec_len + 1] & 0xFF) << 8;
        key_len_1 |= (int) (answer[3 + spec_len + 2] & 0xFF);

        int offset = 6 + spec_len + key_len_1;
        byte[] serial = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        System.arraycopy(answer, offset, serial, 0, 16);
        return new String(serial);
    }

    public void logonKeyFile(String user_name, String key_path_name, String password) throws IOException, CryptoServerException {
        cxi.logonSign(user_name, key_path_name, password);
    }

    public void logonUserNamePassword(String user_name, String password) throws IOException, CryptoServerException {
        cxi.logonPassword(user_name, password);
    }

    public void logonSmartcard(String user_name, String port) throws IOException, CryptoServerException {
        cxi.logonSign(user_name, port, null);
    }

    public void initModule(String creatorOIN, int hw_id, boolean use_kmt, boolean use_activate, boolean use_transform, String supervisor_id) throws IOException, CryptoServerException {
        KMPModuleSetup ms;
        int flags = 0;

        if (use_kmt) {
            flags |= 0x01;
        }
        if (use_activate) {
            flags |= 0x02;
        }
        if (use_transform) {
            flags |= 0x04;
        }

        ms = new KMPModuleSetup(flags, creatorOIN, hw_id, supervisor_id);

        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_MODULE_INIT, ms.encode());
    }

    // Throws exception on failure
    public void updateModuleFlag(int flag, boolean set) throws IOException, CryptoServerException {
        KMPUpdateFlagRequest kmp_update_flags = new KMPUpdateFlagRequest(flag, set ? 1 : 0);
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_UPDATE_MODULE_FLAG, kmp_update_flags.encode());
    }

    public KMPModuleInfo getModuleInfo() throws IOException, CryptoServerException {
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_MODULE_INFO, null);
        return KMPModuleInfo.decode(asn1_out);
    }

    // DEPRECATED. Use getKeyListEx
    @Deprecated
    public KMPKeyList getKeyList(int slot, int max_keys) throws IOException, CryptoServerException {
        KMPKeyListRequest klr = new KMPKeyListRequest(slot, max_keys);
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_LIST_KEYS, klr.encode());
        return KMPKeyList.decode(asn1_out);
    }

    public KMPKeyListEx getKeyListEx(int slot, int max_keys) throws IOException, CryptoServerException {
        KMPKeyListRequest klr = new KMPKeyListRequest(slot, max_keys);
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_LIST_KEYS_EX, klr.encode());
        return KMPKeyListEx.decode(asn1_out);
    }

    public UtimacoAuthState getAthenticationState() throws IOException, CryptoServerException {
        int state = cxi.getAuthState();

        UtimacoAuthState auth_state = new UtimacoAuthState();
        auth_state.user_manager = (state & 0xF0000000) >> (7 * 4);
        auth_state.hsm_manager = (state & 0x0F000000) >> (6 * 4);
        auth_state.crypto_user = state & 0x0000000F;

        return auth_state;
    }

    public void getExtraUserInfo(String short_name, User usr) throws UnsupportedEncodingException, IOException, CryptoServerException {
        byte[] long_name = {};
        byte[] cmd = new byte[9];

        System.arraycopy(short_name.getBytes("UTF-8"), 0, cmd, 1, short_name.length());

        cmd[0] = 'N';

        long_name = cxi.exec(MDL_CMDS, CMDS_GET_USER_INFO, cmd);
        usr.setUsername(new String(long_name, "US-ASCII"));
    }

    public ArrayList<User> getUsers() throws IOException, CryptoServerException {
        byte[] answ;
        byte[] cmd = {};
        ArrayList<User> users = new ArrayList<>();

        answ = cxi.exec(MDL_CMDS, CMDS_LISTUSERS, cmd);

        int answlen = answ.length;
        int i = 0;
        int bsize = answ[0] + 1;
        while ((i * bsize) < answlen) {
            User user = new User();

            int userlen = 8;
            byte[] username = new byte[8];
            byte[] udata = new byte[bsize];

            int uperm;
            System.arraycopy(answ, (i * bsize), udata, 0, bsize);
            System.arraycopy(udata, 1, username, 0, 8);

            uperm = CryptoServerUtil.load_int4(udata, 9);
            user.setPermission(uperm);

            String usernameStr = new String(username);
            user.setUsername(usernameStr.trim());

            user.setAuthenticationMechCode((int) udata[13]);

            getExtraUserInfo(user.getUsername(), user);

            i++;
            users.add(user);
        }

        return users;
    }

    //==========================================================
    // KEY MANAGEMENT (OFFLINE)
    //==========================================================
    // RSERVED BY BSNk
    public void generateKMAMasterKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        byte[] asn1_in = attribs.encode();
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GENERATE_MASTER_KEYS, asn1_in);
    }

    // RSERVED BY BSNk
    public byte[] exportKMAMasterKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_MASTER_KEYS, asn1_in);
        return asn1_out;
    }

    // RSERVED BY BSNk
    public void importKMAMasterKeys(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_MASTER_KEYS, asn1_key_set);
    }

    public void generateActivateMasterKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        byte[] asn1_in = attribs.encode();
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GENERATE_ACTIVATION_MASTER_KEYS, asn1_in);
    }

    public byte[] exportActivateMasterKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_ACTIVATION_MASTER_KEYS, asn1_in);
        return asn1_out;
    }

    public void importActivateMasterKeys(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_ACTIVATION_MASTER_KEYS, asn1_key_set);
    }

    public byte[] exportActivateKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_ACTIVATE_KEYS, asn1_in);
        return asn1_out;
    }

    public void importActivateKeys(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_ACTIVATE_KEYS, asn1_key_set);
    }

    // U/u key
    public void generateSigningKey(int scheme_version, int scheme_ksv, int sign_key_version) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        attribs.setSigningKeyVersion(sign_key_version);
        byte[] asn1_in = attribs.encode();
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GENERATE_SIGNING_KEY_U, asn1_in);
    }

    public byte[] exportSigningKey(int scheme_version, int scheme_ksv, int sign_key_version) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        attribs.setSigningKeyVersion(sign_key_version);
        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_SIGNING_KEY_U, asn1_in);
        return asn1_out;
    }

    public void importSigningKey(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_SIGNING_KEY_U, asn1_key_set);
    }

    // Note that scheme version and scheme_key version are not used
    // This function just creates 2 master sets with version 1 and version 10
    public void generateTestKeys(int scheme_version, int scheme_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        byte[] asn1_in = attribs.encode();
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GENERATE_TEST_KEYS, asn1_in);
    }

    public byte[] exportTransformKeys(int scheme_version, int scheme_ksv, String idp_oin, int idp_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        KMPParticipant idp = new KMPParticipant(idp_oin, idp_ksv);
        attribs.setIdentityProvider(idp);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_TRANSFORM_KEYS, asn1_in);
        return asn1_out;
    }

    public byte[] exportVPIPVerificationKey(int scheme_version, int scheme_ksv, String idp_oin, int idp_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        KMPParticipant idp = new KMPParticipant(idp_oin, idp_ksv);
        attribs.setIdentityProvider(idp);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_VPIP_VERIFICATION_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] exportServiceProviderPublicKey(int scheme_version, int scheme_ksv, String sp_oin, int sp_ksv) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        KMPParticipant sp = new KMPParticipant(sp_oin, sp_ksv);
        attribs.setServiceProvider(sp);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_SP_PUBLIC_KEY, asn1_in);
        return asn1_out;
    }

    public void importTransformKeys(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_TRANSFORM_KEYS, asn1_key_set);
    }

    public void importServiceProviderPublicKey(byte[] asn1_key_set) throws IOException, CryptoServerException {
        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_SP_PUBLIC_KEY, asn1_key_set);
    }

    // U key
    public byte[] exportMessageVerificationKey(int scheme_version, int scheme_ksv, int sig_key_version) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        attribs.setSigningKeyVersion(sig_key_version);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_MSG_VERIFICATION_KEY, asn1_in);
        return asn1_out;
    }

    //==========================================================
    // HSM ACTIVATE
    //==========================================================
    @Deprecated
    public byte[] provideSignedPI_PP_DEP(char idType, String id, int scheme_ksv, String idp, int idp_KeyVersion, String status_service, int status_service_KeyVersion, String transmitter_oin, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(idp, idp_KeyVersion);
        kari.setStatusProvider(status_service, status_service_KeyVersion);
        kari.setActivator(transmitter_oin);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PI_PP_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPIP_DEP(char idType, String id, int scheme_ksv, String idp, int idp_KeyVersion, String status_service, int status_service_KeyVersion, String transmitter_oin, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(idp, idp_KeyVersion);
        kari.setStatusProvider(status_service, status_service_KeyVersion);
        kari.setActivator(transmitter_oin);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPIP_DEP_PP(char idType, String id, int scheme_ksv, String idp, int idp_KeyVersion, String status_service, int status_service_KeyVersion, String transmitter_oin, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(idp, idp_KeyVersion);
        kari.setStatusProvider(status_service, status_service_KeyVersion);
        kari.setActivator(transmitter_oin);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP_DEP_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedDEP(char idType, String id, int scheme_ksv, String recipient_oin, int recipient_key_version, String transmitter_oin, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setStatusProvider(recipient_oin, recipient_key_version);
        kari.setActivator(transmitter_oin);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedDei(char idType, String id, int schemeKeySetVersion, String statusProviderOIN,
            int statusProviderKeySetVersion,
            int signingKeyVersion, String authorizedPartyOIN) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPI(String id, int scheme_ksv, String recipient, int recipientKeyVersion, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) P_TYPE_BSN, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(recipient, recipientKeyVersion);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPP(char idType, String id, int scheme_ksv, String recipient, int recipientKeyVersion, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(recipient, recipientKeyVersion);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPIP(char idType, String id, int scheme_ksv, String recipient, int recipientKeyVersion, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(recipient, recipientKeyVersion);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPiDepPp(char idType, String id, int schemeKeySetVersion, String identityProviderOIN, int identityProviderKeyVersion,
            String statusProviderOIN, int statusProviderKeySetVersion,
            int signingKeyVersion, String authorizedPartyOIN, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException {

        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setIdentityProvider(identityProviderOIN, identityProviderKeyVersion);
        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        if (diversifier != null) {
            kari.setDiversifier(diversifier);
        }

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PI_PP_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPiDepPp(char idType, String id, int schemeKeySetVersion, String identityProviderOIN, int identityProviderKeyVersion, String statusProviderOIN, int statusProviderKeySetVersion, int signingKeyVersion, String authorizedPartyOIN) throws IOException, CryptoServerException {
        return provideSignedPiDepPp(idType, id, schemeKeySetVersion, identityProviderOIN, identityProviderKeyVersion, statusProviderOIN, statusProviderKeySetVersion, signingKeyVersion, authorizedPartyOIN, null);
    }

    @Deprecated
    public byte[] provideSignedPipDepPp(char idType, String id, int schemeKeySetVersion,
            String identityProviderOIN, int identityProviderKeyVersion,
            String statusProviderOIN, int statusProviderKeySetVersion,
            int signingKeyVersion, String authorizedPartyOIN, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setIdentityProvider(identityProviderOIN, identityProviderKeyVersion);
        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        if (diversifier != null) {
            kari.setDiversifier(diversifier);
        }

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP_DEP_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedPipDepPp(char idType, String id, int schemeKeySetVersion,
            String identityProviderOIN, int identityProviderKeyVersion,
            String statusProviderOIN, int statusProviderKeySetVersion,
            int signingKeyVersion, String authorizedPartyOIN) throws IOException, CryptoServerException {
        return provideSignedPipDepPp(idType, id, schemeKeySetVersion, identityProviderOIN, identityProviderKeyVersion, statusProviderOIN, statusProviderKeySetVersion, signingKeyVersion, authorizedPartyOIN, null);
    }

    @Deprecated
    public byte[] provideSignedDep(char idType, String id, int schemeKeySetVersion, String statusProviderOIN,
            int statusProviderKeySetVersion,
            int signingKeyVersion, String authorizedPartyOIN, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        if (diversifier != null) {
            kari.setDiversifier(diversifier);
        }

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedDep(char idType, String id, int schemeKeySetVersion, String statusProviderOIN, int statusProviderKeySetVersion, int signingKeyVersion, String authorizedPartyOIN) throws IOException, CryptoServerException {
        return provideSignedDep(idType, id, schemeKeySetVersion, statusProviderOIN, statusProviderKeySetVersion, signingKeyVersion, authorizedPartyOIN, null);
    }

    @Deprecated
    public byte[] provideSignedDep(char idType,
            String id,
            int schemeKeySetVersion,
            String statusProviderOIN,
            int statusProviderKeySetVersion,
            int signingKeyVersion,
            String authorizedPartyOIN,
            List<KMPKeyObjectValue> extraElements,
            ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        if (extraElements != null) {
            kari.setExtraElements(extraElements);
        }

        if (diversifier != null) {
            kari.setDiversifier(diversifier);
        }

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedDei(char idType,
            String id,
            int schemeKeySetVersion,
            String statusProviderOIN,
            int statusProviderKeySetVersion,
            int signingKeyVersion,
            String authorizedPartyOIN,
            List<KMPKeyObjectValue> extraElements) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, schemeKeySetVersion, signingKeyVersion);

        kari.setStatusProvider(statusProviderOIN, statusProviderKeySetVersion);
        kari.setAuthorizedParty(authorizedPartyOIN);

        if (extraElements != null) {
            kari.setExtraElements(extraElements);
        }

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] provideSignedVPIP(char idType, String id, int scheme_ksv, String idp, int idp_KeyVersion, int sig_key_version) throws IOException, CryptoServerException {
        KMPActivateRequest kari = new KMPActivateRequest(id, (int) idType, scheme_ksv, sig_key_version);

        kari.setIdentityProvider(idp, idp_KeyVersion);

        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_VPIP, asn1_in);
        return asn1_out;
    }

    public byte[] provideSignedPI(KMPActivateRequest kari) throws IOException, CryptoServerException {
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    public byte[] provideSignedPP(KMPActivateRequest kari) throws IOException, CryptoServerException {
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PP, asn1_in);
        return asn1_out;

    }

    public byte[] provideSignedPIP(KMPActivateRequest kari) throws IOException, CryptoServerException {
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_PIP, asn1_in);
        return asn1_out;
    }

    public byte[] provideSignedDep(KMPActivateRequest kari) throws IOException, CryptoServerException {
        // Error checking is also done by HSM but returns a more general code
        // So, do it before submitting to HSM to have more relevant info
        //  if(kari.getIdentityProviderOIN()==null || kari.getIdentityProviderOIN().isEmpty())
        //     throw new KMPException("[UtimacoHSM::provideSignedDep] No identity provider in request");

        //  if(kari.getStatusProviderOIN()==null || kari.getStatusProviderOIN().isEmpty())
        //      throw new KMPException("[UtimacoHSM::provideSignedDep] No status provider in request");
        // Activator element is optional
        //    if(kari.getActivatorOIN()==null || kari.getActivatorOIN().isEmpty())
        //        throw new KMPException("[UtimacoHSM::provideSignedDep] No activator in request");
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEP, asn1_in);
        return asn1_out;
    }

    public byte[] provideSignedDei(KMPActivateRequest kari) throws IOException, CryptoServerException {
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_DEI, asn1_in);
        return asn1_out;
    }

    public byte[] provideSignedVPIP(KMPActivateRequest kari) throws IOException, CryptoServerException {
        byte[] asn1_in = kari.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_ACTIVATE_GET_SIGNED_VPIP, asn1_in);
        return asn1_out;
    }

    //==========================================================
    // HSM TRANSFORM
    //==========================================================
    @Deprecated
    public byte[] transformUnsignedPI(byte[] unsignedPi, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        //       KMPTransformRequest ktri = new KMPTransformRequest(unsignedPi, scheme_ksv, idp, idp_kv, sp, sp_kv);
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPi, scheme_ksv, idp, idp_kv, sp, sp_kv);

        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPI(byte[] signedPi, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        //KMPTransformRequest ktri = new KMPTransformRequest(signedPi, scheme_ksv, idp, idp_kv, sp, sp_kv);
        KMPTransformRequest ktri = new KMPTransformRequest(signedPi, scheme_ksv, idp, idp_kv, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformUnsignedPP(byte[] unsignedPp, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformUnsignedPP(unsignedPp, scheme_ksv, idp, idp_kv, sp, sp_kv, null);
    }

    @Deprecated
    public byte[] transformUnsignedPP(byte[] unsignedPp, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPp, scheme_ksv, idp, idp_kv, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }
        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPP(byte[] signedPp, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPP(signedPp, scheme_ksv, idp, idp_kv, sp, sp_kv, null);
    }

    @Deprecated
    public byte[] transformSignedPP(byte[] signedPp, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPp, scheme_ksv, idp, idp_kv, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }
        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPIPAsPI(byte[] signedPip, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPip, scheme_ksv, idp, idp_kv, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPIPAsPP(byte[] signedPip, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPIPAsPP(signedPip, scheme_ksv, idp, idp_kv, sp, sp_kv, null);
    }

    @Deprecated
    public byte[] transformSignedPIPAsPP(byte[] signedPip, int scheme_ksv, String idp, int idp_kv, String sp, int sp_kv, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPip, scheme_ksv, idp, idp_kv, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformUnsignedPI(byte[] unsignedPi, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPi, sp, sp_kv);

        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformUnsignedPI(byte[] unsignedPi, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformUnsignedPI(unsignedPi, sp, sp_kv, null, 1);
    }

    @Deprecated
    public byte[] transformSignedPI(byte[] signedPi, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPi, sp, sp_kv);

        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPI(byte[] signedPi, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPI(signedPi, sp, sp_kv, null, 1);
    }

    @Deprecated
    public byte[] transformUnsignedPP(byte[] unsignedPp, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, ArrayList<KMPKeyValue> diversifier, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPp, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }
        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }
        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformUnsignedPP(byte[] unsignedPp, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformUnsignedPP(unsignedPp, sp, sp_kv, null, null, 1);
    }

    @Deprecated
    public byte[] transformSignedPP(byte[] signedPp, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, ArrayList<KMPKeyValue> diversifier, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPp, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }
        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPP(byte[] signedPp, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPP(signedPp, sp, sp_kv, null, null, 1);
    }

    // New for version R6
    @Deprecated
    public byte[] transformSignedPIPAsPI(byte[] signedPip, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPip, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }
        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PI, asn1_in);
        return asn1_out;
    }

    // New for version R6
    @Deprecated
    public byte[] transformSignedPIPAsPI(byte[] signedPip, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPIPAsPI(signedPip, sp, sp_kv, null, 1);
    }

    // New for version R6
    @Deprecated
    public byte[] transformSignedPIPAsPP(byte[] signedPip, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, ArrayList<KMPKeyValue> diversifier, int msgVersion) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPip, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }
        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }
        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }
        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PP, asn1_in);
        return asn1_out;
    }

    // New for version R6
    @Deprecated
    public byte[] transformSignedPIPAsPP(byte[] signedPip, String sp, int sp_kv) throws IOException, CryptoServerException, KMPException {
        return transformSignedPIPAsPP(signedPip, sp, sp_kv, null, null, 1);
    }

    @Deprecated
    public byte[] transformUnsignedPI(byte[] unsignedPi, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, int msgVersion, Integer targetSKSV) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPi, sp, sp_kv);

        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        if (targetSKSV != null) {
            ktri.setTargetSKSV(targetSKSV);
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPI(byte[] signedPi, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, int msgVersion, Integer targetSKSV) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPi, sp, sp_kv);

        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        if (targetSKSV != null) {
            ktri.setTargetSKSV(targetSKSV);
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformUnsignedPP(byte[] unsignedPp, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, ArrayList<KMPKeyValue> diversifier, int msgVersion, Integer targetSKSV) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(unsignedPp, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }
        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        if (targetSKSV != null) {
            ktri.setTargetSKSV(targetSKSV);
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PP, asn1_in);
        return asn1_out;
    }

    @Deprecated
    public byte[] transformSignedPP(byte[] signedPp, String sp, int sp_kv, List<KMPKeyObjectValue> extraElements, ArrayList<KMPKeyValue> diversifier, int msgVersion, Integer targetSKSV) throws IOException, CryptoServerException, KMPException {
        KMPTransformRequest ktri = new KMPTransformRequest(signedPp, sp, sp_kv);
        if (activeSignature != null) {
            ktri.setSignatureType(activeSignature);
        }

        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                ktri.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        if (extraElements != null) {
            for (int i = 0; i < extraElements.size(); i++) {
                if (extraElements.get(i).getValue() instanceof Integer) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (Integer) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof String) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (String) extraElements.get(i).getValue());
                } else if (extraElements.get(i).getValue() instanceof byte[]) {
                    ktri.addExtraElement(extraElements.get(i).getKey(), (byte[]) extraElements.get(i).getValue());
                }
            }
        }

        if (targetSKSV != null) {
            ktri.setTargetSKSV(targetSKSV);
        }

        ktri.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PP, asn1_in);
        return asn1_out;
    }

    // Order in = order out
    @Deprecated
    public List<KMPRecipientTransformInfo> transformUnsignedPolymorphEx(byte[] unsignedPI, byte[] unsignedPP, List<KMPRecipientTransformInfo> linkedServiceProviders, ArrayList<KMPKeyValue> diversifier, int msgVersion) throws KMPException, IOException, CryptoServerException {
        KMPMultiTransformRequest kmtr = new KMPMultiTransformRequest(unsignedPI, unsignedPP, linkedServiceProviders);

        if (diversifier != null) {
            for (int i = 0; i < diversifier.size(); i++) {
                kmtr.addDiversifierKeyValue(diversifier.get(i).getKey(), diversifier.get(i).getValue());
            }
        }

        kmtr.setTargetMessageVersion(msgVersion);

        byte[] asn1_in = kmtr.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_EX, asn1_in);

        // The result is a sequence of encrypted objects.
        // Parse the ASN1 sequence and split into list of Encrypted objects
        ASN1InputStream asn1_stream = new ASN1InputStream(asn1_out);
        DLSequence seq = (DLSequence) asn1_stream.readObject();
        if (seq.size() != linkedServiceProviders.size()) {
            throw new KMPException("[UtimacoHSM::transformUnsignedPolymorphEx] Output list size different from input list size");
        }

        List<KMPRecipientTransformInfo> output = new ArrayList<>();
        for (int i = 0; i < seq.size(); i++) {
            KMPRecipientTransformInfo info = new KMPRecipientTransformInfo(linkedServiceProviders.get(i));
            ASN1Encodable encodable = seq.getObjectAt(i);
            byte[] octets = encodable.toASN1Primitive().getEncoded();
            info.setEncryptedData(octets);
            output.add(info);
        }

        return output;
    }

    @Deprecated
    public List<KMPRecipientTransformInfo> transformUnsignedPolymorphEx(byte[] unsignedPI, byte[] unsignedPP, List<KMPRecipientTransformInfo> linkedServiceProviders, int msgVersion) throws KMPException, IOException, CryptoServerException {
        return transformUnsignedPolymorphEx(unsignedPI, unsignedPP, linkedServiceProviders, null, msgVersion);
    }

    public byte[] transformSignedPIPAsPI(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PI, asn1_in);
        return asn1_out;
    }

    public byte[] transformSignedPIPAsPP(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_AS_PP, asn1_in);
        return asn1_out;
    }

    public byte[] transformUnsignedPI(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PI, asn1_in);
        return asn1_out;
    }

    public byte[] transformUnsignedPP(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_PP, asn1_in);
        return asn1_out;
    }

    public byte[] transformSignedPI(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PI, asn1_in);
        return asn1_out;
    }

    public byte[] transformSignedPP(KMPTransformRequest ktri) throws IOException, CryptoServerException, KMPException {
        byte[] asn1_in = ktri.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PP, asn1_in);
        return asn1_out;
    }

    public List<KMPRecipientTransformInfo> transformUnsignedPolymorphEx(KMPMultiTransformRequest kmtr) throws KMPException, IOException, CryptoServerException {
        if (kmtr.getTargetMessageVersion() != 2) {
            throw new KMPException("[UtimacoHSM::transformUnsignedPolymorphEx] Only version 2 supported for target version");
        }

        byte[] asn1_in = kmtr.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_TRANSFORM_SIGNED_PIP_EX, asn1_in);

        // The result is a sequence of encrypted objects.
        // Parse the ASN1 sequence and split into list of Encrypted objects
        ASN1InputStream asn1_stream = new ASN1InputStream(asn1_out);
        DLSequence seq = (DLSequence) asn1_stream.readObject();
        if (seq.size() != kmtr.getRecipients().size()) {
            throw new KMPException("[UtimacoHSM::transformUnsignedPolymorphEx] Output list size different from input list size");
        }

        List<KMPRecipientTransformInfo> output = new ArrayList<>();
        for (int i = 0; i < seq.size(); i++) {
            KMPRecipientTransformInfo info = new KMPRecipientTransformInfo(kmtr.getRecipients().get(i));
            ASN1Encodable encodable = seq.getObjectAt(i);
            byte[] octets = encodable.toASN1Primitive().getEncoded();
            info.setEncryptedData(octets);
            output.add(info);
        }

        return output;
    }

    //==========================================================
    // KEY MANAGEMENT (ONLINE)
    //==========================================================

    /*--------------------------------------------------------------------------------------
    Example requesting Provider keys:
    byte[] asn1_i_keys = hsm.getServiceProviderIKeys(1, 1, "OIN", 1, sp_cert);
    byte[] asn1_p_keys = m_hsmDevice.getServiceProviderPKeys(1, 1, "OIN", 1, 1, sp_cert);
    KMPServiceProviderKeys sp_I_keys = KMPServiceProviderKeys.decode(asn1_i_keys);
    KMPServiceProviderKeys sp_P_keys = KMPServiceProviderKeys.decode(asn1_p_keys);
    
    // Rerieve the PKCS#7 enveloped data blobs
    p7_bytes = sp_I_keys.getP7KeyPairI();
    p7_bytes = sp_P_keys.getP7KeyPairP();
    p7_bytes = sp_P_keys.getP7ClosingKeyP();
    --------------------------------------------------------------------------------------*/
    public byte[] getServiceProviderIKeys(int scheme_version, int scheme_ksv, String sp_oin, int sp_key_set_version, X509Certificate sp_cert) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        KMPParticipant sp = new KMPParticipant(sp_oin, sp_key_set_version);
        sp.setCertificate(sp_cert);

        attribs.setServiceProvider(sp);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_I_KEYS, asn1_in);
        return asn1_out;
    }

    public byte[] getServiceProviderPKeys(int scheme_version, int scheme_ksv, String sp_oin, int sp_key_set_version, int closing_key_version, X509Certificate sp_cert) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        KMPParticipant sp = new KMPParticipant(sp_oin, sp_key_set_version);
        sp.setClosingKeyVersion(closing_key_version);
        sp.setCertificate(sp_cert);

        attribs.setServiceProvider(sp);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_P_KEYS, asn1_in);
        return asn1_out;
    }

    // DRKi
    public byte[] getServiceProviderDEPReceiveKey(int scheme_version, int scheme_ksv, String sp_oin, int sp_key_set_version, X509Certificate sp_cert, ArrayList<KMPKeyValue> diversifier, String means_provider_oin) throws IOException, CryptoServerException, NoSuchAlgorithmException, InvalidKeySpecException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        KMPParticipant sp = new KMPParticipant(sp_oin, sp_key_set_version);
        sp.setCertificate(sp_cert);
        attribs.setServiceProvider(sp);

        KMPParticipant mp = new KMPParticipant(means_provider_oin);
        attribs.setMeansProvider(mp);

        if (diversifier != null) {
            attribs.setDiversifier(diversifier);
        }

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_SP_DEP_RECEIVE_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] getServiceProviderDEPReceiveKey(int scheme_version, int scheme_ksv, String sp_oin, int sp_key_set_version, X509Certificate sp_cert, String means_provider_oin) throws IOException, CryptoServerException, NoSuchAlgorithmException, InvalidKeySpecException {
        return getServiceProviderDEPReceiveKey(scheme_version, scheme_ksv, sp_oin, sp_key_set_version, sp_cert, null, means_provider_oin);
    }

    public byte[] getPublicKey(int scheme_version, int scheme_ksv, String keyLabel) throws IOException, CryptoServerException {
        //KMPKeySetRequest ksri = new KMPKeySetRequest(scheme_version, scheme_ksv, "", 0, 0, "");
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);
        attribs.setKeyLabel(keyLabel);

        byte[] asn1_in = attribs.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_PUBLIC_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] generateKey(int key_type, int scheme_version, int scheme_ksv, int key_version, String key_label, String subject) throws IOException, CryptoServerException {
        KMPKeyDef kd = new KMPKeyDef(scheme_version, scheme_ksv, key_version, key_label, subject);

        kd.setKeyType(key_type);

        byte[] asn1_in = kd.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GENERATE_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] deleteKey(int scheme_version, int scheme_ksv, int key_version, String key_label, String user_id) throws IOException, CryptoServerException {
        KMPKeyInfoEx ki = new KMPKeyInfoEx(scheme_version, scheme_ksv, key_version, key_label, user_id);

        byte[] asn1_in = ki.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_DELETE_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] copyKey(int scheme_version, int scheme_ksv, int key_version, String key_label, String subject, int scheme_ksv_dst) throws IOException, CryptoServerException {
        KMPKeyDef kd = new KMPKeyDef(scheme_version, scheme_ksv_dst, key_version, key_label, subject);
        kd.setSourceKeySetVersion(scheme_ksv);

        byte[] asn1_in = kd.encode();
        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_COPY_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] getPseudonymConversionKey(int scheme_version, int scheme_ksv, String sp_1_oin, int sp_1_ck, X509Certificate sp_1_cert, String sp_2_oin, int sp_2_ck, X509Certificate sp_2_cert, ArrayList<KMPKeyValue> diversifier) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes attribs = new KMPKeyRequestAttributes(scheme_version, scheme_ksv);

        // Provide dummy key version. Not used but expected in ASN1
        KMPParticipant sp_1 = new KMPParticipant(sp_1_oin, 0);
        sp_1.setClosingKeyVersion(sp_1_ck);
        sp_1.setCertificate(sp_1_cert);

        KMPParticipant sp_2 = new KMPParticipant(sp_2_oin, 0);
        sp_2.setClosingKeyVersion(sp_2_ck);
        sp_2.setCertificate(sp_2_cert);

        attribs.setServiceProvider(sp_1);
        attribs.setServiceProvider2(sp_2);

        if (diversifier != null) {
            attribs.setDiversifier(diversifier);
        }

        byte[] asn1_in = attribs.encode();

        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_GET_PSEUDONYM_CONVERSION_KEY, asn1_in);
        return asn1_out;
    }

    public byte[] getPseudonymConversionKey(int scheme_version, int scheme_ksv, String sp_1_oin, int sp_1_ck, X509Certificate sp_1_cert, String sp_2_oin, int sp_2_ck, X509Certificate sp_2_cert) throws IOException, CryptoServerException {
        return getPseudonymConversionKey(scheme_version, scheme_ksv, sp_1_oin, sp_1_ck, sp_1_cert, sp_2_oin, sp_2_ck, sp_2_cert, null);
    }

    public byte[] decryptAuditElement(int scheme_version, int scheme_ksv, String src_oid, String supervisor, String recipient_oin, int recipient_kv, byte[] audit_element) throws IOException, CryptoServerException {
        KMPAuditElementDecryptRequest aedr = new KMPAuditElementDecryptRequest(scheme_version, scheme_ksv, src_oid, supervisor, recipient_oin, recipient_kv, audit_element);

        byte[] asn1_in = aedr.encode();

        byte[] asn1_out = cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_DECRYPT_AUDIT_ELEMENT, asn1_in);

        return asn1_out;
    }

    public byte[] exportPolymorphMappingKey(int sksv_1, int sksv_2) throws IOException, CryptoServerException {
        KMPKeyRequestAttributes kra = new KMPKeyRequestAttributes(1, sksv_1);
        kra.setTargetSchemeKeySetVersion(sksv_2);

        byte[] asn1_in = kra.encode();

        return cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_EXPORT_POLYMORPH_MAPPING_KEY, asn1_in);
    }

    public void importPolymorphMappingKey(byte[] asn1_key_set) throws IOException, CryptoServerException {

        cxi.exec(BSNK_MODULE_ID, BSNK_FUNC_IMPORT_POLYMORPH_MAPPING_KEY, asn1_key_set);
    }
}
