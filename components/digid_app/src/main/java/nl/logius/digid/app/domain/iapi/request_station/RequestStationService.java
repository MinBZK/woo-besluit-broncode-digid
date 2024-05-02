
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

package nl.logius.digid.app.domain.iapi.request_station;

import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.authenticator.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionRepository;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.exceptions.SwitchDisabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

@Service
public class RequestStationService {

    private final AppSessionRepository sessionRepository;
    private final RequestStationTransactionRepository requestStationTransactionRepository;
    private final AppAuthenticatorRepository appAuthenticatorRepository;
    private final AppAuthenticatorService appAuthenticatorService;
    private final DigidClient digidClient;
    private final SharedServiceClient sharedServiceClient;
    private final SwitchService switchService;
    private static final String ACCOUNT_ID_WITH_BSN = "account_id_with_bsn";

    @Autowired
    public RequestStationService(AppSessionRepository sessionRepository, AppAuthenticatorRepository appAuthenticatorRepository, AppAuthenticatorService appAuthenticatorService, DigidClient digidClient, RequestStationTransactionRepository requestStationTransactionRepository, SharedServiceClient sharedServiceClient, SwitchService switchService) {
        this.sessionRepository = sessionRepository;
        this.appAuthenticatorRepository = appAuthenticatorRepository;
        this.appAuthenticatorService = appAuthenticatorService;
        this.digidClient = digidClient;
        this.requestStationTransactionRepository = requestStationTransactionRepository;
        this.sharedServiceClient = sharedServiceClient;
        this.switchService = switchService;
    }

    public Map<String, String> validateAppActivation(ValidateAppActivationRequest request) throws SharedServiceClientException {

        checkSwitchesEnabled();

        long requestStationTransactionExpirationInMinutes = sharedServiceClient.getSSConfigLong("RvIG-Aanvraagstation_session_expiration");
        RequestStationTransaction requestStationTransaction = new RequestStationTransaction(requestStationTransactionExpirationInMinutes * 60);
        requestStationTransaction.setTransactionId(request.getIapi().getTransactionId());

        digidClient.remoteLog("1384", Map.of(TRANSACTION_ID, request.getIapi().getTransactionId(), "aanvraagstationid", request.getIapi().getStationId()));

        Map<String, String> resultAccountStanding = digidClient.accountStanding(request.getIapi().getBsn());

        String status = "NOK";
        String errorCode;

        if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("invalid")) {
            digidClient.remoteLog("1394", Map.of(lowerUnderscore(ACCOUNT_ID), resultAccountStanding.get(ACCOUNT_ID_WITH_BSN)));

            if (resultAccountStanding.get("initial").equals("true")) {
                digidClient.remoteLog("1385", Map.of(lowerUnderscore(ACCOUNT_ID), resultAccountStanding.get("account_id_with_bsn_initial")));
            }

            errorCode = "E_DIGID_ACCOUNT_NOT_VALID";
        } else if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("existing")) {
            digidClient.remoteLog("1394", Map.of(lowerUnderscore(ACCOUNT_ID),resultAccountStanding.get(ACCOUNT_ID_WITH_BSN)));
            Map<String, String> resultBrpCheck = checkDocumentInBrp(requestStationTransaction,request.getIapi().getBsn(), request.getIapi().getDocumentNumber());
            status = resultBrpCheck.get(lowerUnderscore(STATUS));
            errorCode = resultBrpCheck.get(lowerUnderscore(ERROR_CODE));
        } else if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("new")) {
            Map<String, String> resultBrpCheck = checkDocumentInBrp(requestStationTransaction, request.getIapi().getBsn(), request.getIapi().getDocumentNumber());
            status = resultBrpCheck.get(lowerUnderscore(STATUS));
            errorCode = resultBrpCheck.get(lowerUnderscore(ERROR_CODE));
        } else {
            errorCode = "E_GENERAL";
        }

        requestStationTransaction.setBsn(request.getIapi().getBsn());
        requestStationTransaction.setDocumentNumber(request.getIapi().getDocumentNumber());
        requestStationTransactionRepository.save(requestStationTransaction);

        Map<String, String> body = new HashMap<>();
        body.put(lowerUnderscore(STATUS), status);
        body.put("message", errorCode);
        return body;
    }

    public Map<String, String> applyOnlyOrApplyAndActivateApp(AppActivationRequest request) throws SharedServiceClientException {

        checkSwitchesEnabled();

        String status;
        String errorCode = null;
        AppSession appSession = null;

        digidClient.remoteLog("1389", Map.of(TRANSACTION_ID, request.getIapi().getTransactionId()));

        Optional<AppSession> optionalAppSession = sessionRepository.findByAppActivationCode(request.getIapi().getActivationCode());
        if (optionalAppSession.isPresent()) {
            appSession =  optionalAppSession.get();
        }
        Optional<RequestStationTransaction> requestStationTransaction = requestStationTransactionRepository.findByTransactionId(request.getIapi().getTransactionId());

        if (appSession != null && requestStationTransaction.isPresent()){
            String deviceName = appSession.getDeviceName();
            String instanceId = appSession.getInstanceId();
            String userAppId = appSession.getUserAppId();
            String bsn = requestStationTransaction.get().getBsn();
            String documentType = requestStationTransaction.get().getDocumentType();
            boolean authenticated = appSession.isAuthenticated();

            Map<String, String> resultAccountStanding = digidClient.accountStanding(bsn);

            if (authenticated) {
                long accountId = Long.parseLong(resultAccountStanding.get("account_id_active"));
                createAppAuthenticator(accountId, userAppId, "pending", deviceName, instanceId, documentType, null, null, null);

                if (tooMuchApps(accountId, instanceId)) {
                    digidClient.remoteLog("1386", Map.of(lowerUnderscore(ACCOUNT_ID), accountId));
                    appSession.setActivationStatus(TOO_MANY_APPS);
                } else {
                    digidClient.remoteLog("1393", Map.of(TRANSACTION_ID, request.getIapi().getTransactionId()));
                    appSession.setActivationStatus("OK");
                }

                status = "OK";
            } else {
                if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("invalid")) {
                    appSession.setActivationStatus("NOK");
                    status = "NOK";
                    errorCode = "E_DIGID_ACCOUNT_NOT_VALID";
                } else if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("existing")) {
                    Map<String, String>  resultCreateAppActivationLetter = createAppActivationLetter(appSession, bsn, false, userAppId, deviceName, instanceId, documentType);
                    status = resultCreateAppActivationLetter.get(lowerUnderscore(STATUS));
                    errorCode = resultCreateAppActivationLetter.get(ERROR_CODE);

                    if (resultCreateAppActivationLetter.get(lowerUnderscore(STATUS)).equals("OK")) {
                        long accountId = Long.parseLong(resultAccountStanding.get(ACCOUNT_ID_WITH_BSN));
                        if (tooMuchApps(accountId, instanceId)) {
                            digidClient.remoteLog("1386", Map.of(lowerUnderscore(ACCOUNT_ID), accountId));
                            appSession.setActivationStatus(TOO_MANY_APPS);
                        }
                    }
                } else if (resultAccountStanding.get(lowerUnderscore(STANDING)).equals("new")) {
                    Map<String, String>  resultCreateAppActivationLetter = createAppActivationLetter(appSession, bsn, true, userAppId, deviceName, instanceId, documentType);
                    status = resultCreateAppActivationLetter.get(lowerUnderscore(STATUS));
                    errorCode = resultCreateAppActivationLetter.get(ERROR_CODE);
                } else {
                    appSession.setActivationStatus("NOK");
                    status = "NOK";
                    errorCode = "E_GENERAL";
                }
            }
            sessionRepository.save(appSession);
        } else {
            status = "NOK";
            errorCode = "E_APP_ACTIVATION_CODE_NOT_FOUND";
            digidClient.remoteLog("1391");
            if (appSession != null) {
                appSession.setActivationStatus("NOK");
            }
        }

        Map<String, String> body = new HashMap<>();
        body.put(lowerUnderscore(STATUS), status);
        body.put("message", errorCode);
        return body;
    }

    private boolean tooMuchApps(Long accountId, String instanceId) throws SharedServiceClientException {
        int maxAppsPerUser = sharedServiceClient.getSSConfigInt("Maximum_aantal_DigiD_apps_eindgebruiker");
        return appAuthenticatorService.countByAccountIdAndInstanceIdNot(accountId, instanceId) >= maxAppsPerUser;
    }

    private Map<String, String> createAppActivationLetter(AppSession appSession, String bsn, boolean createAccount, String userAppId, String deviceName, String instanceId, String documentType) throws SharedServiceClientException {
        Map<String, String> result = new HashMap<>();

        String geldigheidsTermijn = String.valueOf(sharedServiceClient.getSSConfigInt("geldigheid_brief"));

        Map<String, String> resultCreateActivationLetter = digidClient.createActivationLetter(bsn, createAccount);

        if (resultCreateActivationLetter.get(lowerUnderscore(STATUS)).equals("OK")) {
            createAppAuthenticator(Long.valueOf(resultCreateActivationLetter.get(lowerUnderscore(ACCOUNT_ID))), userAppId, "pending", deviceName, instanceId, documentType, resultCreateActivationLetter.get("controle_code"), geldigheidsTermijn, ZonedDateTime.now());

            appSession.setAccountId(Long.valueOf(resultCreateActivationLetter.get(lowerUnderscore(ACCOUNT_ID))));
            appSession.setActivationStatus("OK");
            digidClient.remoteLog("1393");
            digidClient.remoteLog("905", Map.of(lowerUnderscore(ACCOUNT_ID), Long.valueOf(resultCreateActivationLetter.get(lowerUnderscore(ACCOUNT_ID))), "app_code", instanceId.substring(0,6).toUpperCase(), "device_name", deviceName));
        } else {
            appSession.setActivationStatus("NOK");
        }

        result.put(lowerUnderscore(STATUS), resultCreateActivationLetter.get(lowerUnderscore(STATUS)) );
        result.put(ERROR_CODE, resultCreateActivationLetter.get(lowerUnderscore(ERROR_CODE)) );
        return result;
    }

    private AppAuthenticator createAppAuthenticator(Long accountId,String userAppId, String status, String deviceName, String instanceId, String documentType, String activationCode, String geldigheidsTermijn, ZonedDateTime requestedAt){
        removeExistingAppsWithInstanceId(instanceId);

        AppAuthenticator authenticator = new AppAuthenticator();
        authenticator.setAccountId(accountId);
        authenticator.setUserAppId(userAppId);
        authenticator.setStatus(status);
        authenticator.setDeviceName(deviceName);
        authenticator.setIssuerType(GEMEENTEBALIE);
        authenticator.setInstanceId(instanceId);
        authenticator.setSubstantieelActivatedAt(ZonedDateTime.now());
        authenticator.setSubstantieelDocumentType(documentType);

        authenticator.setActivationCode(activationCode);
        authenticator.setGeldigheidstermijn(geldigheidsTermijn);
        authenticator.setRequestedAt(requestedAt);

        appAuthenticatorRepository.save(authenticator);
        return authenticator;
    }

    private void removeExistingAppsWithInstanceId(String instanceId) {
        appAuthenticatorRepository.removeByInstanceId(instanceId);
    }

    private Map<String, String> checkDocumentInBrp(RequestStationTransaction requestStationTransaction, String bsn, String documentNumber) {
        Map<String, String> result = new HashMap<>();
        Map<String, String> resultCheckDocumentInBrp = digidClient.checkDocumentInBrp(bsn, documentNumber);

        if (resultCheckDocumentInBrp.get(lowerUnderscore(STATUS)).equals("OK")) {
            requestStationTransaction.setDocumentType(resultCheckDocumentInBrp.get("document_type"));
            result.put(lowerUnderscore(STATUS), "OK");
            result.put(lowerUnderscore(ERROR_CODE), null);
        } else {
            result.put(lowerUnderscore(STATUS), "NOK");
            result.put(lowerUnderscore(ERROR_CODE), resultCheckDocumentInBrp.get(lowerUnderscore(ERROR_CODE)));
        }
        return result;
    }

    private void checkSwitchesEnabled() {
        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("1397", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        } else if (!switchService.digidRequestStationEnabled()) {
            digidClient.remoteLog("1398", Map.of(lowerUnderscore(HIDDEN), true));
            throw new SwitchDisabledException();
        }
    }
}
