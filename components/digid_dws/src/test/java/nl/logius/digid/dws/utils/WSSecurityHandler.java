
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

import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import java.io.StringWriter;
import java.util.Properties;

public class WSSecurityHandler {

    private Properties properties;

    public WSSecurityHandler(Properties properties) {
        this.properties = properties;
    }

    public String handleMessage(SOAPMessage message, int keyIdentifierType) throws SOAPException, WSSecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException{
        Document doc = message.getSOAPBody().getOwnerDocument();
        Crypto crypto = CryptoFactory.getInstance(properties);

        WSSecHeader secHeader = new WSSecHeader(doc);
        Element element = secHeader.insertSecurityHeader();
        secHeader.setSecurityHeaderElement(element);

        WSSecSignature wsSignature = new WSSecSignature(secHeader);
        wsSignature.setUserInfo(properties.getProperty("org.apache.ws.security.crypto.merlin.keystore.alias"),
                properties.getProperty("privatekeypassword"));
        wsSignature.setKeyIdentifierType(keyIdentifierType);
        wsSignature.setUseSingleCertificate(true);
        wsSignature.setDigestAlgo(DigestMethod.SHA256);
        wsSignature.setSignatureAlgorithm(WSConstants.RSA_SHA256);

        wsSignature.getParts().add(WSSecurityUtil.getDefaultEncryptionPart(doc));
        wsSignature.getParts().add(new WSEncryptionPart("KeyInfo", WSConstants.SIG_NS, "Content"));

        Document signedDoc = wsSignature.build(crypto);

        final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        final DOMImplementationLS domImplementationLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
        final LSSerializer lsSerializer = domImplementationLS.createLSSerializer();

        LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        StringWriter stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(signedDoc, lsOutput);
        return stringWriter.toString();
    }
}
