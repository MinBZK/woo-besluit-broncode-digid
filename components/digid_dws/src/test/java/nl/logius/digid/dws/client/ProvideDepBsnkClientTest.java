
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.io.Resources;
import eid.gdi.nl._1_0.webservices.ProvideDEPsRequest;
import eid.gdi.nl._1_0.webservices.RelyingPartyType;
import nl.logius.digid.dws.Application;
import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.dws.utils.SignatureValidator;
import nl.logius.digid.dws.utils.SigningHelper;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ProvideDepBsnkClientTest {
    protected ProvideDepBsnkClient client;

    protected ProvideDEPsRequest request;

    protected String mockResponseBody;

    protected SigningHelper signingHelper;

    protected WireMockServer wireMockServer = new WireMockServer(BSNK_ACTIVATE_WIREMOCK_PORT);

    @Value(value = "classpath:responses/ProvideDEPsResponse_valid_unsigned.xml")
    protected Resource provideDepResponseFile;

    @Value(value = "classpath:certs/tls_keystore/keystore_base64.txt")
    protected Resource tlsKeystoreFile;

    protected String tlsKeystorePassword = "SSSSSSSSSSSSSSSSSSS";

    @Value(value = "classpath:certs/signing_keystore/keystore_base64.txt")
    protected Resource signingKeystoreFile;

    protected String signingKeystorePassword = "SSSSSSSSSSSSSSSS";

    @Value(value = "classpath:certs/signing_keystore/truststore.jks.base64.txt")
    protected Resource signingTruststoreFile;

    protected String signingTruststorePassword = "SSSSSSSSSSSSSSSSSSSSS";

    protected String signingTruststore;

    protected String signatureValidationKeystorePath = "src/test/resources/certs/signature_validation_keystore/keystore.pkcs12";

    protected String signatureValidationKeystorePassword = "SSSSSSSSSSSSSSSS";

    @Value(value = "classpath:certs/signature_validation_keystore/truststore.jks.base64.txt")
    protected Resource signatureValidationTruststoreFile;

    protected String signatureValidationTruststorePassword = "SSSSSSSSSSSSSSSSSSSSS";

    public static final int BSNK_ACTIVATE_WIREMOCK_PORT = 7666;

    @Mock
    private AbstractLoggingInterceptor logInInterceptor;
    @Mock
    private AbstractLoggingInterceptor logOutInterceptor;

    @BeforeEach
    protected void init() throws IOException, DatatypeConfigurationException {
        // Client keystores
        String tlsKeystore = Resources.toString(tlsKeystoreFile.getURL(), StandardCharsets.UTF_8);
        String signingKeystore = Resources.toString(signingKeystoreFile.getURL(), StandardCharsets.UTF_8);
        String signatureValidationTruststore = Resources.toString(signatureValidationTruststoreFile.getURL(),
                StandardCharsets.UTF_8);

        // To Validate outgoing signatures
        this.signingTruststore = Resources.toString(this.signingTruststoreFile.getURL(), StandardCharsets.UTF_8);
        this.signingHelper = new SigningHelper(this.signatureValidationKeystorePath,
                this.signatureValidationKeystorePassword);

        // The Soap Mock response
        String ppResponseTemplate = Resources.toString(provideDepResponseFile.getURL(), StandardCharsets.UTF_8);
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        client = new ProvideDepBsnkClient(90000, 90000, tlsKeystorePassword, tlsKeystore, signatureValidationTruststorePassword,
                signatureValidationTruststore, signingKeystorePassword, signingKeystore,
                "http://localhost:" + BSNK_ACTIVATE_WIREMOCK_PORT + "/bsnk_stub/provideDep", logInInterceptor, logOutInterceptor);

        setupRequest();
    }

    protected void setupRequest() throws DatatypeConfigurationException {
        request = new ProvideDEPsRequest();

        request.setDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

        request.setRequestID(UUID.randomUUID().toString());
        request.setRequester("digidMuOin");

        RelyingPartyType relyingParty = new RelyingPartyType();
        relyingParty.setEntityID("oin");
        relyingParty.setKeySetVersion(BigInteger.valueOf(1l));
        request.getRelyingParties().add(relyingParty);

        request.setBSN("bsn");
    }

    protected void setupWireMock() {
        wireMockServer.start();
        wireMockServer.stubFor(post("/bsnk_stub/provideDep")
                .willReturn(ok().withHeader("Content-Type", "text/xml").withBody(this.mockResponseBody)));
    }

    @AfterEach
    protected void stopWireMock() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    public void testValidResponseSuccess() {
        setupWireMock();

        try {
            assertEquals(1, client.provideDep(request).size());
        } catch (BsnkException ex) {
            fail(ex.getMessage());
        }
    }


    @Test
    public void testRequestHasValidSignature() throws BsnkException {
        setupWireMock();

        client.provideDep(request);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/bsnk_stub/provideDep")).withHeader("Content-Type",
                containing("xml")));
        ServeEvent serveEvent = wireMockServer.getAllServeEvents().get(0);
        String requestBody = new String(serveEvent.getRequest().getBody());
        SignatureValidator signatureValidator = new SignatureValidator(this.signingTruststore,
                this.signingTruststorePassword);

        try {
            signatureValidator.validate(requestBody);
        } catch (WSSecurityException ex) {
            fail("Signature was invalid");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testResponseWithX509KeyIdentifier() throws IOException {
        String ppResponseTemplate = Resources.toString(provideDepResponseFile.getURL(), StandardCharsets.UTF_8);
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.X509_KEY_IDENTIFIER);

        setupWireMock();

        try {
            assertEquals(1, client.provideDep(request).size());
        } catch (BsnkException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testResponseWithoutSignature() throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("responses/ProvideDEPsResponse_valid_unsigned.xml");
        this.mockResponseBody = IOUtils.toString(is, StandardCharsets.UTF_8);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.provideDep(request));
        assertEquals("BSNKProvideDEPFault", ex.getFaultReason());
    }
}

