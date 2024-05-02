
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

package nl.logius.digid.app.domain.iapi.rda;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.DwsClient;
import nl.logius.digid.app.domain.activation.flow.flows.UpgradeLoginLevel;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import nl.logius.digid.app.shared.logcodes.WidErrorLogCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

@Service
public class RdaService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AppSessionService appSessionService;
    private final AppAuthenticatorService appAuthenticatorService;
    private final SwitchService switchService;
    private final DigidClient digidClient;
    private final DwsClient dwsClient;
    private final IdCheckDocumentRepository idCheckDocumentRepository;

    private static final String ID_CHECK_ACTION = "upgrade_rda_widchecker";
    private static final String SCANNING_FOREIGN = "SCANNING_FOREIGN";
    private static final String SCANNING = "SCANNING";


    @Autowired
    public RdaService(AppSessionService appSessionService, AppAuthenticatorService appAuthenticatorService,
                      SwitchService switchService, DigidClient digidClient, DwsClient dwsClient, IdCheckDocumentRepository idCheckDocumentRepository) {
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.switchService = switchService;
        this.digidClient = digidClient;
        this.dwsClient = dwsClient;
        this.idCheckDocumentRepository = idCheckDocumentRepository;
    }

    public Map<String, String> confirm(RdaConfirmRequest params) {
        AppSession appSession = appSessionService.getSession(params.getAppSessionId());
        AppAuthenticator appAuthenticator = appAuthenticatorService.findByUserAppId(appSession.getUserAppId());

        if(!checkSecret(params, appSession) || !checkAccount(params, appSession)){
            appSession.setRdaSessionStatus("ABORTED");
            appSessionService.save(appSession);
            return Map.of("arrivalStatus", "NOK");
        }
        if(checkAndProcessError(params, appSession)){
            appSessionService.save(appSession);
            return Map.of("arrivalStatus", "OK");
        }

        if (!switchService.digidAppSwitchEnabled()) {
            digidClient.remoteLog("853",
                Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            appSession.setRdaSessionStatus("REFUTED");
        } else if (!switchService.digidRdaSwitchEnabled()){
            digidClient.remoteLog("579",
                Map.of(lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(), lowerUnderscore(HIDDEN), true));
            appSession.setRdaSessionStatus("REFUTED");
        } else if (params.isVerified() && (SCANNING.equals(appSession.getRdaSessionStatus()) ||
                                            SCANNING_FOREIGN.equals(appSession.getRdaSessionStatus()))) {
            appSession.setRdaSessionStatus("VERIFIED");
            appAuthenticator.setSubstantieelActivatedAt(ZonedDateTime.now());
            appAuthenticator.setSubstantieelDocumentType(params.getDocumentType().toLowerCase());

            if (appAuthenticator.getWidActivatedAt() == null) {
                appAuthenticator.setIssuerType("rda");
            }

            storeIdCheckDocument(params.getDocumentNumber(), params.getDocumentType(), appSession.getAccountId(), appAuthenticator.getUserAppId());

            if (ID_CHECK_ACTION.equals(appSession.getRdaAction())) {
                digidClient.remoteLog("1321",
                    Map.of("document_type", params.getDocumentType().toLowerCase(), lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId()));
            } else {
                digidClient.remoteLog("848",
                    Map.of("document_type", params.getDocumentType().toLowerCase(),
                        lowerUnderscore(ACCOUNT_ID), appAuthenticator.getAccountId(),
                        lowerUnderscore(APP_CODE), appAuthenticator.getAppCode(),
                        lowerUnderscore(DEVICE_NAME), appAuthenticator.getDeviceName()));
            }

            appAuthenticatorService.save(appAuthenticator);

            if(appSession.getFlow().equals(UpgradeLoginLevel.NAME)) {
                digidClient.sendNotificationMessage(appSession.getAccountId(), "ED024", "SMS20");
                logger.debug("Sending notify email ED024 / SMS20 for device {}", appAuthenticator.getDeviceName());
            }
        }
        appSession.setAppAuthenticationLevel(appAuthenticator.getAuthenticationLevel());
        appSessionService.save(appSession);
        return Map.of("arrivalStatus", "OK");
    }

    private void storeIdCheckDocument(String documentNumber, String documentType, Long accountId, String userAppId) {
        var document = new IdCheckDocument();
        document.setDocumentType(documentType);
        document.setDocumentNumber(documentNumber);
        document.setAccountId(accountId);
        document.setUserAppId(userAppId);
        idCheckDocumentRepository.save(document);
    }

    private boolean checkAndProcessError(RdaConfirmRequest params, AppSession appSession){
        String error = params.getError();
        if(error == null && SCANNING_FOREIGN.equals(appSession.getRdaSessionStatus())){
            error = checkForeignDocuments(params, appSession);
        }
        return processError(error, appSession);
    }

    private String checkForeignDocuments(RdaConfirmRequest params, AppSession appSession){
        if(!checkBsnOrIdentifier(params, appSession))
            return "bsn.error";

        digidClient.remoteLog("1489", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(HIDDEN), true));

        if(!checkBvBsn(appSession))
            return "bvbsn.error";

        return null;
    }

    private boolean checkBsnOrIdentifier(RdaConfirmRequest params, AppSession appSession){
        if (params.getBsn() != null){
            return digidClient.checkBsn(appSession.getAccountId(), params.getBsn()).get(STATUS).equals("OK");
        }

        logger.debug("brp_identifier: {}", appSession.getBrpIdentifier());
        logger.debug("mrz_identifier: {}", params.getMrzIdentifier());
        return appSession.getBrpIdentifier() != null && appSession.getBrpIdentifier().startsWith(params.getMrzIdentifier());
    }

    private boolean checkBvBsn(AppSession appSession){
        Map<String, String> response = dwsClient.checkBvBsn(appSession.getRdaDocumentType(), appSession.getRdaDocumentNumber());
        digidClient.remoteLog("1490", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(HIDDEN), true));

        return response.get(STATUS).equals("OK");
    }

    private boolean processError(String error, AppSession appSession){
        if(error != null) {
            digidClient.remoteLog(WidErrorLogCodes.getLogCode(error),
                Map.of("status_code", error, lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), lowerUnderscore(HIDDEN), true));
            if (error.equals("CANCELLED")) {
                appSession.setRdaSessionStatus("CANCELLED");
            } else if (error.equals("bsn.error")) {
                appSession.setRdaSessionStatus("BSN_NOT_MATCHING");
            } else {
                appSession.setRdaSessionStatus("REFUTED");
            }
            return true;
        }
        return false;
    }

    private boolean checkSecret(RdaConfirmRequest params, AppSession appSession){
        if(params.getSecret().equals(appSession.getConfirmSecret()))
            return true;

        logger.info("RdaController > Confirm without correct secret, with error type {}", params.getError());
        return false;
    }

    private boolean checkAccount(RdaConfirmRequest params, AppSession appSession){
        if(appSession.getAccountId() != null){
            return true;
        }
        logger.info("RdaController > No account available, with error type {}", params.getError());
        return false;
    }
}
