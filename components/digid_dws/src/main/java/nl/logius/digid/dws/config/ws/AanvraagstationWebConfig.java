
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

package nl.logius.digid.dws.config.ws;

import https.digid_nl.schema.aanvraagstation.AanvraagstationPort;
import https.digid_nl.schema.aanvraagstation.AanvraagstationPortService;
import nl.logius.digid.dws.util.Wss4jStreamInInterceptor;
import nl.logius.digid.dws.ws.AppActivationEndpoint;
import nl.logius.digid.dws.ws.AppActivationValidationFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AanvraagstationWebConfig {
    public static final String AANVRAAGSTATION_APP_ACTIVATION_SERVICE_URL = "aanvraagstation_app_activation";

    @Value("${ws.server.rvig-aanvraagstation.ws-signature.password}")
    private String signaturePassword;

    @Value("${ws.server.rvig-aanvraagstation.ws-signature.keystore}")
    private String signatureKeystore;

    @Bean
    public AanvraagstationPort aanvraagstationPort() { return new AppActivationEndpoint();
    }

    @Bean
    public Endpoint aanvraagstationAppActivationEndpoint(@Autowired final SpringBus springBus) {
        EndpointImpl endpoint = new EndpointImpl(springBus, aanvraagstationPort());

        endpoint.setServiceName(appActivationService().getServiceName());
        endpoint.setWsdlLocation(appActivationService().getWSDLDocumentLocation().toString());
        endpoint.publish(AANVRAAGSTATION_APP_ACTIVATION_SERVICE_URL);
        endpoint.getOutFaultInterceptors().add(appActivationValidationFaultInterceptor());
        endpoint.getInInterceptors().add(signatureValidationInterceptor());
        return endpoint;
    }

    @Bean
    public AanvraagstationPortService appActivationService() {
        return new AanvraagstationPortService();
    }


    @Bean
    public AbstractWSS4JInterceptor signatureValidationInterceptor() {

        Map<String, Object> inProps = new HashMap<>();

        inProps.put(WSHandlerConstants.ACTION, "Signature");
        //loadCryptoFromPropertiesFile is overridden so property file name is ignored
        inProps.put(WSHandlerConstants.SIG_PROP_FILE, "");
        inProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");

        return new Wss4jStreamInInterceptor(inProps, signatureKeystore, signaturePassword);
    }

    @Bean
    public AbstractSoapInterceptor appActivationValidationFaultInterceptor() {
        return new AppActivationValidationFaultInterceptor();
    }
}
