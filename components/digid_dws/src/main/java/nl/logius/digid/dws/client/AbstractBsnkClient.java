
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

package nl.logius.digid.dws.client;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.WSS4JConstants;

import nl.logius.digid.dws.util.PasswordCallbackHandler;
import nl.logius.digid.dws.util.Wss4jStreamInInterceptor;
import nl.logius.digid.dws.util.Wss4jStreamOutInterceptor;

public abstract class AbstractBsnkClient extends AbstractWsClient {

    protected final String bsnkActivateEndpoint;
    protected final String validateSignatureKeystorePassword;
    protected final String validateSignatureKeystore;
    protected final String signingKeystorePassword;
    protected final String signingKeystore;
    private final AbstractLoggingInterceptor logInInterceptor;
    private final AbstractLoggingInterceptor logOutInterceptor;

    protected BindingProvider bindingProvider;

    protected AbstractBsnkClient(Integer connectionTimeout, Integer receiveTimeout, String clientTlsPassphrase,
            String clientTlsKeystore, String validateSignatureKeystorePassword, String validateSignatureKeystore,
            String signingKeystorePassword, String signingKeystore, String endpoint,  AbstractLoggingInterceptor logInInterceptor,
            AbstractLoggingInterceptor logOutInterceptor) {

        super(connectionTimeout, receiveTimeout, clientTlsPassphrase, clientTlsKeystore);

        this.bsnkActivateEndpoint = endpoint;
        this.validateSignatureKeystorePassword = validateSignatureKeystorePassword;
        this.validateSignatureKeystore = validateSignatureKeystore;
        this.signingKeystorePassword = signingKeystorePassword;
        this.signingKeystore = signingKeystore;

        this.logInInterceptor = logInInterceptor;
        this.logOutInterceptor = logOutInterceptor;

        this.bindingProvider = getBsnkPort();

        setupBsnkClient(bindingProvider);
    }

    protected void setupBsnkClient(BindingProvider port) {
        port.getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, getAddressingProperties());
        port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, bsnkActivateEndpoint);

        final Client client = ClientProxy.getClient(port);
        setTimeouts(client);
        setupTLS(client);

        client.getInInterceptors().add(this.logInInterceptor);
        client.getOutInterceptors().add(this.logOutInterceptor);

        client.getOutInterceptors().add(bsnkSigningOutInterceptor());
        client.getInInterceptors().add(bsnkValidateSignatureInInterceptor());


        final Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://www.w3.org/2005/08/addressing");
        client.getRequestContext().put("soap.env.ns.map", nsMap);
    }

    private AddressingProperties getAddressingProperties(){
        AddressingProperties addressingProperties = new AddressingProperties();

        AttributedURIType actionProperty = new AttributedURIType();
        actionProperty.setValue("urn:nl-gdi-eid:1.0:webservices:ProvidePP_PPCAOptimizedRequest");
        addressingProperties.setAction(actionProperty);

        AttributedURIType toProperty = new AttributedURIType();
        toProperty.setValue(bsnkActivateEndpoint);
        addressingProperties.setTo(toProperty);

        AttributedURIType referenceAddress = new AttributedURIType();
        EndpointReferenceType reference = new EndpointReferenceType();
        referenceAddress.setValue("http://www.w3.org/2005/08/addressing/anonymous");
        reference.setAddress(referenceAddress);
        addressingProperties.setReplyTo(reference);
        addressingProperties.setFaultTo(reference);

        return addressingProperties;
    }

    private AbstractWSS4JInterceptor bsnkSigningOutInterceptor() {
        Map<String, Object> outProps = new HashMap<>();

        outProps.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        outProps.put(ConfigurationConstants.USER, "1");
        outProps.put(ConfigurationConstants.SIG_DIGEST_ALGO, WSS4JConstants.SHA256);
        outProps.put(ConfigurationConstants.SIG_ALGO, WSS4JConstants.RSA_SHA256);
        outProps.put(ConfigurationConstants.SIG_C14N_ALGO, WSS4JConstants.C14N_EXCL_OMIT_COMMENTS);
        CallbackHandler callbackHandler = new PasswordCallbackHandler(signingKeystorePassword);
        outProps.put(ConfigurationConstants.PW_CALLBACK_REF, callbackHandler);
        outProps.put(ConfigurationConstants.SIG_KEY_ID, "IssuerSerial");
        outProps.put(ConfigurationConstants.SIGNATURE_PARTS,
                WSS4JConstants.ELEM_BODY + ";{}{" + WSS4JConstants.SIG_NS + "}" + WSS4JConstants.KEYINFO_LN);
        // loadCryptoFromPropertiesFile is overridden so property file name is ignored`
        outProps.put(ConfigurationConstants.SIG_PROP_FILE, "");

        return new Wss4jStreamOutInterceptor(outProps, signingKeystore, signingKeystorePassword);
    }

    private AbstractWSS4JInterceptor bsnkValidateSignatureInInterceptor() {
        Map<String, Object> inProps = new HashMap<>();

        inProps.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        // loadCryptoFromPropertiesFile is overridden so property file name is ignored
        inProps.put(ConfigurationConstants.SIG_PROP_FILE, "");

        return new Wss4jStreamInInterceptor(inProps, validateSignatureKeystore, validateSignatureKeystorePassword);
    }

    protected abstract BindingProvider getBsnkPort();
}
