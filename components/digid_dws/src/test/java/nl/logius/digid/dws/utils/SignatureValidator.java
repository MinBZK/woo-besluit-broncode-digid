
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

package nl.logius.digid.dws.utils;

import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.soap.SOAPException;
import org.apache.commons.codec.binary.Base64;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class SignatureValidator {
    String validateSignatureKeystore;
    String validateSignatureKeystorePassword;

    public SignatureValidator(String validateSignatureKeystore, String validateSignatureKeystorePassword) {
        this.validateSignatureKeystore = validateSignatureKeystore;
        this.validateSignatureKeystorePassword = validateSignatureKeystorePassword;
    }

    // Throws an WSSecurityException if the signature is not valid
    public void validate(String soapRequest) throws IOException, SOAPException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, WSSecurityException, TransformerException, ParserConfigurationException, SAXException {
        final InputStream is = new ByteArrayInputStream(Base64.decodeBase64(this.validateSignatureKeystore));
        final KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(is, this.validateSignatureKeystorePassword.toCharArray());

        Properties properties = new Properties();
        properties.setProperty("org.apache.ws.security.crypto.provider",
                "org.apache.ws.security.components.crypto.Merlin");
        Crypto crypto = CryptoFactory.getInstance(properties);

        ((Merlin) crypto).setTrustStore(keystore);
        WSSecurityEngine engine = new WSSecurityEngine();
        WSSConfig config = WSSConfig.getNewInstance();
        engine.setWssConfig(config);

        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new ByteArrayInputStream(soapRequest.getBytes()));

        engine.processSecurityHeader(document, null, null, crypto);
    }
}
