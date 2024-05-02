
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

package nl.logius.digid.app.domain.version;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.version.response.AppVersionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.Map;

@Service
public class AppVersionService {
    private final String minApiVersion;
    private final String maxApiVersion;

    private final AppVersionRepository appVersionRepository;
    private final DigidClient digidClient;
    private final SharedServiceClient sharedServiceClient;
    private static final String BETA = "Beta";

    @Autowired
    public AppVersionService(@Value("${minApiVersion}") String minApiVersion, @Value("${maxApiVersion}") String maxApiVersion, AppVersionRepository appVersionRepository, DigidClient digidClient, SharedServiceClient sharedServiceClient) {
        this.minApiVersion = minApiVersion;
        this.maxApiVersion = maxApiVersion;
        this.appVersionRepository = appVersionRepository;
        this.digidClient = digidClient;
        this.sharedServiceClient = sharedServiceClient;
    }


    public AppVersionResponse appVersionResponse(Status status, String apiVersion, String appVersion, String osType, String osVersion, String releaseType) throws SharedServiceClientException {
        String logCode = isIdChecker(osType) ? "1317" : "734";
        digidClient.remoteLog(logCode, Map.of("request_type", "versie controle", "statistics", String.format("API-Version=%s&OS-Type=%s&App-Version=%s&OS-Version=%s&Release-Type=%s", apiVersion, osType, appVersion, osVersion, releaseType)));

        String url = switch (osType.toUpperCase()) {
            case "ANDROID" -> sharedServiceClient.getSSConfigString("digid_app_android_store_url");
            case "IOS" -> sharedServiceClient.getSSConfigString("digid_app_ios_store_url");
            case "MACOS" -> sharedServiceClient.getSSConfigString("digid_app_macos_store_url");
            case "UWP" -> sharedServiceClient.getSSConfigString("digid_app_windows_store_url");
            default -> "unknown platform";
        };

        logStatusWarnings(status, osType, appVersion, releaseType);

        return new AppVersionResponse(status.getAction(), status.getMessage(), url, minApiVersion, maxApiVersion);
    }

    public boolean validateAppVersion(HttpServletRequest request, Status status) {
        String appVersionHeader = request.getHeader("App-Version");
        String osTypeHeader = request.getHeader("OS-Type");
        String releaseTypeHeader = request.getHeader("Release-Type");

        if (status == Status.ACTIVE ||  status == Status.UPDATE_WARNING)
            return true;


        logStatusWarnings(status, osTypeHeader, appVersionHeader, releaseTypeHeader);

        return false;
    }

    @Cacheable(value = "app-status-response", key = "{ #appVersion, #osType, #releaseType }")
    public Status checkAppStatus(String appVersion, String osType, String releaseType){
        AppVersion appVersionReference = appVersionRepository.findByVersionAndOperatingSystemAndReleaseType(appVersion, osType, releaseType).
            orElse(appVersionRepository.findByVersionAndOperatingSystemAndReleaseType(appVersion, osType, BETA).orElse(null));

        return appVersionReference != null ? getStatus(appVersionReference) : Status.KILL_APP_INVALID;
    }

    private Status getStatus(AppVersion appVersion) {
        if (appVersion.getNotValidBefore() == null || killAppNotValidBefore(appVersion.getNotValidBefore())) return Status.KILL_APP_INVALID;
        else if (appVersion.getKillAppOnOrAfter() != null && killAppExpired(appVersion.getKillAppOnOrAfter())) return Status.KILL_APP_EXPIRED;
        else if (appVersion.getNotValidOnOrAfter() != null && forceUpdate(appVersion.getNotValidOnOrAfter())) return Status.FORCE_UPDATE;
        else if (higherActiveAppVersionExists(appVersion)) return Status.UPDATE_WARNING;
        else return Status.ACTIVE;
    }

    private boolean killAppNotValidBefore(ZonedDateTime notValidBefore){
        return ZonedDateTime.now().isBefore(notValidBefore);
    }

    private boolean killAppExpired(ZonedDateTime killAppOnOrAfter) {
        return ZonedDateTime.now().isAfter(killAppOnOrAfter);
    }

    private boolean forceUpdate(ZonedDateTime notValidOnOrAfter) {
        return ZonedDateTime.now().isAfter(notValidOnOrAfter);
    }

    private boolean higherActiveAppVersionExists(AppVersion appVersion) {
        return appVersionRepository.hasHigherActiveAppVersion(appVersion.getVersion(), appVersion.getOperatingSystem(), appVersion.getReleaseType(), appVersion.getId()) > 0;
    }

    private void logStatusWarnings(Status status, String osType, String appVersion, String releaseType){
        String logCode;
        if ((logCode = getLoggingNumber(status, osType)) != null) {
            digidClient.remoteLog(logCode, Map.of(
                "request_type", "versie controle",
                "version", appVersion,
                "operating_system", osType,
                "release_type", releaseType));
        }
    }

    private String getLoggingNumber(Status status, String osType) {
        return switch (status) {
            case KILL_APP_INVALID -> isIdChecker(osType) ? "1315" : "772";
            case KILL_APP_EXPIRED -> isIdChecker(osType) ? "1314" : "771";
            case FORCE_UPDATE -> isIdChecker(osType) ? "1313" : "770";
            case UPDATE_WARNING -> isIdChecker(osType) ? "1312" : "769";
            default -> null;
        };
    }

    private boolean isIdChecker(String osType) {
        return osType.startsWith("idChecker");
    }
}
