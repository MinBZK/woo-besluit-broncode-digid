
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

import nl.logius.digid.dgl.api.DigileveringSender;
import nl.logius.digid.dgl.model.MessageUtil;
import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.digilevering.lib.model.Headers;
import nl.logius.digid.digilevering.lib.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DigileveringClient {

    @Value("${digilevering.oin.self}")
    protected String digidOIN;

    @Value("${digilevering.oin.other}")
    protected String digileveringOIN;

    @Autowired
    private DigileveringSender digileveringSender;

    @Autowired
    private DigidXClient digidXClient;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public boolean sendRequest(AfnemersberichtAanDGL request) {
        Map<String, Object> extraHeaders = new HashMap<>();
        extraHeaders.put(Headers.X_AUX_SENDER_ID, digidOIN);
        extraHeaders.put(Headers.X_AUX_RECEIVER_ID, digileveringOIN);

        try {
            digileveringSender.sendMessage(request, HeaderUtil.createAfnemersberichtAanDGLHeaders(extraHeaders));
            if( MessageUtil.getBerichttype(request).equals("Av01")){
                digidXClient.remoteLogWithoutRelatingToAccount(Log.SEND_SUCCESS, "Av01");
            } else {
                digidXClient.remoteLogBericht(Log.SEND_SUCCESS, request);
            }
            return true;
        } catch (JmsException jmsException) {
            logger.error(jmsException.getMessage());
            digidXClient.remoteLogBericht(Log.SEND_FAILURE, request);
            return false;
        }
    }

}
