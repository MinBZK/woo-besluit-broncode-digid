
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
import nl.logius.digid.app.domain.activation.flow.flows.ActivateAppWithPasswordRdaFlow;
import nl.logius.digid.app.domain.activation.flow.flows.UndefinedFlow;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import nl.logius.digid.app.domain.switches.SwitchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class RdaServiceTest {
    private AppSession appSession;
    private AppAuthenticator appAuthenticator;
    private RdaConfirmRequest rdaConfirmRequest;

    private static final Long T_ACCOUNT_ID = 1234L;

    @Mock
    private AppSessionService appSessionService;

    @Mock
    private AppAuthenticatorService appAuthenticatorService;

    @Mock
    private SwitchService switchService;

    @Mock
    private DigidClient digidClient;

    @Mock
    private DwsClient dwsClient;

    @Mock
    private IdCheckDocumentRepository idCheckDocumentRepository;

    @InjectMocks
    RdaService rdaService;

    @BeforeEach
    protected void setup(){
        appSession = new AppSession();
        appAuthenticator = new AppAuthenticator();
        appAuthenticator.setAccountId(T_ACCOUNT_ID);
        rdaConfirmRequest = new RdaConfirmRequest();

        appSession.setConfirmSecret("secret1");
        appSession.setAccountId(T_ACCOUNT_ID);
        appSession.setRdaSessionStatus("SCANNING");
        appSession.setFlow(ActivateAppWithPasswordRdaFlow.NAME);
        rdaConfirmRequest.setSecret("secret1");
        rdaConfirmRequest.setBsn("1234");
        rdaConfirmRequest.setVerified(true);
        rdaConfirmRequest.setDocumentType("DRIVING_LICENSE");
    }

    @Test
    void checkSecretError(){
        rdaConfirmRequest.setSecret("secret2");

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        assertEquals("ABORTED", appSession.getRdaSessionStatus());
        assertEquals("NOK", result.get("arrivalStatus"));
    }

    @Test
    void checkAccountError(){
        appSession.setAccountId(null);
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        assertEquals("ABORTED", appSession.getRdaSessionStatus());
        assertEquals("NOK", result.get("arrivalStatus"));
    }

    @Test
    void checkErrorError(){
        rdaConfirmRequest.setError("CANCELLED");

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        assertEquals("CANCELLED", appSession.getRdaSessionStatus());
        assertEquals("OK", result.get("arrivalStatus"));
    }

    @Test
    void checkForeignBsnError(){
        appSession.setRdaSessionStatus("SCANNING_FOREIGN");

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(digidClient.checkBsn(any(),any())).thenReturn(Map.of(STATUS, "NOK"));

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        assertEquals("BSN_NOT_MATCHING", appSession.getRdaSessionStatus());
        assertEquals("OK", result.get("arrivalStatus"));
    }

    @Test
    void checkForeignBvBsnError(){
        appSession.setRdaSessionStatus("SCANNING_FOREIGN");

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(digidClient.checkBsn(any(),any())).thenReturn(Map.of(STATUS, "OK"));
        when(dwsClient.checkBvBsn(any(), any())).thenReturn(Map.of(STATUS, "NOK"));

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        verify(digidClient, times(1)).remoteLog("1490",
            Map.of(lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_ID, lowerUnderscore(HIDDEN), true));
        assertEquals("REFUTED", appSession.getRdaSessionStatus());
        assertEquals("OK", result.get("arrivalStatus"));
    }

    @Test
    void checkDigidAppSwitchError(){
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(Boolean.FALSE);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        verify(digidClient, times(1)).remoteLog("853",
            Map.of(lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_ID, lowerUnderscore(HIDDEN), true));
        assertEquals("REFUTED", appSession.getRdaSessionStatus());
        assertEquals("OK", result.get("arrivalStatus"));
    }

    @Test
    void checkDigidRdaSwitchError(){
        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(Boolean.TRUE);
        when(switchService.digidRdaSwitchEnabled()).thenReturn(Boolean.FALSE);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        verify(digidClient, times(1)).remoteLog("579",
            Map.of(lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_ID, lowerUnderscore(HIDDEN), true));
        assertEquals("REFUTED", appSession.getRdaSessionStatus());
        assertEquals("OK", result.get("arrivalStatus"));
    }

    @Test
    void checkVerifiedWidchecker(){
        appSession.setRdaAction("upgrade_rda_widchecker");

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(Boolean.TRUE);
        when(switchService.digidRdaSwitchEnabled()).thenReturn(Boolean.TRUE);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        verify(digidClient, times(1)).remoteLog("1321",
            Map.of("document_type", "driving_license", lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_ID));
        assertEquals("VERIFIED", appSession.getRdaSessionStatus());
        assertEquals("rda", appAuthenticator.getIssuerType());
        assertEquals("OK", result.get("arrivalStatus"));

        verify(idCheckDocumentRepository, times(1)).save(any(IdCheckDocument.class));
    }

    @Test
    void checkVerifiedAppUpgradeFlow(){
        appSession.setRdaAction("app");
        appAuthenticator.setInstanceId("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        appAuthenticator.setDeviceName("testDevice");
        appAuthenticator.setAccountId(T_ACCOUNT_ID);
        appSession.setFlow(UndefinedFlow.NAME);

        when(appSessionService.getSession(any())).thenReturn(appSession);
        when(appAuthenticatorService.findByUserAppId(any())).thenReturn(appAuthenticator);
        when(switchService.digidAppSwitchEnabled()).thenReturn(Boolean.TRUE);
        when(switchService.digidRdaSwitchEnabled()).thenReturn(Boolean.TRUE);

        Map<String, String> result = rdaService.confirm(rdaConfirmRequest);

        verify(digidClient, times(1)).remoteLog("848",
            Map.of("document_type", "driving_license",
                lowerUnderscore(ACCOUNT_ID), T_ACCOUNT_ID,
                lowerUnderscore(APP_CODE), "EC0DC",
                lowerUnderscore(DEVICE_NAME), "testDevice"));
        assertEquals("VERIFIED", appSession.getRdaSessionStatus());
        assertEquals("rda", appAuthenticator.getIssuerType());
        assertEquals("OK", result.get("arrivalStatus"));

        verify(idCheckDocumentRepository, times(1)).save(any(IdCheckDocument.class));
    }
}
