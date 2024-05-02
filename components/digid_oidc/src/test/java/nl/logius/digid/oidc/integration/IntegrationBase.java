
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

package nl.logius.digid.oidc.integration;


import nl.logius.digid.oidc.client.AdClient;
import nl.logius.digid.oidc.client.AppClient;
import nl.logius.digid.oidc.client.DienstenCatalogusClient;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.helpers.OidcTestClient;
import nl.logius.digid.oidc.model.DcMetadataResponse;
import nl.logius.digid.oidc.model.ProtocolType;
import nl.logius.digid.oidc.repository.OpenIdRepository;
import nl.logius.digid.oidc.service.OpenIdService;
import nl.logius.digid.oidc.service.Provider;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class IntegrationBase {

    protected WebTestClient webTestClient;

    @Autowired
    protected OpenIdRepository repository;

    @Autowired
    protected OidcTestClient client;


    @MockBean
    protected AdClient adClient;

    @MockBean
    protected AppClient appClient;

    @MockBean
    protected DienstenCatalogusClient dcClient;

    @Value("${oidc.keystore_file}") protected String keystoreFile;
    @Value("${oidc.keystore_password}") protected String keystoreFilePassword;
    @Value("${proxy:}") protected String configuredProxyUrl;
    @Value("${protocol}://${hosts.ad}/openid-connect/v1") protected String issuer;
    @Value("${protocol}://${hosts.api}/openid-connect/v1") protected String backchannel;
    @Value("${hosts.app}") protected String appHost;
    @Value("${hosts.digid}") protected String digidHost;
    @Value("${protocol}") protected String protocol;

    protected OpenIdService service;

    protected void setup() throws DienstencatalogusException, IOException, ParseException {
        service = new OpenIdService(repository, appClient, adClient, dcClient, provider);

        ReflectionTestUtils.setField(service, "issuer", issuer);
        ReflectionTestUtils.setField(service, "appHost", appHost);
        ReflectionTestUtils.setField(service, "protocol", protocol);
        ReflectionTestUtils.setField(service, "digidHost", digidHost);

        ReflectionTestUtils.setField(provider, "keystoreFile", keystoreFile);
        ReflectionTestUtils.setField(provider, "keystoreFilePassword", keystoreFilePassword);

        when(dcClient.retrieveMetadataFromDc(any())).thenReturn(metadataResponse());
        doReturn(client.getJWKSet()).when(provider).getPublicKeys(anyString());
        when(appClient.startAppSession(any(), any(), any(), any(), any(), any())).thenReturn(Map.of("id", "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
    }

    @Spy
    protected Provider provider = new Provider(keystoreFile, keystoreFilePassword, configuredProxyUrl, issuer, backchannel);

    protected DcMetadataResponse metadataResponse() {
        var metadata = new DcMetadataResponse();
        metadata.setMetadataUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        metadata.setAppActive(true);
        metadata.setAppReturnUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        metadata.setMinimumReliabilityLevel("20");
        metadata.setProtocolType(ProtocolType.OIDC_APP);
        metadata.setClientId(client.CLIENT_ID);
        metadata.setServiceName("Mijn DigiD - app");
        metadata.setLegacyWebserviceId(1L);
        metadata.setRequestStatus("STATUS_OK");

        return metadata;
    }
}
