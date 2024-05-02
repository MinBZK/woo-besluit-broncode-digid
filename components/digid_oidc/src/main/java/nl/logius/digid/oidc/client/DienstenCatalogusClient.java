
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

package nl.logius.digid.oidc.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import nl.logius.digid.oidc.exception.DienstencatalogusException;
import nl.logius.digid.oidc.model.DcMetadataResponse;
import nl.logius.digid.sharedlib.client.IapiClient;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class DienstenCatalogusClient extends IapiClient {

    private static Logger logger = LoggerFactory.getLogger(DienstenCatalogusClient.class);
    private final ObjectReader reader;

    public DienstenCatalogusClient(HttpUrl url, String authToken, int timeout) {
        super(url, authToken, timeout);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        reader = objectMapper.readerFor(DcMetadataResponse.class);
    }

    public DcMetadataResponse retrieveMetadataFromDc(String clientId) throws DienstencatalogusException {
        try {
            DcMetadataResponse response = reader.readValue(execute("dc/oidc/metadata", Map.of("client_id", clientId)));

            if ("STATUS_OK".equals(response.getRequestStatus())) {
                logger.info("OIDC metadata found");
                return response;
            } else {
                logger.error("Unknown status from digid_dc");
                return response;
            }
        } catch (IOException e) {
            logger.error("IOException while parsing response from digid_dc: {}", e.getMessage());
        } catch (ClientException e) {
            logger.error("Connection error while trying to connect to digid_dc: {}", e.getMessage());
            throw new DienstencatalogusException("Connection error while trying to connect to digid_dc");
        }

        return null;
    }
}
