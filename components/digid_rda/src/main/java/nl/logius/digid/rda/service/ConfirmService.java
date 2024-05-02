
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

package nl.logius.digid.rda.service;

import com.google.common.collect.ImmutableMap;
import nl.logius.digid.rda.clients.ConfirmClient;
import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.models.card.App;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmService {
    @Value("${confirm.concurrency}")
    private int concurrency;
    private ThreadPoolExecutor executorService;
    @Value("${iapi.token}")
    private String iapiToken;
    @Value("${confirm.keepAlive}")
    private int keepAlive;
    @Value("${confirm.timeout}")
    private int timeout;

    @PostConstruct
    public void init() {
        executorService = new ThreadPoolExecutor(concurrency, concurrency, keepAlive, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
        executorService.allowCoreThreadTimeOut(true);
    }

    public void sendConfirm(String returnUrl, String id, String secret, boolean verified, App.Session session, RdaError error) {
        final ConfirmClient client = new ConfirmClient(HttpUrl.get(returnUrl), iapiToken, timeout);
        final ImmutableMap.Builder builder = new ImmutableMap.Builder<String, Object>()
            .put("id", id).put("secret", secret).put("verified", verified);

        if (session.getDocumentType() != null) {
            builder.put("documentType", session.getDocumentType());
        }

        if (session.getDocumentNumber() != null) {
            builder.put("documentNumber", session.getDocumentNumber());
        }

        if (error != null) {
            builder.put("error", error.name());
        }

        if (session.getBsn() != null) {
            builder.put("bsn", session.getBsn());
        }

        if (session.getMrzIdentifier() != null) {
            builder.put("mrzIdentifier", session.getMrzIdentifier());
        }


        final Map<String, Object> body = builder.build();
        executorService.submit(() -> client.confirm(body));
    }

    public void sendError(String returnUrl, String id, String secret, String error) {
        final Map<String, Object> body = ImmutableMap.of("id", id, "secret", secret, "error", error);
        executorService.submit(() -> getClient(returnUrl).sendError(body));
    }

    private ConfirmClient getClient(String returnUrl) {
        return new ConfirmClient(HttpUrl.get(returnUrl), iapiToken, timeout);
    }
}
