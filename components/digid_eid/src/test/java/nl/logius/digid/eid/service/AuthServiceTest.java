
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

package nl.logius.digid.eid.service;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.CardPolymorph;
import nl.logius.digid.card.crypto.CmsVerifier;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.models.rest.RequestHeader;
import nl.logius.digid.eid.models.rest.Version;
import nl.logius.digid.eid.models.rest.app.*;
import nl.logius.digid.eid.models.rest.digid.Confirmation;
import nl.logius.digid.eid.repository.EidSessionRepository;
import nl.logius.digid.eid.security.SecurityFactory;
import nl.logius.digid.eid.util.GetApduResponseFunction;
import nl.logius.digid.eid.validations.IpValidations;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.smartcardio.ResponseAPDU;
import java.util.Optional;
import java.util.stream.Stream;

import static nl.logius.digid.eid.models.PolymorphType.PIP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AuthServiceTest {
    private static BsnkService bsnkService;
    private static IpValidations ipValidations;
    private static ConfirmService confirmService;
    private static Asn1ObjectMapper mapper;
    private static CmsVerifier cmsVerifier;
    private static CVCertificateService cvCertificateService;
    private static SignatureService signatureService;
    private static SecurityFactory securityFactory;
    private static EidSessionRepository sessionRepo;
    private static AuthService nikService, rdwService;

    private final String localhost = "127.0.0.1";

    /**
     * @Mock does not work in static context -> create manually
     */
    static {
        bsnkService = mock(BsnkService.class);
        ipValidations = mock(IpValidations.class);
        confirmService = mock(ConfirmService.class);
        mapper = mock(Asn1ObjectMapper.class);
        cmsVerifier = mock(CmsVerifier.class);
        cvCertificateService = mock(CVCertificateService.class);
        signatureService = mock(SignatureService.class);
        securityFactory = mock(SecurityFactory.class);
        sessionRepo = mock(EidSessionRepository.class);
        nikService = new NIKService(bsnkService, ipValidations, confirmService, mapper, cmsVerifier, cvCertificateService, signatureService, securityFactory, sessionRepo);
        rdwService = new RDWService(bsnkService, ipValidations, confirmService, mapper, cmsVerifier, cvCertificateService, signatureService, securityFactory, sessionRepo);
        nikService = spy(nikService);
        rdwService = spy(rdwService);
    }

    /**
     * Clear all registered invocations between parameterised tests
     */
    @BeforeEach
    public void clearSpyInvocations() {
        clearInvocations(bsnkService, ipValidations, confirmService, mapper, cmsVerifier, cvCertificateService, signatureService, securityFactory, sessionRepo);
    }

    @ParameterizedTest
    @MethodSource("getAuthServiceExtendersWithDocumentType")
    public void getCertificateRestServiceTest(AuthService authService, byte[] documentType) {
        EidSession session = new EidSession();
        GetCertificateRequest request = createCertificateRequest();
        request.setDocumentType(documentType);
        Certificate certificate = new Certificate();
        certificate.setSubject("Subject");
        certificate.setRaw("certificate".getBytes());

        doReturn(session).when(authService).initSession(any(AppRequest.class), anyString(), any(AppResponse.class));
        when(sessionRepo.save(any(EidSession.class))).thenReturn(null);
        when(cvCertificateService.getAt(any(DocumentType.class), any(PolymorphType.class))).thenReturn(certificate);

        GetCertificateResponse result = authService.getCertificateRestService(request, localhost);

        assertNull(result.getSessionId());
        assertEquals("OK", result.getStatus());
    }

    @ParameterizedTest
    @MethodSource("getAuthServiceExtenders")
    public void getPolymorphicDataRestServiceTest(AuthService authService) {
        EidSession session = new EidSession();
        AppRequest appRequest = createCertificateRequest();
        PolyDataResponse response = new PolyDataResponse();
        GetApduResponseFunction<AppRequest> func = (AppRequest r) -> new ResponseAPDU("responseApdu".getBytes());

        when(mapper.read(any(byte[].class), eq(CardPolymorph.class))).thenReturn(new CardPolymorph());

        PolyDataResponse result = authService.getPolymorphicDataRestService(appRequest, response, session, func);

        assertEquals("OK", result.getResult());
        assertEquals("OK", result.getStatus());
        assertNull(result.getSessionId());
        verify(confirmService, times(1)).sendAssertion(eq(session.getReturnUrl()), eq(session.getConfirmId()), eq(session.getConfirmSecret()),
            eq(session.getUserConsentType()), any(Confirmation.class));
        verify(sessionRepo, times(1)).delete(session);

    }

    @ParameterizedTest
    @MethodSource("getAuthServiceExtenders")
    public void initSessionTest(AuthService authService) {
        EidSession session = new EidSession();
        session.setId("1");
        session.setClientIpAddress(localhost);
        AppRequest request = createCertificateRequest();
        AppResponse response = new GetCertificateResponse();

        when(sessionRepo.findById(anyString())).thenReturn(Optional.of(session));

        EidSession result = authService.initSession(request, localhost, response);

        assertEquals("1", result.getId());
        assertEquals(session, result);
        verify(authService, times(1)).verifyClient(eq(session), eq(localhost));
    }

    @ParameterizedTest
    @MethodSource("getAuthServiceExtenders")
    public void verifyClientTest(AuthService authService) {
        EidSession session = new EidSession();
        session.setId("1");
        session.setClientIpAddress(localhost);

        authService.verifyClient(session, localhost);

        verify(ipValidations, times(1)).ipCheck(eq(session), eq(localhost));
    }

    private GetCertificateRequest createCertificateRequest() {
        GetCertificateRequest request = new GetCertificateRequest();
        request.setUserConsentType(PIP);
        Version version = new Version();
        version.setMinor("1");
        version.setMajor("1");
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.setSessionId("");
        requestHeader.setSupportedAPIVersion(version);
        request.setHeader(requestHeader);
        return request;
    }

    private static Stream<Arguments> getAuthServiceExtenders() {
        return Stream.of(
            Arguments.of(nikService),
            Arguments.of(rdwService)
        );
    }

    private static Stream<Arguments> getAuthServiceExtendersWithDocumentType() {
        return Stream.of(
            Arguments.of(nikService, Hex.decode("")),
            Arguments.of(rdwService, Hex.decode("SSSSSSSSSSSSSSSSSSSSSS"))
        );
    }
}
