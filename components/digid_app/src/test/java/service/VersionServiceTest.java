
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

package service;

import helpers.MyHttpServletResponseWrapper;
import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.version.*;
import nl.logius.digid.app.domain.version.response.AppVersionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class VersionServiceTest {

    private static final String API_VERSION = "1";
    private static final String VERSION = "5.18.0";
    private static final String OPERATING_SYSTEM = "Android";
    private static final String OPERATING_SYSTEM_IOS = "IOS";
    private static final String OPERATING_SYSTEM_VERSION = "10";
    private static final String RELEASE_TYPE = "Productie";
    private static final String RELEASE_TYPE_BETA = "Beta";
    private static final String KILL_APP_INVALID_LOG = "772";
    private static final String KILL_APP_EXPIRED_LOG = "771";
    private static final String FORCED_UPDATE_LOG = "770";
    private static final String UPDATE_WARNING_LOG = "769";

    @Mock
    private AppVersionRepository appVersionRepositoryMock;

    @Mock
    private SharedServiceClient sharedServiceClient;

    @Mock
    private DigidClient digidClient;

    @InjectMocks
    private AppVersionService appVersionService;

    @Test
    void getAppVersionValidReponseTest() throws SharedServiceClientException {
        appVersionService = new AppVersionService("2", "3", appVersionRepositoryMock, digidClient, sharedServiceClient);

        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.of(getValidAppVersion()));
        when(sharedServiceClient.getSSConfigString("digid_app_android_store_url")).thenReturn("https://play.google.com/store/apps/details?id=nl.rijksoverheid.digid");
        Status status = appVersionService.checkAppStatus(VERSION, OPERATING_SYSTEM, RELEASE_TYPE);
        AppVersionResponse appVersionResponse = appVersionService.appVersionResponse(status, API_VERSION, VERSION, OPERATING_SYSTEM, OPERATING_SYSTEM_VERSION, RELEASE_TYPE);

        assertEquals("active", appVersionResponse.getAction());
        assertEquals("", appVersionResponse.getMessage());
        assertEquals("3", appVersionResponse.getMaxApiVersion());
    }

    @ParameterizedTest
    @MethodSource("getOperatingSystemStoreUrl")
    void getAppVersionKillAppResponseTest(String operatingSystem, String ssStoreUrl, String storeUrl) throws SharedServiceClientException {
        appVersionService = new AppVersionService("2", "3", appVersionRepositoryMock, digidClient, sharedServiceClient);

        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, operatingSystem, RELEASE_TYPE)).thenReturn(Optional.empty());
        when(sharedServiceClient.getSSConfigString(ssStoreUrl)).thenReturn(storeUrl);

        Status status = appVersionService.checkAppStatus(VERSION, OPERATING_SYSTEM, RELEASE_TYPE);
        AppVersionResponse appVersionResponse = appVersionService.appVersionResponse(status, API_VERSION, VERSION, operatingSystem, OPERATING_SYSTEM_VERSION, RELEASE_TYPE);

        assertEquals("kill_app", appVersionResponse.getAction());
        assertEquals("De versie die u gebruikt is niet geldig. U kunt de DigiD app opnieuw downloaden en activeren in Mijn DigiD.", appVersionResponse.getMessage());
        assertEquals(storeUrl, appVersionResponse.getUpdateUrl());
        assertEquals("3", appVersionResponse.getMaxApiVersion());
    }

    @Test
    void validAppVersionTest() throws IOException {
        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.of(getValidAppVersion()));

        HttpServletResponse httpServletResponse = mockResponse();
        boolean validAppVersion = appVersionService.validateAppVersion(mockRequest(), Status.ACTIVE);

        assertTrue(validAppVersion);
        assertEquals("", httpServletResponse.toString());
    }

    @Test
    void validAppVersionBetaRequestTest() throws IOException {
        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE_BETA)).thenReturn(Optional.of(getValidAppVersion()));

        MockHttpServletRequest httpServletRequest = (MockHttpServletRequest) mockRequest();
        httpServletRequest.removeHeader("Release-Type");
        httpServletRequest.addHeader("Release-Type", RELEASE_TYPE_BETA);
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(httpServletRequest, Status.ACTIVE);

        assertTrue(validAppVersion);
        assertEquals("", httpServletResponse.toString());
    }

    @Test
    void validAppVersionProductionIfBetaExistsRequestTest() throws IOException {
        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE_BETA)).thenReturn(Optional.of(getValidAppVersion()));

        MockHttpServletRequest httpServletRequest = (MockHttpServletRequest) mockRequest();
        httpServletRequest.removeHeader("Release-Type");
        httpServletRequest.addHeader("Release-Type", RELEASE_TYPE);
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(httpServletRequest, Status.ACTIVE);

        assertTrue(validAppVersion);
        assertEquals("", httpServletResponse.toString());
    }

    @Test
    void validAppVersionUpdateWarningTest() throws IOException {
        AppVersion appVersion = getValidAppVersion();

        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.of(appVersion));
        when(appVersionRepositoryMock.hasHigherActiveAppVersion(VERSION, OPERATING_SYSTEM, RELEASE_TYPE, appVersion.getId())).thenReturn(1);
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(mockRequest(), Status.ACTIVE);

        assertTrue(validAppVersion);
        assertEquals("", httpServletResponse.toString());
    }

    @Test
    void invalidAppVersionNonexistentTest() throws IOException {
        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.empty());
        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE_BETA)).thenReturn(Optional.empty());
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(mockRequest(), Status.KILL_APP_INVALID);

        assertFalse(validAppVersion);
//        assertEquals(new Gson().toJson(new AppVersionResponse(Status.KILL_APP_INVALID.getAction(), Status.KILL_APP_INVALID.getMessage())), httpServletResponse.toString());
        verify(digidClient, times(1)).remoteLog(KILL_APP_INVALID_LOG,
            Map.of("request_type", "versie controle", "version", VERSION, "operating_system", OPERATING_SYSTEM, "release_type", RELEASE_TYPE));

    }

    @Test
    void invalidAppVersionExpiredTest() throws IOException {
        AppVersion appVersion = getValidAppVersion();
        appVersion.setKillAppOnOrAfter(ZonedDateTime.now().minus(1, ChronoUnit.DAYS));

        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.of(appVersion));
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(mockRequest(), Status.KILL_APP_EXPIRED);

        assertFalse(validAppVersion);
//        assertEquals(new Gson().toJson(new AppVersionResponse(Status.KILL_APP_EXPIRED.getAction(), Status.KILL_APP_EXPIRED.getMessage())), httpServletResponse.toString());
        verify(digidClient, times(1)).remoteLog(KILL_APP_EXPIRED_LOG,
            Map.of("request_type", "versie controle", "version", VERSION, "operating_system", OPERATING_SYSTEM, "release_type", RELEASE_TYPE));

    }

    @Test
    void invalidAppVersionUpdateRequiredTest() throws IOException {
        AppVersion appVersion = getValidAppVersion();
        appVersion.setNotValidOnOrAfter(ZonedDateTime.now().minus(1, ChronoUnit.DAYS));

        when(appVersionRepositoryMock.findByVersionAndOperatingSystemAndReleaseType(VERSION, OPERATING_SYSTEM, RELEASE_TYPE)).thenReturn(Optional.of(appVersion));
        HttpServletResponse httpServletResponse = mockResponse();

        boolean validAppVersion = appVersionService.validateAppVersion(mockRequest(), Status.FORCE_UPDATE);

        assertFalse(validAppVersion);
//        assertEquals(new Gson().toJson(new AppVersionResponse(Status.FORCE_UPDATE.getAction(), Status.FORCE_UPDATE.getMessage())), httpServletResponse.toString());
        verify(digidClient, times(1)).remoteLog(FORCED_UPDATE_LOG,
            Map.of("request_type", "versie controle", "version", VERSION, "operating_system", OPERATING_SYSTEM, "release_type", RELEASE_TYPE));

    }

    private HttpServletRequest mockRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("OS-Type", OPERATING_SYSTEM);
        mockRequest.addHeader("App-Version", VERSION);
        mockRequest.addHeader("Release-Type", RELEASE_TYPE);

        return mockRequest;
    }

    private HttpServletResponse mockResponse() {
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        return new MyHttpServletResponseWrapper(mockResponse);
    }

    private AppVersion getValidAppVersion() {
        AppVersion appVersion = new AppVersion();
        appVersion.setId(1L);
        appVersion.setKillAppOnOrAfter(null);
        appVersion.setKillAppOnOrAfter(null);
        appVersion.setNotValidBefore(ZonedDateTime.now());
        appVersion.setNotValidOnOrAfter(null);
        appVersion.setOperatingSystem(OPERATING_SYSTEM);
        appVersion.setReleaseType(RELEASE_TYPE);
        appVersion.setVersion(VERSION);
        return appVersion;
    }

    private static Stream<Arguments> getOperatingSystemStoreUrl() {
        return Stream.of(
            Arguments.of("Android", "digid_app_android_store_url", "https://play.google.com/store/apps/details?id=nl.rijksoverheid.digid"),
            Arguments.of("iOS", "digid_app_ios_store_url", "https://appstore.com/digid"),
            Arguments.of("macOS", "digid_app_macos_store_url", "https://appstore.com/nl.rijksoverheid.digid"),
            Arguments.of("UWP", "digid_app_windows_store_url", "https://www.microsoft.com/nl-nl/windows/windows-10-apps")
            );
    }
}
