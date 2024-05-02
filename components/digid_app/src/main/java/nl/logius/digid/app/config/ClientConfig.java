
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

package nl.logius.digid.app.config;

import nl.logius.digid.app.client.*;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {
    @Value("${urls.internal.x}")
    private String digidXBaseUrl;

    @Value("${urls.internal.ss}")
    private String digidSsBaseUrl;

    @Value("${urls.internal.rda}")
    private String digidRdaBaseUrl;

    @Value("${urls.internal.ns}")
    private String digidNsBaseUrl;

    @Value("${urls.internal.eid}")
    private String digidEIdBaseUrl;

    @Value("${urls.internal.msc}")
    private String digidMscBaseUrl;

    @Value("${urls.internal.hsm-bsnk}")
    private String digidHsmBsnkBaseUrl;

    @Value("${urls.internal.dws}")
    private String digidDwsBaseUrl;

    @Value("${urls.internal.oidc}")
    private String digidOidcBaseUrl;

    @Value("${urls.internal.saml}")
    private String digidSamlBaseUrl;

    @Value("${iapi.timeout}")
    private int timeout;

    @Value("${iapi.token}")
    private String iapiToken;

    @Value("${source_ip_salt}")
    private String sourceIpSalt;

    @Bean
    public DigidClient digidClient() {
        return new DigidClient(HttpUrl.get(digidXBaseUrl), iapiToken, timeout);
    }

    @Bean
    public SharedServiceClient sharedServiceClient() {
        return new SharedServiceClient(HttpUrl.get(digidSsBaseUrl), iapiToken, timeout);
    }

    @Bean
    public RdaClient rdaClient() {
        return new RdaClient(HttpUrl.get(digidRdaBaseUrl), iapiToken, timeout, sourceIpSalt);
    }

    @Bean
    public EidClient eidClient() {
        return new EidClient(HttpUrl.get(digidEIdBaseUrl), iapiToken, timeout, sourceIpSalt);
    }

    @Bean
    public NsClient nsClient() {
        return new NsClient(HttpUrl.get(digidNsBaseUrl), iapiToken, timeout);
    }

    @Bean
    public MscClient mscClient() {
        return new MscClient(HttpUrl.get(digidMscBaseUrl), iapiToken, timeout);
    }

    @Bean
    public HsmBsnkClient hsmBsnkClient() {
        return new HsmBsnkClient(HttpUrl.get(digidHsmBsnkBaseUrl), iapiToken, timeout);
    }

    @Bean
    public DwsClient dwsClient(){
        return new DwsClient(HttpUrl.get(digidDwsBaseUrl), iapiToken, timeout);
    }

    @Bean
    public OidcClient oidcClient(){
        return new OidcClient(HttpUrl.get(digidOidcBaseUrl), iapiToken, timeout);
    }

    @Bean
    public SamlClient samlClient(){
        return new SamlClient(HttpUrl.get(digidSamlBaseUrl), iapiToken, timeout);
    }
}
