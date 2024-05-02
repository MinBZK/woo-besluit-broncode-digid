
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

package nl.logius.digid.saml.domain.encryption;

import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.opensaml.saml.saml2.core.EncryptedID;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.encryption.support.*;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoGenerator;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.KeyName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Service
public class EncryptionService {
    @Value("${saml.brp_crt}")
    private String brpCrt;

    @Value("${saml.brp_entity_id}")
    private String brpEntityId;

    /**
     * Encrypts a String value
     * @param value BSN, VI or VP in string format
     * @param attributeType type of value
     * @param keyEncryptionParameters list of values used for encryption e.g. recipient
     * @return
     * @throws EncryptionException
     */
    public EncryptedID encryptValue(String value, String attributeType, List<KeyEncryptionParameters> keyEncryptionParameters) throws EncryptionException {
        final DataEncryptionParameters dataEncryptionParameters = new DataEncryptionParameters();
        dataEncryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256);

        if (keyEncryptionParameters.size() > 1) dataEncryptionParameters.setKeyInfoGenerator(generateKeyInfoName(null));

        final NameID nameID = getNameID(value, attributeType);
        final Encrypter encrypter = new Encrypter(dataEncryptionParameters, keyEncryptionParameters);
        encrypter.setKeyPlacement(Encrypter.KeyPlacement.PEER);

        return encrypter.encrypt(nameID);
    }

    /**
     * Decrypts an EncryptedID
     * @param encrypted Encrypted BSN, VI or VP
     * @param credential
     * @param entityId
     * @return
     * @throws DecryptionException
     */
    public NameID decryptValue(EncryptedID encrypted, Credential credential, String entityId) throws DecryptionException {
        StaticKeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(credential);
        Decrypter decrypter = new Decrypter(null, resolver, new EncryptedElementTypeEncryptedKeyResolver(entityId));

        return (NameID) decrypter.decrypt(encrypted);
    }

    public KeyEncryptionParameters getEncryptionParams(String recipient, KeyInfo keyInfo) throws CertificateException {
        String cert = keyInfo.getX509Datas().get(0).getX509Certificates().get(0).getValue().replaceAll("\\s+", "");
        final Credential cred = serviceCertificateCredential(cert);

        KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
        setKeyEncryptionParameters(keyEncryptionParameters, recipient, keyInfo.getKeyNames().get(0).getValue(), cred);
        return keyEncryptionParameters;
    }

    public KeyEncryptionParameters getEncryptionParamsBRP() throws CertificateException, NoSuchAlgorithmException {
        String cert = brpCrt.replaceAll("\\s+", "");
        final Credential cred = serviceCertificateCredential(cert);
        X509Certificate x509Cert = getServiceCertificate(cert);
        String keyValue = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-1").digest(x509Cert.getEncoded()));

        KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
        setKeyEncryptionParameters(keyEncryptionParameters, brpEntityId, keyValue, cred);
        return keyEncryptionParameters;
    }

    public void setKeyEncryptionParameters(KeyEncryptionParameters keyEncryptionParameters, String recipient, String keyValue, Credential cred) {
        keyEncryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        keyEncryptionParameters.setRecipient(recipient);
        keyEncryptionParameters.setEncryptionCredential(cred);
        keyEncryptionParameters.setKeyInfoGenerator(generateKeyInfoName(keyValue));
    }

    private KeyInfoGenerator generateKeyInfoName(String keyValue){
        KeyInfo keyInfo = OpenSAMLUtils.buildSAMLObject(KeyInfo.class);
        KeyName keyName = OpenSAMLUtils.buildSAMLObject(KeyName.class);
        keyName.setValue(keyValue);
        keyInfo.getKeyNames().add(keyName);
        return new StaticKeyInfoGenerator(keyInfo);
    }

    private NameID getNameID(String value, String type) {
        final NameID nameId = OpenSAMLUtils.buildSAMLObject(NameID.class);
        nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        nameId.setNameQualifier(type);
        nameId.setValue(value);
        return nameId;
    }

    private Credential serviceCertificateCredential(String string) throws CertificateException {
        final X509Certificate cert = getServiceCertificate(string);
        return CredentialSupport.getSimpleCredential(cert, null);
    }

    private X509Certificate getServiceCertificate(String string) throws CertificateException {
        // TODO do this in a more elegant way than string manipulation
        string = "-----BEGIN CERTIFICATE-----\n" + string + "-----END CERTIFICATE-----";
        final InputStream targetStream = new ByteArrayInputStream(string.getBytes());
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(targetStream);
    }
}
