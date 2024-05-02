
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

package nl.logius.digid.dgl.service;


import nl.logius.digid.dgl.client.DigidXClient;
import nl.logius.digid.dgl.client.DigileveringClient;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DglSendService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AfnemersberichtRepository afnemersberichtRepository;

    @Autowired
    private DigileveringClient digileveringClient;

    @Async
    public void sendAfnemersBerichtAanDGL(AfnemersberichtAanDGL afnemersberichtAanDGL, Afnemersbericht afnemersbericht){
        boolean success = digileveringClient.sendRequest(afnemersberichtAanDGL);
        afnemersbericht.setStatus(success ? Afnemersbericht.Status.PENDING : Afnemersbericht.Status.SEND_FAILED);
        afnemersberichtRepository.save(afnemersbericht);
    }
}
