
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

package nl.logius.digid.ns.model;

import com.fasterxml.jackson.databind.JsonNode;
import nl.logius.digid.ns.client.MnsStatus;
import okhttp3.Response;

public class MnsResponse {
    private MnsStatus mnsStatus;
    private Response response;
    private JsonNode nodes;

    public MnsResponse(MnsStatus mnsStatus, Response response, JsonNode nodes) {
        this.mnsStatus = mnsStatus;
        this.response = response;
        this.nodes = nodes;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public MnsStatus getMnsStatus() {
        return mnsStatus;
    }

    public void setMnsStatus(MnsStatus mnsStatus) {
        this.mnsStatus = mnsStatus;
    }

    public JsonNode getNodes() {
        return nodes;
    }

    public void setNodes(JsonNode nodes) {
        this.nodes = nodes;
    }
}
