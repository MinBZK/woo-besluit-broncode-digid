
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

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import eid.gdi.nl._1_0.webservices.ProvidePPPPCAOptimizedRequest;
import nl.logius.digid.dws.Application;
import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.dws.utils.SignatureValidator;
import nl.logius.digid.dws.utils.SigningHelper;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ActivateBsnkClientTest {
    protected ActivateBsnkClient client;

    protected ProvidePPPPCAOptimizedRequest request;

    protected String mockResponseBody;

    protected SigningHelper signingHelper;

    protected WireMockServer wireMockServer = new WireMockServer(BSNK_ACTIVATE_WIREMOCK_PORT);

    @Value("${ws.client.bsnk_activate.pp_ppca_response_path}")
    protected String ppResponsePath;

    // BSNK Activate Client
    @Value("${ws.client.bsnk_activate.tls_keystore}")
    protected String tlsKeystore;
    @Value("${ws.client.bsnk_activate.tls_keystore_password}")
    protected String tlsKeystorePassword;
    @Value("${ws.client.bsnk_activate.signing_keystore}")
    protected String signingKeystore;
    @Value("${ws.client.bsnk_activate.signing_keystore_password}")
    protected String signingKeystorePassword;
    @Value("${ws.client.bsnk_activate.validate_signature_truststore}")
    protected String signatureValidationTruststore;
    @Value("${ws.client.bsnk_activate.validate_signature_truststore_password}")
    protected String signatureValidationTruststorePassword;

    // Validate outgoing signatures
    @Value("${ws.client.bsnk_activate.validate_signature_truststore_password}")
    protected String signingTruststorePassword = "signing_truststore_password";
    @Value("${ws.client.bsnk_activate.signing_truststore}")
    protected String signingTruststore;

    // Sign the mock responses
    @Value("${ws.client.bsnk_activate.signature_validation_keystore_path}")
    protected String signatureValidationKeystorePath;
    @Value("${ws.client.bsnk_activate.signature_validation_keystore_password}")
    protected String signatureValidationKeystorePassword;

    public static final int BSNK_ACTIVATE_WIREMOCK_PORT = 7666;

    @Mock
    private AbstractLoggingInterceptor logInInterceptor;
    @Mock
    private AbstractLoggingInterceptor logOutInterceptor;

    @BeforeEach
    protected void init() throws IOException, DatatypeConfigurationException {
        // The Soap Mock response
        this.signingHelper = new SigningHelper(this.signatureValidationKeystorePath,
                this.signatureValidationKeystorePassword);
        String ppResponseTemplate = Files.readString(Paths.get(this.ppResponsePath));

        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        client = new ActivateBsnkClient(90000, 90000, this.tlsKeystorePassword, this.tlsKeystore,
                this.signatureValidationTruststorePassword,
                signatureValidationTruststore, signingKeystorePassword, signingKeystore,
                "http://localhost:" + BSNK_ACTIVATE_WIREMOCK_PORT + "/bsnk_stub/activateBSN", logInInterceptor,
                logOutInterceptor);

        setupPpRequest();
    }

    protected void setupPpRequest() throws DatatypeConfigurationException {
        request = new ProvidePPPPCAOptimizedRequest();

        request.setDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));

        request.setRequestID(UUID.randomUUID().toString());
        request.setRequester("digidMuOin");
        request.setRequesterKeySetVersion(BigInteger.valueOf(1l));
        request.setBSN("bsn");
    }

    protected void setupWireMock() {
        wireMockServer.start();
        wireMockServer.stubFor(post("/bsnk_stub/activateBSN")
                .willReturn(ok().withHeader("Content-Type", "text/xml").withBody(this.mockResponseBody)));
    }

    @AfterEach
    protected void stopWireMock() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    // AM1 Voer een activatie uit.
    @Test
    public void testAm1ResponseSuccess() {
        setupWireMock();

        try {
            assertEquals(2, client.providePPRequest(request).size());
        } catch (BsnkException ex) {
            fail(ex.getMessage());
        }
    }

    // AM2 AuthorizationError
    @Test
    public void testAm2ResponseBsnkFaultAuthorizationError() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_authorization_error_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    // AM3 DocumentRejected
    @Test
    public void testAm3ResponseBsnkFaultDocumentRejected() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_document_rejected_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("DocumentRejected", ex.getFaultReason());
    }

    // AM4 NotEnoughInfo
    @Test
    public void testAm4ResponseBsnkFaultNotEnoughInfo() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_not_enough_info_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("NotEnoughInfo", ex.getFaultReason());
    }

    // AM5 NotFound
    @Test
    public void testAm5ResponseBsnkFaultNotFound() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_not_found_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("NotFound", ex.getFaultReason());
    }

    // AM6 NotUnique
    @Test
    public void testAm6ResponseBsnkFaultNotUnique() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_not_unique_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("NotUnique", ex.getFaultReason());
    }

    // AM7 ProvisioningRefused
    @Test
    public void testAm7ResponseBsnkFaultProvisioningRefused() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_provisioning_refused_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("ProvisioningRefused", ex.getFaultReason());
    }

    // AM8: SyntaxError
    @Test
    public void testAm8ResponseBsnkFaultSyntax() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_syntax_error_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    // AM9 Temporarily Unavailable
    @Test
    public void testAm9ResponseBsnkFaultTemporarilyUnavailable() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_fault_temporarily_unavailable_unsigned.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    @Test
    public void testResponseInvalidBody() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_invalid_xml.xml";
        this.mockResponseBody = Files.readString(Paths.get(filePath));

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    @Test
    public void testResponseServerError() {
        wireMockServer.start();
        wireMockServer.stubFor(post("/bsnk_stub/activateBSN")
                .willReturn(serverError().withHeader("Content-Type", "text/xml").withBody("Internal server error")));

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    @Test
    public void testRequestHasCorrectSignatureAttributes() throws BsnkException {
        setupWireMock();

        client.providePPRequest(request);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/bsnk_stub/activateBSN")).withHeader("Content-Type",
                containing("xml")));
        ServeEvent serveEvent = wireMockServer.getAllServeEvents().get(0);
        String requestBody = new String(serveEvent.getRequest().getBody());
        assertTrue(requestBody
                .contains("<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\""));
        assertTrue(requestBody
                .contains("<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\""));
        assertTrue(requestBody.contains("<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\""));
        assertTrue(requestBody.contains("<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\""));
        assertTrue(requestBody.contains(
                "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"));
    }

    @Test
    public void testRequestHasAddressingProperties() throws BsnkException {
        setupWireMock();

        client.providePPRequest(request);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/bsnk_stub/activateBSN")).withHeader("Content-Type",
                containing("xml")));
        ServeEvent serveEvent = wireMockServer.getAllServeEvents().get(0);
        String requestBody = new String(serveEvent.getRequest().getBody());

        assertTrue(requestBody.contains("""
                <wsa:Action>urn:nl-gdi-eid:1.0:webservices:ProvidePP_PPCAOptimizedRequest</wsa:Action>\
                <wsa:MessageID>urn:uuid:"""));

        assertTrue(requestBody.contains("""
                </wsa:MessageID>\
                <wsa:To>http://localhost:7666/bsnk_stub/activateBSN</wsa:To>\
                <wsa:ReplyTo>\
                <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>\
                </wsa:ReplyTo>"""));
    }

    @Test
    public void testRequestHasValidSignature() throws BsnkException {
        setupWireMock();

        client.providePPRequest(request);

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/bsnk_stub/activateBSN")).withHeader("Content-Type",
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
        String ppResponseTemplate = Files.readString(Paths.get(this.ppResponsePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.X509_KEY_IDENTIFIER);

        setupWireMock();

        try {
            assertEquals(2, client.providePPRequest(request).size());
        } catch (BsnkException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testResponseWithoutSignature() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_valid_unsigned.xml";
        this.mockResponseBody = Files.readString(Paths.get(filePath));

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    @Test
    public void testResponseWithoutKeyInfo() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_no_keyinfo.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("BsnkActivateSoapFault", ex.getFaultReason());
    }

    @Test
    public void testResponseWithTamperedSignedInfoElement() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_tampered_signed_info.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("SignatureValidationFault", ex.getFaultReason());
    }

    @Test
    public void testResponseWithTamperedBodyElement() throws IOException {
        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_tampered_body.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("SignatureValidationFault", ex.getFaultReason());
    }

    @Test
    public void testResponseSigningCertNotInTruststore() throws IOException {
        this.signingHelper = new SigningHelper("src/test/resources/certs/tls_keystore/keystore.pkcs12",
                "SSSSSSSSSSSSSSSSSSS");

        String filePath = "src/test/resources/responses/ProvidePP_PPCAOptimizedResponse_tampered_body.xml";
        String ppResponseTemplate = Files.readString(Paths.get(filePath));
        this.mockResponseBody = this.signingHelper.sign(ppResponseTemplate, WSConstants.ISSUER_SERIAL);

        setupWireMock();

        BsnkException ex = assertThrows(BsnkException.class, () -> client.providePPRequest(request));
        assertEquals("SignatureValidationFault", ex.getFaultReason());
    }
}
