
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

package nl.logius.digid.oidc.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import net.minidev.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public class JWTable {
    private static final Logger logger = LoggerFactory.getLogger(JWTable.class);

    protected final String keystoreFile;
    protected final String keystoreFilePassword;

    public JWTable(String keystoreFile, String keystoreFilePassword) {
        this.keystoreFile = keystoreFile;
        this.keystoreFilePassword = keystoreFilePassword;
    }

    protected String generateJWT(String data) {
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(getKeyname()).build();
        JWSObject jwsObject = new JWSObject(header, new Payload(data));
        logger.debug("jwt data: {}", data);

        try {
            jwsObject.sign(new RSASSASigner(getPrivateKey()));
        } catch (JOSEException e) {
            return null;
        }

        return jwsObject.serialize();
    }

    public JSONObject generateJWK() {
        try {
            JSONObject response = new JSONObject(JWK.load(getKeyStore(), "1", keystoreFilePassword.toCharArray()).toPublicJWK().toJSONObject());
            response.appendField("use", KeyUse.SIGNATURE.identifier());
            response.put("kid", getKeyname());
            return response;
        } catch (KeyStoreException | JOSEException e) {
            e.printStackTrace();
        }

        return new JSONObject();
    }

    protected PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) getKeyStore().getKey("1", keystoreFilePassword.toCharArray());
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

      protected PublicKey getPublicKey() {
        try {
            RSAPrivateCrtKey privk = (RSAPrivateCrtKey)getPrivateKey();
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
            KeyFactory fac = KeyFactory.getInstance("RSA");

            return fac.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected KeyStore getKeyStore() {
        try {
            InputStream is = new ByteArrayInputStream(Base64.decode(keystoreFile));
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, keystoreFilePassword.toCharArray());

            return keyStore;
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String getKeyname() {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            return Hex.encodeHexString(digest.digest(getPublicKey().getEncoded()));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
