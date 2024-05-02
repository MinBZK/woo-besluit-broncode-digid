
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

package nl.logius.digid.scheduler.client;

import nl.logius.digid.scheduler.exception.ApplicationNotFoundException;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TaskClientFactory {

    @Value("${urls.internal.x}")
    private String kernBaseUrl;

    @Value("${urls.internal.admin}")
    private String adminBaseUrl;

    @Value("${urls.internal.balie}")
    private String balieBaseUrl;

    @Value("${urls.internal.ns}")
    private String nsBaseUrl;

    @Value("${urls.internal.app}")
    private String appBaseUrl;

    @Value("${urls.internal.dgl}")
    private String dglBaseUrl;

    @Value("${iapi.timeout}")
    private int timeout;

    @Value("${iapi.token}")
    private String iapiToken;

    public TaskClient getClientForApplication(String applicationName) {
        return switch (applicationName) {
            case "digid_x" -> new TaskClient(HttpUrl.get(kernBaseUrl), iapiToken, timeout);
            case "digid_admin" -> new TaskClient(HttpUrl.get(adminBaseUrl), iapiToken, timeout);
            case "digid_balie" -> new TaskClient(HttpUrl.get(balieBaseUrl), iapiToken, timeout);
            case "digid_ns" -> new TaskClient(HttpUrl.get(nsBaseUrl), iapiToken, timeout);
            case "digid_app" -> new TaskClient(HttpUrl.get(appBaseUrl), iapiToken, timeout);
            case "digid_dgl" -> new TaskClient(HttpUrl.get(dglBaseUrl), iapiToken, timeout);
            default -> throw new ApplicationNotFoundException("Cannot find client for: " + applicationName);
        };
    }
}
