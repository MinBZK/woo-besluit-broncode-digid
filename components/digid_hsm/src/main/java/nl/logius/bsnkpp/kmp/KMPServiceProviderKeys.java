
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

/*-------------------------------------------------
KMPServiceProviderKeys ::= SEQUENCE {
    schemeVersion       INTEGER,
    schemeKeySetVersion INTEGER,
    creator             A5String,
    recipient           KMPParticipant,
    recipientKeyI   [0] IMPLICIT OCTET STRING OPTIONAL,
    recipientKeyP   [1] IMPLICIT OCTET STRING OPTIONAL,
    recipientKeyC   [2] IMPLICIT OCTET STRING OPTIONAL,
    recipientKeyDRK [3] IMPLICIT OCTET STRING OPTIONAL
}
-------------------------------------------------*/
package nl.logius.bsnkpp.kmp;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DLSequence;

public class KMPServiceProviderKeys {

    static private final int TAG_ID_D = 0;
    static private final int TAG_PD_D = 1;
    static private final int TAG_PC_D = 2;
    static private final int TAG_DRK = 3;
    static private final int TAG_SECRET_1 = 4;
    static private final int TAG_SECRET_2 = 5;
    static private final int TAG_MIGRATION_ID = 6;
    
    private int schemeVersion;
    private int schemeKeyVersion;
    private String creator;
    private KMPParticipant recipient;

    private byte[] p7ECKeypairI;
    private byte[] p7ECKeypairP;
    private byte[] p7ECKeypairC;
    private byte[] p7ECKeypairDRK;
    private byte[] p7SecretKey1;
    private byte[] p7SecretKey2;
    private String migrationId;

    static public byte[] extractASN1Keypair(String pem) {
        int start = pem.indexOf("\r\n\r\n");
        int end = pem.indexOf("-----END");

        String base64 = pem.substring(start, end).replaceAll("\r\n", "");

        return Base64.getDecoder().decode(base64.getBytes());
    }

    static public BigInteger getPrivateKeyFromPEM(String pemECKeypair) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        byte[] asn1 = extractASN1Keypair(pemECKeypair);
        org.bouncycastle.asn1.sec.ECPrivateKey key = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(asn1);
        return key.getKey();
    }

    static public BigInteger getSecretKeyFromPEM(String pemECKeypair) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        byte[] asn1 = extractASN1Keypair(pemECKeypair);
        org.bouncycastle.asn1.sec.ECPrivateKey key = org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(asn1);
        return key.getKey();
    }

    
    /*
    static public BigInteger extractPrivateKey(byte[] der_encoded_p7, PrivateKey priv_key) throws Exception {
        Pkcs7 p7 = Pkcs7.decode(der_encoded_p7, priv_key);
        return KMPServiceProviderKeys.getPrivateKeyFromPEM(new String(p7.getContent()));
    }

    static public ECPoint extractPublicKey(byte[] der_encoded_p7, PrivateKey priv_key) throws Exception {
        Pkcs7 p7 = Pkcs7.decode(der_encoded_p7, priv_key);
        return KMPServiceProviderKeys.getPublicKeyFromPEM(new String(p7.getContent()));
    }

    static public String extractPEM(byte[] der_encoded_p7, PrivateKey priv_key) throws Exception {
        Pkcs7 p7 = Pkcs7.decode(der_encoded_p7, priv_key);
        return new String(p7.getContent());
    }
     */
    // ID_D
    public byte[] getP7KeyPairI() {
        return p7ECKeypairI;
    }

    // PD_D
    public byte[] getP7KeyPairP() {
        return p7ECKeypairP;
    }

    // PC_D
    public byte[] getP7KeyPairC() {
        return p7ECKeypairC;
    }

    public byte[] getP7KeyPairDRK() {
        return p7ECKeypairDRK;
    }

    public byte[] getP7SecretKey1() {
        return p7SecretKey1;
    }

    public byte[] getP7SecretKey2() {
        return p7SecretKey2;
    }

    public String getMigrationId() {
        return this.migrationId;
    }
    
    public static KMPServiceProviderKeys decode(byte[] encoded) throws IOException {
        KMPServiceProviderKeys result = new KMPServiceProviderKeys();

        ASN1InputStream parser = new ASN1InputStream(encoded);

        DLSequence seq = (DLSequence) parser.readObject();

        result.schemeVersion = ((ASN1Integer) seq.getObjectAt(0)).getValue().intValue(); // schemeVersion
        result.schemeKeyVersion = ((ASN1Integer) seq.getObjectAt(1)).getValue().intValue(); // schemeKeyVersion
        result.creator = ((DERIA5String) seq.getObjectAt(2)).getString(); // creator

        byte[] party_encoded = seq.getObjectAt(3).toASN1Primitive().getEncoded();
        result.recipient = KMPParticipant.decode(party_encoded);

        DLSequence key_seq = (DLSequence) seq.getObjectAt(4);

        for (int i = 0; i < key_seq.size(); i++) {
            ASN1TaggedObject to = (ASN1TaggedObject) key_seq.getObjectAt(i);

            switch (to.getTagNo()) {
                case TAG_ID_D:
                    result.p7ECKeypairI = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_PD_D:
                    result.p7ECKeypairP = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_PC_D:
                    result.p7ECKeypairC = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_DRK:
                    result.p7ECKeypairDRK = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_SECRET_1:
                    result.p7SecretKey1 = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_SECRET_2:
                    result.p7SecretKey2 = ((ASN1OctetString) to.getObject()).getOctets();
                    break;
                case TAG_MIGRATION_ID:
                    result.migrationId = new String(((ASN1OctetString) to.getObject()).getOctets()); 
                    break;
            }
        }

        return result;
    }
}
