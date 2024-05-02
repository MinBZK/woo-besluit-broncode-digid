
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

package nl.logius.digid.dws.utils;

import java.io.IOException;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.springframework.core.io.Resource;

import https.digid_nl.schema.mu_pin_reset.RegisterPinResetRequest;
import nl.logius.digid.dws.util.XmlUtils;
import nl.logius.digid.dws.util.XmlUtilsException;

public class TestHelper {

    public static RegisterPinResetRequest generateSoapRequest(Resource request) throws XmlUtilsException, IOException {
        return XmlUtils.readSoapMessageFromStreamAndUnmarshallBody2Object(request.getInputStream(), RegisterPinResetRequest.class);
    }

    public static <T> T sendResourceSoapRequest(final Resource request, Class<T> responseClass, final int port, final String service) throws XmlUtilsException, IOException {
        //TODO: Use a mock
        Response httpResponseContainer = Request
                .Post("http://localhost:"+ port +"/dws/"+ service)
                .bodyStream(request.getInputStream(), ContentType.create(ContentType.TEXT_XML.getMimeType(), Consts.UTF_8))
                .execute();

        HttpResponse httpResponse = httpResponseContainer.returnResponse();
        return XmlUtils.readSoapMessageFromStreamAndUnmarshallBody2Object(httpResponse.getEntity().getContent(), responseClass);
    }

}
