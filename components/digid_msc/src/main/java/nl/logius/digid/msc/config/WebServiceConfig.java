
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

package nl.logius.digid.msc.config;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import https.digid_nl.schema.mu_status_controller.RegisterSCStatusPort;
import https.digid_nl.schema.mu_status_controller.RegisterSCStatusPortService;
import nl.logius.digid.msc.ws.RegisterStatusSCEndpoint;
import nl.logius.digid.msc.ws.ValidationFaultInterceptor;
import nl.logius.digid.msc.ws.WebServiceLoggingInInterceptor;
import nl.logius.digid.msc.ws.WebServiceLoggingOutInterceptor;
/*
 * the suppress warning is here cause the logging changes a lot of Apache CXF in the newer versions
 * http://cxf.apache.org/docs/message-logging.html
 */
@SuppressWarnings("deprecation")
@Configuration
public class WebServiceConfig {
    public static final String BASE_URL = "/ws";
    public static final String SERVICE_URL = "";

    @Value("${ws.wsdl-file-name}")
    private String wsdlFileName;

    @Bean
    public ServletRegistrationBean cxfServlet() {
        return new ServletRegistrationBean(new CXFServlet(), BASE_URL + "/*");
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        SpringBus springBus = new SpringBus();
        springBus.getInInterceptors().add(logInInterceptor());
        springBus.getInFaultInterceptors().add(logInInterceptor());
        springBus.getOutInterceptors().add(logOutInterceptor());
        springBus.getOutFaultInterceptors().add(logOutInterceptor());
        return springBus;
    }

    @Bean
    public RegisterSCStatusPort registerSCStatusPort() {
        return new RegisterStatusSCEndpoint();
    }

    @Bean
    public Endpoint endpoint() {
        EndpointImpl endpoint = new EndpointImpl(springBus(), registerSCStatusPort());

        endpoint.setServiceName(registerSCStatusPortService().getServiceName());
        endpoint.setWsdlLocation(registerSCStatusPortService().getWSDLDocumentLocation().toString());
        endpoint.publish(SERVICE_URL);
        endpoint.getOutFaultInterceptors().add(validationFaultInterceptor());
        return endpoint;
    }

    @Bean
    public RegisterSCStatusPortService registerSCStatusPortService() {
        return new RegisterSCStatusPortService();
    }

    @Bean
    public AbstractLoggingInterceptor logInInterceptor() {
        LoggingInInterceptor webserviceLoggingInInterceptor = new WebServiceLoggingInInterceptor();
        // The In-Messages are pretty without setting it - when setting it Apache CXF throws empty lines into the In-Messages
        return webserviceLoggingInInterceptor;
    }

    @Bean
    public AbstractLoggingInterceptor logOutInterceptor() {
        LoggingOutInterceptor webserviceLoggingOutInterceptor = new WebServiceLoggingOutInterceptor();
        webserviceLoggingOutInterceptor.setPrettyLogging(true);
        return webserviceLoggingOutInterceptor;
    }

    @Bean
    public AbstractSoapInterceptor validationFaultInterceptor() {
        return new ValidationFaultInterceptor();
    }
}
