
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

package nl.logius.bsnkpp.crypto;

import nl.logius.bsnkpp.util.Util;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import org.bouncycastle.math.ec.ECPoint;
import nl.logius.bsnkpp.usve.USvEIdentity;
import nl.logius.bsnkpp.usve.USvEPIP;
import nl.logius.bsnkpp.usve.USvEConstants;
import nl.logius.bsnkpp.usve.USvEProofOfConformity;
import nl.logius.bsnkpp.usve.USvEPseudonym;

public class ServiceProviderCrypto {

    static BigInteger zero = BigInteger.valueOf(0L);
    static BigInteger one = BigInteger.valueOf(1L);
    static BigInteger three = BigInteger.valueOf(3L);
    static BigInteger two = BigInteger.valueOf(2L);
    static BigInteger four = BigInteger.valueOf(4L);
    static BigInteger BigInt256 = BigInteger.valueOf(256L);

    BigInteger privateKeyIDD = null;
    ECPoint publicKeyIDD = null;
    BigInteger privateKeyPDD = null;
    ECPoint publicKeyPDD = null;
    BigInteger privateKeyPCD = null;

    public ServiceProviderCrypto() {
        privateKeyIDD = null;
        publicKeyIDD = null;
        privateKeyPDD = null;
        publicKeyPDD = null;
        privateKeyPCD = null;
    }

    public void registerIdentityKeys(ECPoint publicKey, BigInteger privateKey) {
        publicKeyIDD = publicKey;
        privateKeyIDD = privateKey;
    }

    public void registerPseudonymKeys(ECPoint publicKey, BigInteger privateKey, BigInteger privateClosingKey) {
        publicKeyPDD = publicKey;
        privateKeyPDD = privateKey;
        privateKeyPCD = privateClosingKey;
    }

    // Return ASN1 coded USvEIdentity
    public byte[] getDecryptedIdentity(int schemeVersion, int schemeKeysetVersion, String recipient, int recipientKeyVersion, ECPoint[] userEI) throws Exception {
        /* Decrypt raw identity */
        String[] result = decryptEncryptedIdentity(userEI);

        /* convert to ASN1 USvE required format */
        USvEIdentity identifier = new USvEIdentity(USvEConstants.OID_BSNK_I, schemeVersion, schemeKeysetVersion, recipient, recipientKeyVersion, result[0].charAt(0), result[1]);

        return identifier.encode();
    }

    public byte[] getDecryptedPseudonym(int schemeVersion, int schemeKeysetVersion, String recipient, int recipientKeyVersion, ECPoint[] userEP, char pseudoType) throws Exception {
        ECPoint pseudonymPoint = decryptEncryptedPseudonym(userEP);
        //String pseudonymString = Util.toHexString(pseudonymPoint.getEncoded(false));
        String pseudonymString = Base64.getEncoder().encodeToString(pseudonymPoint.getEncoded(false));
        USvEPseudonym pseudonym = new USvEPseudonym(USvEConstants.OID_BSNK_P, schemeVersion, schemeKeysetVersion, recipient, recipientKeyVersion, null, pseudoType, pseudonymString);
        return pseudonym.encode();
    }

    // Return identity type and identity number as strings
    // Result[0] = String(Type)
    // Result[1] = String(Identifier)
    public String[] decryptEncryptedIdentity(ECPoint[] user_ei) throws Exception {

        if (privateKeyIDD == null || publicKeyIDD == null) {
            throw new Exception("[ServiceProviderCrypto::decryptEncryptedIdentity] No keys loaded for identity decryption");
        }

      if (!user_ei[2].equals(publicKeyIDD)) {
            throw new Exception("[ServiceProviderCrypto::decryptEncryptedIdentity] Encrypted Identity does not belong to the Service Provider keys");
        }     
      
      /* Decrypt raw identity */
        ECPoint IPoint1 = (user_ei[1].subtract(user_ei[0].multiply(privateKeyIDD))).normalize();

        /*interpret raw identity and do OAEP checks */
        return getIdentityInfo(IPoint1, 18, 10);
    }

    public ECPoint decryptEncryptedPseudonym(ECPoint[] user_ep) throws Exception {
        if (privateKeyPDD == null || publicKeyPDD == null || privateKeyPCD == null) {
            throw new Exception("[ServiceProviderCrypto::decryptEncryptedPseudonym] No keys loaded for pseudonym decryption");
        }

        if (!user_ep[2].equals(publicKeyPDD)) {
            throw new Exception("[ServiceProviderCrypto::decryptEncryptedPseudonym] Encrypted Pseudonym does not belong to the Service Provider keys");
        }
        return ((user_ep[1].subtract(user_ep[0].multiply(privateKeyPDD))).multiply(privateKeyPCD)).normalize();
    }

    static String[] getIdentityInfo(ECPoint IPoint, int mesLen, int hashLen) throws Exception {
        /* Parse Identity (typically, BSN) from decrypted Identity in scheme version 1.
	 * Identity is assumed to be present as OAEP encoding message of size mesLen 
	 * whereby hash of length hashLen is used.
	 * That is, OAEP encoded message is of size 2*hashLen + 2 + mesLen. 
	 * Method check is format specification is followed.
	 * On Error return string array of size zero.
	 * */

        if (IPoint.isInfinity() || mesLen < 0 || hashLen < 0) {
            throw new Exception("[ServiceProviderCrypto::getIdentityInfo] Invalid encrypted identity");
        }

        BigInteger x_coord = IPoint.normalize().getXCoord().toBigInteger();

        byte[] OAEParray = Util.Big_to_byte_array(x_coord, BrainpoolP320r1.Q.bitLength() / 8);

//        byte[] message = new byte[mesLen];

        byte[] message = OAEP.decode(OAEParray, mesLen, hashLen);

        if (message[0] != (byte) 0x01 || (int) message[2] < 0 || (int) message[2] > mesLen - 3) {
            throw new Exception("[ServiceProviderCrypto::getIdentityInfo] Invalid message");
        }

        for (int i = 3 + (int) message[2]; i < mesLen; i++) {
            if (message[i] != (byte) 0x00) {
                throw new Exception("[ServiceProviderCrypto::getIdentityInfo] Invalid message patching"); // everything other than the actual message payload should be zero 
            }
        }

        String[] result = new String[2];
        result[0] = new String(message, 1, 1);                 // type
        result[1] = new String(message, 3, (int) message[2]);   // identifier

        return result;
    }

    public ECPoint[] convertDEP2EP(ECPoint[] dep, BigInteger DRKi) throws Exception {
        if (!dep[2].equals(publicKeyPDD)) {
            throw new Exception("[ServiceProviderCrypto::convertDEP2EP] Direct Encrypted Pseudonym does not belong to the Service Provider keys");
        }

        ECPoint[] Result = new ECPoint[3];
        Result[0] = dep[0].multiply(DRKi);
        Result[1] = dep[1].multiply(DRKi);
        Result[2] = dep[2];

        return Result;
    }

    static public Boolean verifyVPIP(USvEPIP pip, USvEProofOfConformity poc, String Identifier, ECPoint Vi, ECPoint Y) throws Exception {
        MessageDigest md;
        byte[] hash;
        BigInteger r;
        ECPoint temp1, temp2;

        // Check if Identifier in VPIP equals that given as parameter
        if (pip.getPoints().length != 5) {
            throw new Exception("[ServiceProviderCrypto::verifyVPIP] PIP is missing public points");
        }

        if (!pip.getPoints()[3].equals(Y)) {
            throw new Exception("[ServiceProviderCrypto::verifyVPIP] PIP does not belong to the Service Provider keys");
        }

        String[] values = getIdentityInfo(poc.getP1(), 18, 10);

        if (!Identifier.equals(values[1])) {
            return false;
        }

        //ZK1 begin
        //Check signature input
        if (!(poc.getZK1(0).compareTo(zero) > 0) || (poc.getZK1(0).bitLength() > 320)) {
            return false;
        }
        if (!(poc.getZK1(1).compareTo(zero) > 0) || !(poc.getZK1(1).compareTo(BrainpoolP320r1.Q) < 0)) {
            return false;
        }

        ECPoint T_1, V_1;
        temp1 = poc.getT().multiply(poc.getZK1(1));
        temp2 = pip.getPoints()[1].multiply(poc.getZK1(0));
        T_1 = temp1.subtract(temp2);

        temp1 = Vi.multiply(poc.getZK1(1));
        temp2 = Y.multiply(poc.getZK1(0));
        V_1 = temp1.subtract(temp2);

        if (T_1.isInfinity() || V_1.isInfinity()) {
            return false;
        }

        md = MessageDigest.getInstance("SHA384");
        md.update(T_1.normalize().getXCoord().getEncoded());
        md.update(T_1.normalize().getYCoord().getEncoded());
        md.update(V_1.normalize().getXCoord().getEncoded());
        md.update(V_1.normalize().getYCoord().getEncoded());
        hash = md.digest();
        r = new BigInteger(1, Arrays.copyOfRange(hash, 0, 40));

        if (!r.equals(poc.getZK1(0))) {
            return false;
        }

        //ZK1 end
        //ZK2 begin
        //Check signature input 
        if (!(poc.getZK2(0).compareTo(zero) > 0) || (poc.getZK2(0).bitLength() > 320)) {
            return false;
        }
        if (!(poc.getZK2(1).compareTo(zero) > 0) || !(poc.getZK2(1).compareTo(BrainpoolP320r1.Q) < 0)) {
            return false;
        }

        ECPoint G_2, V_2;
        temp1 = BrainpoolP320r1.G.multiply(poc.getZK2(1));
        temp2 = pip.getPoints()[0].multiply(poc.getZK2(0));
        G_2 = temp1.subtract(temp2);

        temp1 = Vi.multiply(poc.getZK2(1));
        temp2 = poc.getT().subtract(poc.getP1());
        temp2 = temp2.multiply(poc.getZK2(0));
        V_2 = temp1.subtract(temp2);

        if (G_2.isInfinity() || V_2.isInfinity()) {
            return false;
        }

        md = MessageDigest.getInstance("SHA384");
        md.update(G_2.normalize().getXCoord().getEncoded());
        md.update(G_2.normalize().getYCoord().getEncoded());
        md.update(V_2.normalize().getXCoord().getEncoded());
        md.update(V_2.normalize().getYCoord().getEncoded());
        hash = md.digest();
        r = new BigInteger(1, Arrays.copyOfRange(hash, 0, 40));
        //ZP2 end

        return r.equals(poc.getZK2(0));
    }

    public static boolean verifyECSchnorrBSI(byte[] mes, BigInteger[] Signature, ECPoint used_gen, ECPoint VerKey) throws Exception {
        if ((Signature[0].bitLength() > 320) || (Signature[1].bitLength() > 320)) {
            return false;
        }

        ECPoint Q;
        BigInteger ZeroBG = BigInteger.valueOf(0L);
        if (Signature[0].bitCount() > 320) {
            return false;
        }
        if (!(Signature[1].compareTo(ZeroBG) > 0) || !(Signature[1].compareTo(BrainpoolP320r1.Q) < 0)) {
            return false;
        }
        Q = used_gen.multiply(Signature[1]).add(VerKey.multiply(Signature[0]));
        if (Q.isInfinity()) {
            return false;
        }
        MessageDigest md = MessageDigest.getInstance("SHA384");

        md.update(mes);
        md.update(Q.normalize().getXCoord().getEncoded());

        byte[] hash = md.digest();
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(hash, 0, 40));/* use only 320 MSB bits of hash */

        return r.equals(Signature[0]);
    }

    public static boolean verifyBSIECSDSA(byte[] mes, BigInteger[] Signature, ECPoint used_gen, ECPoint VerKey) throws Exception {
        BigInteger ZeroBG = BigInteger.valueOf(0L);
        if (!(Signature[0].compareTo(ZeroBG) > 0) || (Signature[0].bitLength() > 320)) {
            return false;
        }
        if (!(Signature[1].compareTo(ZeroBG) > 0) || !(Signature[1].compareTo(BrainpoolP320r1.Q) < 0)) {
            return false;
        }
        ECPoint Q;

        // Q = used_gen_G * s - auth_pub_key_Pa * r
        Q = used_gen.multiply(Signature[1]).subtract(VerKey.multiply(Signature[0]));
        if (Q.isInfinity()) {
            return false;
        }
        MessageDigest md = MessageDigest.getInstance("SHA384");
        /* md.update(mes); place in TR-3111 v2.0 */

        byte[] x = Q.normalize().getXCoord().getEncoded();
        byte[] y = Q.normalize().getYCoord().getEncoded();

        md.update(x);
        md.update(y);
        /* new in TR-3111 v2.1 */
        md.update(mes);
        /* new place in TR-3111 v2.1 */
        byte[] hash = md.digest();
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(hash, 0, 40));/* use only 320 MSB bits of hash */

        return (r.compareTo(Signature[0]) == 0);
    }

    public static boolean VerifyECSchnorr(byte[] msg, String oid, BigInteger[] signature, ECPoint used_gen, ECPoint ver_key) throws Exception {
        if(msg==null || oid==null || signature==null || used_gen==null || ver_key==null)
            throw new Exception("[ServiceProviderCrypto::VerifyECSchnorr] NULL parameter provided to function");
        switch (oid) {
            case "SSSSSSSSSSSSSSSS":  // ISO ECDSA (not used in firmware anymore)
            case "0.4.0.127.0.7.1.1.4.4.3":  // BSI as ISO Used in R3 release BSNk
               // System.out.println("[ServiceProviderCrypto:: VerifyECSchnorr] verifyBSIECSDSA");
                return verifyBSIECSDSA(msg, signature, used_gen, ver_key);
            case "0.4.0.127.0.7.1.1.4.3.3":  // BSI legacy (used in R2release BSNk
               // System.out.println("[ServiceProviderCrypto:: VerifyECSchnorr] verifyECSchnorrBSI");
                return verifyECSchnorrBSI(msg, signature, used_gen, ver_key);
        }

        return false;
    }
}
