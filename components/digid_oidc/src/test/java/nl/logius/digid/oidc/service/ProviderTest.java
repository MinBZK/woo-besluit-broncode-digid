
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


package nl.logius.digid.oidc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import nl.logius.digid.oidc.Application;
import nl.logius.digid.oidc.client.DienstenCatalogusClient;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.exception.InvalidSignatureException;
import nl.logius.digid.oidc.helpers.OidcTestClient;
import nl.logius.digid.oidc.model.DcMetadataResponse;
import nl.logius.digid.oidc.model.ProtocolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.KeyStoreException;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class})
class ProviderTest {

    private static final String VALID_REQUEST = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";

    @Mock
    private DienstenCatalogusClient dcClient;

    @Autowired
    private OidcTestClient client;

    @Value("${oidc.keystore_file}")
    private String keystoreFile;
    @Value("${oidc.keystore_password}")
    private String keystoreFilePassword;
    @Value("${proxy:}")
    private String configuredProxyUrl;
    @Value("${protocol}://${hosts.ad}/openid-connect/v1")
    private String issuer;
    @Value("${protocol}://${hosts.api}/openid-connect/v1")
    private String backchannel;

    @InjectMocks
    @Spy
    private Provider provider = new Provider(keystoreFile, keystoreFilePassword, configuredProxyUrl, issuer, backchannel);

    @BeforeEach
    void beforeEach() throws DienstencatalogusException, KeyStoreException, IOException, ParseException {
        when(dcClient.retrieveMetadataFromDc(client.CLIENT_ID)).thenReturn(metadataResponse());
        doReturn(client.getJWKSet()).when(provider).getPublicKeys(anyString());

        ReflectionTestUtils.setField(provider, "keystoreFile", keystoreFile);
        ReflectionTestUtils.setField(provider, "keystoreFilePassword", keystoreFilePassword);
    }

    @Test
    void metadataTest() throws JsonProcessingException {
        var response = provider.metadata();

        assertEquals(List.of("code"), response.get("response_types_supported"));

    }

    @Test
    void generateJWETest() {
        //given
        //when
        provider.generateJWE("data", "jwskUri");
        //then
    }

    @Test
    void verifyValidSignatureTest() throws ParseException, InvalidSignatureException, IOException, JOSEException {
        provider.verifySignature("jwskUri", SignedJWT.parse(client.generateRequest()));
    }

    @Test
    void verifyInvalidSignatureTest() {
        assertThrows(
                InvalidSignatureException.class,
                () -> provider.verifySignature("jwskUri", SignedJWT.parse(VALID_REQUEST))
        );
    }

    private DcMetadataResponse metadataResponse() {
        var metadata = new DcMetadataResponse();
        metadata.setMetadataUrl("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        metadata.setAppActive(true);
        metadata.setAppReturnUrl("return_url");
        metadata.setMinimumReliabilityLevel("20");
        metadata.setProtocolType(ProtocolType.OIDC_APP);
        metadata.setClientId(client.CLIENT_ID);
        metadata.setServiceName("Mijn DigiD - app");

        return metadata;
    }
}
