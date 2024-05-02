
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

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.google.common.collect.ImmutableMap;

import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nl.logius.digid.eid.clients.ConfirmClient;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.rest.digid.Confirmation;

@Service
public class ConfirmService {
    @Value("${confirm.concurrency}")
    private int concurrency;

    @Value("${confirm.keepAlive}")
    private int keepAlive;

    @Value("${confirm.timeout}")
    private int timeout;

    @Value("${iapi.token}")
    private String iapiToken;

    private ThreadPoolExecutor executorService;

    @PostConstruct
    public void init() {
        executorService  = new ThreadPoolExecutor(concurrency, concurrency, keepAlive, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());
        executorService.allowCoreThreadTimeOut(true);
    }

    public void sendAssertion(String returnUrl, String id, String secret, PolymorphType type, Confirmation confirm) {
        final Map<String, Object> body = new ImmutableMap.Builder<String, Object>()
            .put("id", id).put("secret", secret).put(type.name().toLowerCase(), confirm.polymorph)
            .put("sequenceNumber", confirm.sequenceNo).put("documentType", confirm.documentType.value)
            .build();
        executorService.submit(() -> getClient(returnUrl).sendAssertion(body));
    }

    public void sendError(String returnUrl, String id, String secret, String reason) {
        final Map<String, Object> body = ImmutableMap.of("id", id, "secret", secret, "reason", reason);
        executorService.submit(() -> getClient(returnUrl).sendError(body));
    }

    private ConfirmClient getClient(String returnUrl) {
        return new ConfirmClient(HttpUrl.get(returnUrl), iapiToken, timeout);
    }
}
