
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
import nl.logius.digid.dgl.client.Log;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DglResponseService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DigidXClient digidXClient;

    @Autowired
    private AfnemersberichtRepository afnemersberichtRepository;

    public void processNullMessage(Null nullMessage, Afnemersbericht afnemersbericht){
        if (afnemersbericht != null && afnemersbericht.getType() == Afnemersbericht.Type.Av01){
            afnemersberichtRepository.delete(afnemersbericht);
        }

        logger.info("Received null message");
    }

    public void processAg01(Ag01 ag01, Afnemersbericht afnemersbericht){
        String aNummer = CategorieUtil.findANummer(ag01.getCategorie());
        String bsn = afnemersbericht.getBsn();

        digidXClient.setANummer(bsn, aNummer);
        if(ag01.getStatus() != null && ag01.getDatum() != null){
            digidXClient.setOpschortingsStatus(aNummer, ag01.getStatus());
        }
        afnemersberichtRepository.delete(afnemersbericht);
        logger.info("Finished processing Ag01 message");
    }

    public void processAg31(Ag31 ag31, Afnemersbericht afnemersbericht){
        String aNummer = CategorieUtil.findANummer(ag31.getCategorie());
        String bsn = CategorieUtil.findBsn(ag31.getCategorie());

        digidXClient.setANummer(bsn, aNummer);
        if(ag31.getStatus() != null  && ag31.getDatum() != null){
            digidXClient.setOpschortingsStatus(aNummer, ag31.getStatus());
        }

        // Only possible if original message (Ag01) was not sent
        if(afnemersbericht != null) {
            afnemersberichtRepository.delete(afnemersbericht);
        }

        logger.info("Finished processing Ag31 message");
    }

    public void processAf01(Af01 af01, Afnemersbericht afnemersbericht){

        afnemersberichtRepository.delete(afnemersbericht);
        logger.info("Finished processing Af01 message");
    }

    public void processAf11(Af11 af11, Afnemersbericht afnemersbericht){
        afnemersbericht.setStatus(Afnemersbericht.Status.FAILED);
        afnemersbericht.setErrorCode(af11.getFoutreden());
        afnemersberichtRepository.save(afnemersbericht);
        logger.info("Finished processing Af11 message");
    }

    public void processGv01(Gv01 gv01){
        String oldBsn = CategorieUtil.findBsnOudeWaarde(gv01.getCategorie());
        if (oldBsn != null && CategorieUtil.findBsn(gv01.getCategorie()) != null){
            digidXClient.remoteLogSpontaneVerstrekking(Log.BSN_CHANGED, "Gv01", gv01.getANummer(), oldBsn);
        }
        String aNummer = gv01.getANummer();
        String redenOpschorting = CategorieUtil.findRedenOpschorting(gv01.getCategorie());
        digidXClient.setOpschortingsStatus(aNummer, redenOpschorting);

        logger.info("Finished processing Gv01 message");
    }

    public void processNg01(Ng01 ng01){
        String aNummer = CategorieUtil.findANummer(ng01.getCategorie());
        String redenOpschorting = CategorieUtil.findRedenOpschorting(ng01.getCategorie());
        digidXClient.setOpschortingsStatus(aNummer, redenOpschorting);

        logger.info("Finished processing Ng01 message");
    }

    public void processWa11(Wa11 wa11){
        String oldANummer = CategorieUtil.findANummer(wa11.getCategorie());
        String newANummer = wa11.getANummer();

        digidXClient.updateANummer(oldANummer, newANummer);

        logger.info("Finished processing Wa11 message");
    }
}
