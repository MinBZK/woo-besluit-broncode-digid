
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

package nl.logius.digid.app.domain.iapi.eid;

import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.logius.digid.app.client.*;
import nl.logius.digid.app.domain.session.AppSession;
import nl.logius.digid.app.domain.session.AppSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import static nl.logius.digid.app.shared.Constants.*;

@Service
public class EidService {

    private final AppSessionService appSessionService;
    private final MscClient mscClient;
    private final HsmBsnkClient hsmClient;
    private final DigidClient digidClient;

    private final String kernOin;
    private final String kernKsv;
    private final String mscOin;
    private final String mscKsv;
    private final String eidasOin;
    private final String eidasKsv;
    private final String brpOin;
    private final String brpKsv;

    @Autowired
    public EidService(AppSessionService appSessionService, MscClient mscClient, HsmBsnkClient hsmClient, DigidClient digidClient,
                       @Value("${kern_oin}") String kernOin, @Value("${kern_ksv}") String kernKsv, @Value("${msc_oin}") String mscOin, @Value("${msc_ksv}") String mscKsv, @Value("${eidas_oin}") String eidasOin, @Value("${eidas_ksv}") String eidasKsv, @Value("${brp_oin}") String brpOin, @Value("${brp_ksv}") String brpKsv) {
        this.appSessionService = appSessionService;
        this.mscClient = mscClient;
        this.hsmClient = hsmClient;
        this.digidClient = digidClient;
        this.kernOin = kernOin;
        this.kernKsv = kernKsv;
        this.mscOin = mscOin;
        this.mscKsv = mscKsv;
        this.eidasOin = eidasOin;
        this.eidasKsv = eidasKsv;
        this.brpOin = brpOin;
        this.brpKsv = brpKsv;
    }

    public Map<String, String> confirm(EidConfirmRequest request) {
        AppSession session = appSessionService.getSession(request.getId());

        if (request.getReason() != null) {
            var error = List.of("eid_timeout", "desktop_clients_ip_address_not_equal").contains(request.getReason()) ? request.getReason() : "unknown";
            session.setError(error);
            session.setState("ABORTED");

            appSessionService.save(session);
            return Map.of("arrivalStatus", "OK");
        }

        Map<String, Map<String, Object>> input = new HashMap<>();
        input.put(kernOin, Map.of(KSV, kernKsv, IDENTITY, true, PSEUDONYM, false));
        input.put(mscOin, Map.of(KSV, mscKsv));

        addExtraPolymorphInputs(session, input);

        var transforms = hsmClient.transformMultiple(request.getPip(), input);

        addExtraPolymorpDataIfPresent(session, transforms);
        setAccountData(session, transforms.get(kernOin).get(IDENTITY).asText());
        setDocumentData(session, transforms.get(mscOin).get(PSEUDONYM).asText(), request);

        session.setState("VERIFIED");

        appSessionService.save(session);
        return Map.of("arrivalStatus", "OK");
    }


    private void setAccountData(AppSession session, String ei) {
        var response = digidClient.decryptEi(ei);
        if (response.get("account_id") != null) {
            session.setAccountId(Long.parseLong(response.get("account_id")));
        }
    }

    private void setDocumentData(AppSession session, String epsc, EidConfirmRequest request) {
        var cardStatus = mscClient.fetchStatus(epsc, request.getSequenceNumber(), request.getDocumentType());
        session.setDocumentType(request.getDocumentType());
        session.setSequenceNo(request.getSequenceNumber());
        session.setCardStatus(cardStatus);
    }

    private void addExtraPolymorpDataIfPresent(AppSession session, ObjectNode transforms){
        if (session.getEidasUit()) {
            session.setPolymorphPseudonym(transforms.get(eidasOin).get(PSEUDONYM).asText());
            session.setPolymorphIdentity(transforms.get(brpOin).get(IDENTITY).asText());
        }
    }

    private void addExtraPolymorphInputs(AppSession session, Map<String, Map<String, Object>> input) {
        if (session.getEidasUit()) {
            input.put(eidasOin, Map.of(KSV, eidasKsv, IDENTITY, false, PSEUDONYM, true, INCLUDE_LINKS, true));
            input.put(brpOin, Map.of(KSV, brpKsv, IDENTITY, true, PSEUDONYM, false, INCLUDE_LINKS, false));
        }
    }
}
