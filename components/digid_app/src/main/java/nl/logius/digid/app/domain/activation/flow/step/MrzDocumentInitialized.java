
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

package nl.logius.digid.app.domain.activation.flow.step;

import nl.logius.digid.app.client.DigidClient;
import nl.logius.digid.app.client.RdaClient;
import nl.logius.digid.app.domain.activation.request.MrzDocumentRequest;
import nl.logius.digid.app.domain.activation.response.RdaResponse;
import nl.logius.digid.app.domain.flow.AbstractFlowStep;
import nl.logius.digid.app.domain.flow.Flow;
import nl.logius.digid.app.shared.response.AppResponse;
import nl.logius.digid.app.shared.response.NokResponse;

import java.util.List;
import java.util.Map;

import static nl.logius.digid.app.shared.Constants.*;

public class MrzDocumentInitialized extends AbstractFlowStep<MrzDocumentRequest> {

    private final DigidClient digidClient;
    private final RdaClient rdaClient;
    private final String returnUrl;

    public MrzDocumentInitialized(DigidClient digidClient, RdaClient rdaClient, String returnUrl) {
        super();
        this.digidClient = digidClient;
        this.rdaClient = rdaClient;
        this.returnUrl = returnUrl;
    }

    @Override
    public AppResponse process(Flow flow, MrzDocumentRequest params) {
        if(!(params.getDocumentType().equals("PASSPORT") || params.getDocumentType().equals("ID_CARD"))){
            return new NokResponse();
        }
        Map<String, String> travelDocument = Map.of(
            "documentNumber", params.getDocumentNumber(),
            "dateOfBirth", params.getDateOfBirth(),
            "dateOfExpiry", params.getDateOfExpiry());

        digidClient.remoteLog("867", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), HIDDEN, true));
        appSession.setRdaSessionStatus("DOCUMENTS_RECEIVED");

        Map<String, String> rdaSession = rdaClient.startSession(
            returnUrl.concat("/iapi/rda/confirm"),
            appSession.getId(), params.getIpAddress(), List.of(travelDocument), List.of());

        if(rdaSession.isEmpty()){
            digidClient.remoteLog("873", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), HIDDEN, true));
            return new NokResponse();
        }
        appSession.setConfirmSecret(rdaSession.get("confirmSecret"));
        appSession.setUrl(rdaSession.get("url"));
        appSession.setRdaSessionId(rdaSession.get("sessionId"));
        appSession.setRdaSessionTimeoutInSeconds(rdaSession.get("expiration"));
        appSession.setRdaSessionStatus("SCANNING_FOREIGN");
        appSession.setRdaDocumentType(params.getDocumentType());
        appSession.setRdaDocumentNumber(params.getDocumentNumber());
        digidClient.remoteLog("868", Map.of(lowerUnderscore(ACCOUNT_ID), appSession.getAccountId(), HIDDEN, true));

        return new RdaResponse(appSession.getUrl(), appSession.getRdaSessionId());
    }
}
