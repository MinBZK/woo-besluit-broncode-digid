
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

import java.util.List;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eid.gdi.nl._1_0.webservices.BSNKDEPPort;
import eid.gdi.nl._1_0.webservices.BSNKDEPService;
import eid.gdi.nl._1_0.webservices.BSNKProvideDEPFault;
import eid.gdi.nl._1_0.webservices.DirectEncryptedPseudonymType;
import eid.gdi.nl._1_0.webservices.ProvideDEPsRequest;
import nl.logius.digid.dws.exception.BsnkException;

@Component
public class
ProvideDepBsnkClient extends AbstractBsnkClient {

    @Autowired
    public ProvideDepBsnkClient(@Value("${ws.client.connection_timeout}") Integer connectionTimeout,
            @Value("${ws.client.receive_timeout}") Integer receiveTimeout,
            @Value("${ws.client.bsnk_activate.tls_keystore_password}") String clientTlsPassphrase,
            @Value("${ws.client.bsnk_activate.tls_keystore}") String clientTlsKeystore,
            @Value("${ws.client.bsnk_activate.validate_signature_truststore_password}") String validateSignatureKeystorePassword,
            @Value("${ws.client.bsnk_activate.validate_signature_truststore}") String validateSignatureKeystore,
            @Value("${ws.client.bsnk_activate.signing_keystore_password}") String signingKeystorePassword,
            @Value("${ws.client.bsnk_activate.signing_keystore}") String signingKeystore,
            @Value("${ws.client.bsnk_provide_dep.endpoint}") String endpoint,
            AbstractLoggingInterceptor logInInterceptor,
            AbstractLoggingInterceptor logOutInterceptor) {

        super(connectionTimeout, receiveTimeout, clientTlsPassphrase, clientTlsKeystore,
                validateSignatureKeystorePassword, validateSignatureKeystore, signingKeystorePassword, signingKeystore,
                endpoint, logInInterceptor, logOutInterceptor);
    }

    public List<DirectEncryptedPseudonymType> provideDep(ProvideDEPsRequest request) throws BsnkException {
        try {
            return ((BSNKDEPPort) this.bindingProvider).bsnkProvideDEPs(request).getDirectEncryptedPseudonyms();
        } catch (SOAPFaultException ex) {
            if (ex.getCause().getMessage().equals("The signature or decryption was invalid")) {
                throw new BsnkException("SignatureValidationFault", ex.getCause().getMessage(), ex.getCause());
            }
            throw new BsnkException("BSNKProvideDEPFault", ex.getMessage(), ex);
        } catch (WebServiceException ex) {
            throw new BsnkException("Could not send bsnkProvidePPPPCAOptimized", ex.getCause().getMessage(),
                    ex.getCause());
        } catch (BSNKProvideDEPFault ex) {
            throw new BsnkException("BSNKProvideDEPFault", ex.getCause().getMessage(), ex.getCause());
        }
    }

    @Override
    protected BindingProvider getBsnkPort() {
        BSNKDEPService service = new BSNKDEPService(new WSAddressingFeature());
        return (BindingProvider) service.getBSNKDEP();
    }
}
