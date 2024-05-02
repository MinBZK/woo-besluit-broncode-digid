
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

import https.digid_nl.schema.mu_pin_reset.RegisterPRPortService;
import https.digid_nl.schema.mu_pin_reset.RegisterPinResetPort;
import nl.logius.digid.dws.ws.RegisterPinResetEndpoint;
import nl.logius.digid.dws.ws.RegisterPinResetValidationFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

@Configuration
public class PinResetWebServiceConfig {
    public static final String PIN_RESET_SERVICE_URL = "pin_reset";

    @Bean
    public RegisterPinResetPort registerPinResetPort() {
        return new RegisterPinResetEndpoint();
    }

    @Bean
    public Endpoint pinResetEndpoint(@Autowired final SpringBus springBus) {
        EndpointImpl endpoint = new EndpointImpl(springBus, registerPinResetPort());

        endpoint.setServiceName(registerPRPortService().getServiceName());
        endpoint.setWsdlLocation(registerPRPortService().getWSDLDocumentLocation().toString());
        endpoint.publish(PIN_RESET_SERVICE_URL);
        endpoint.getOutFaultInterceptors().add(registerPinResetvalidationFaultInterceptor());
        return endpoint;
    }

    @Bean
    public RegisterPRPortService registerPRPortService() {
        return new RegisterPRPortService();
    }

    @Bean
    public AbstractSoapInterceptor registerPinResetvalidationFaultInterceptor() {
        return new RegisterPinResetValidationFaultInterceptor();
    }
}
