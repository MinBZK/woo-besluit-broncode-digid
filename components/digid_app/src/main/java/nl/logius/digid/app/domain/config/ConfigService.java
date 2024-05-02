
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

package nl.logius.digid.app.domain.config;

import nl.logius.digid.app.client.SharedServiceClient;
import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.config.response.ConfigResponse;
import nl.logius.digid.app.domain.config.response.WebServerResponse;
import nl.logius.digid.app.domain.switches.SwitchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigService {

    private final String protocol;
    private final String host;

    private SwitchService switchService;
    private SharedServiceClient sharedServiceClient;

    @Autowired
    public ConfigService(@Value("${protocol}") String protocol, @Value("${hosts.mijn}") String host, SharedServiceClient sharedServiceClient,SwitchService switchService )   {
        this.protocol = protocol;
        this.host = host;
        this.switchService = switchService;
        this.sharedServiceClient = sharedServiceClient;
    }

    public WebServerResponse getWebserverUrls() {
        return new WebServerResponse("OK", List.of(new WebService("Mijn DigiD", protocol + "://" + host + "/authn_app")));
    }

    public ConfigResponse getConfig() throws SharedServiceClientException {
        ConfigResponse configResponse = new ConfigResponse();
        configResponse.setDigidAppSwitchEnabled(switchService.digidAppSwitchEnabled());
        configResponse.setDigidRdaEnabled(switchService.digidRdaSwitchEnabled());
        configResponse.setRequestStationEnabled(switchService.digidRequestStationEnabled());
        configResponse.setEhaEnabled(switchService.digidEhaEnabled());
        configResponse.setLetterRequestDelay(sharedServiceClient.getSSConfigInt("snelheid_aanvragen_digid_app"));
        configResponse.setMaxPinChangePerDay(sharedServiceClient.getSSConfigInt("change_app_pin_maximum_per_day"));

        return configResponse;
    }
}
