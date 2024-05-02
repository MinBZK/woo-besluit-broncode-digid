
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

package nl.logius.digid.dgl.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.model.MessageUtil;
import nl.logius.digid.digilevering.api.model.AfnemersberichtAanDGL;
import nl.logius.digid.digilevering.api.model.VerstrekkingAanAfnemer;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DigidXClient extends IapiClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DigidXClient(HttpUrl baseUrl, String authToken, int timeout) {
        super(baseUrl, authToken, timeout);
    }

    public void setANummer(String bsn, String aNumber) {
        logger.info("To X: bsn: " + bsn + " , A-nummer: " + aNumber);
        final Map<String, Object> content = new HashMap<>();
        content.put("bsn", bsn);
        content.put("a_number", aNumber);

        try {
            execute("set_a_nummer", content);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public void updateANummer(String oldANumber, String newANumber) {
        logger.info("To X: old a-nummer: " + oldANumber + " , new a-nummer: " + newANumber);

        final Map<String, Object> content = new HashMap<>();
        content.put("old_a_number", oldANumber);
        content.put("new_a_number", newANumber);

        try {
            execute("update_a_nummer", content);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public void setOpschortingsStatus(String aNumber, String status) {
        logger.info("To X: A-nummer: " + aNumber + " , Status: " + status);

        final Map<String, Object> content = new HashMap<>();
        content.put("a_number", aNumber);
        content.put("status", status);

        try {
            execute("set_opschortings_status", content);
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }

    public void remoteLogBericht(String key, AfnemersberichtAanDGL request) {
        Map<String, String> payload = new HashMap<>();
        String messageType = MessageUtil.getBerichttype(request);
        payload.put("message_type", messageType);

        String aNumber = CategorieUtil.findANummer(MessageUtil.getCategorie(request));
        String bsn = CategorieUtil.findBsn(MessageUtil.getCategorie(request));
        logWithContent(key, payload, aNumber, bsn);
    }

    public void remoteLogBericht(String key, VerstrekkingAanAfnemer request, Afnemersbericht afnemersbericht) {
        Map<String, String> payload = new HashMap<>();
        String messageType = MessageUtil.getBerichttype(request);
        payload.put("message_type", messageType);

        String aNumber = CategorieUtil.findANummer(MessageUtil.getCategorie(request));
        if (aNumber == null && afnemersbericht != null) {
            aNumber = afnemersbericht.getANummer();
        }

        String bsn = CategorieUtil.findBsn(MessageUtil.getCategorie(request));
        if (bsn == null && afnemersbericht != null) {
            bsn = afnemersbericht.getBsn();
        }

        logWithContent(key, payload, aNumber, bsn);
    }

    private void logWithContent(String key, Map<String, String> payload, String aNumber, String bsn) {
        final Map<String, Object> content = new HashMap<>();
        if (aNumber != null) {
            content.put("a_number", aNumber);
        }

        if (bsn != null) {
            content.put("bsn", bsn);
        }

        remoteLog(key, payload, content);
    }

    public void remoteLogSpontaneVerstrekking(String key, String messageType, String aNumber, String bsn) {
        Map<String, String> payload = new HashMap<>();
        payload.put("message_type", messageType);

        logWithContent(key, payload, aNumber, bsn);
    }

    public void remoteLogWithoutRelatingToAccount(String key, String messageType) {
        Map<String, String> payload = new HashMap<>();
        payload.put("message_type", messageType);
        remoteLog(key, payload, new HashMap<>());
    }

    public void remoteLog(String key, Map payload, Map content) {
        content.put("key", key);

        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put("hidden", "true");
        content.put("payload", payload);

        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>().put("iapi", content).build();

        try {
            JsonNode response = execute("log", content);
            if (response.get("error") != null && "DIGID_ACCOUNT_NOT_FOUND".equals(response.get("error").textValue())) {
                Map<String, String> payloadAccountNotFound = new HashMap<>();
                payloadAccountNotFound.put("message_type", payload.get("message_type").toString());
                remoteLog(Log.DIGID_ACCOUNT_NOT_FOUND, payloadAccountNotFound, new HashMap<>());
                remoteLog(Log.MESSAGE_PROCESSED, payloadAccountNotFound, new HashMap<>());
            }
        } catch (ClientException e) {
            logger.error(e.getMessage());
        }
    }
}
