
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
import nl.logius.digid.dgl.model.MessageUtil;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.List;

@Component
public class VerstrekkingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AfnemersberichtRepository afnemersberichtRepository;

    @Autowired
    private DglResponseService dglResponseService;

    @Autowired
    private DigidXClient digidXClient;

    public void processVerstrekkingAanAfnemer(VerstrekkingAanAfnemer verstrekkingAanAfnemer){
        if (logger.isDebugEnabled())
            logger.debug("Processing verstrekkingAanAfnemer: {}", marshallElement(verstrekkingAanAfnemer));

        Afnemersbericht afnemersbericht = afnemersberichtRepository.findByOnzeReferentie(verstrekkingAanAfnemer.getReferentieId());
        if(mismatch(verstrekkingAanAfnemer, afnemersbericht)){
            digidXClient.remoteLogBericht(Log.NO_RELATION_TO_SENT_MESSAGE, verstrekkingAanAfnemer, afnemersbericht);
            return;
        }

        switch (verstrekkingAanAfnemer.getGebeurtenissoort().getNaam()) {
            case "Null" -> {
                logger.info("Start processing Null message");
                dglResponseService.processNullMessage(verstrekkingAanAfnemer.getGebeurtenisinhoud().getNull(), afnemersbericht);
                digidXClient.remoteLogWithoutRelatingToAccount(Log.MESSAGE_PROCESSED, "Null");
            }
            case "Ag01" -> {
                logger.info("Start processing Ag01 message");
                dglResponseService.processAg01(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAg01(), afnemersbericht);
                digidXClient.remoteLogBericht(Log.MESSAGE_PROCESSED, verstrekkingAanAfnemer, afnemersbericht);
            }
            case "Ag31" -> {
                logger.info("Start processing Ag31 message");
                dglResponseService.processAg31(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAg31(), afnemersbericht);
                digidXClient.remoteLogBericht(Log.MESSAGE_PROCESSED, verstrekkingAanAfnemer, afnemersbericht);
            }
            case "Af01" -> {
                logger.info("Start processing Af01 message");
                dglResponseService.processAf01(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAf01(), afnemersbericht);
                digidXClient.remoteLogBericht(Log.MESSAGE_PROCESSED, verstrekkingAanAfnemer, afnemersbericht);
            }
            case "Af11" -> {
                logger.info("Start processing Af11 message");
                dglResponseService.processAf11(verstrekkingAanAfnemer.getGebeurtenisinhoud().getAf11(), afnemersbericht);
                digidXClient.remoteLogWithoutRelatingToAccount(Log.MESSAGE_PROCESSED, "Af11");
            }
            case "Gv01" -> {
                logger.info("Start processing Gv01 message");
                Gv01 gv01 = verstrekkingAanAfnemer.getGebeurtenisinhoud().getGv01();
                dglResponseService.processGv01(gv01);
                String bsn = CategorieUtil.findBsnOudeWaarde(gv01.getCategorie());
                if (bsn == null) {
                    bsn = CategorieUtil.findBsn(gv01.getCategorie());
                }
                digidXClient.remoteLogSpontaneVerstrekking(Log.MESSAGE_PROCESSED, "Gv01", gv01.getANummer(), bsn);
            }
            case "Ng01" -> {
                logger.info("Start processing Ng01 message");
                Ng01 ng01 = verstrekkingAanAfnemer.getGebeurtenisinhoud().getNg01();
                dglResponseService.processNg01(ng01);
                digidXClient.remoteLogSpontaneVerstrekking(Log.MESSAGE_PROCESSED, "Ng01", CategorieUtil.findANummer(ng01.getCategorie()), "");
            }
            case "Wa11" -> {
                logger.info("Start processing Wa11 message");
                dglResponseService.processWa11(verstrekkingAanAfnemer.getGebeurtenisinhoud().getWa11());
            }
        }
    }

    private boolean mismatch(VerstrekkingAanAfnemer verstrekkingAanAfnemer, Afnemersbericht afnemersbericht) {
        List<String> messageTypesWithRelatedMessage = List.of("Null", "Ag01", "Af01", "Af11");
        String messageType = verstrekkingAanAfnemer.getGebeurtenissoort().getNaam();
        if(messageTypesWithRelatedMessage.contains(messageType)) {
            if(afnemersbericht == null) {
                return true;
            }
            if ("Null".equals(messageType)) {
                return false;
            } else if ("Ag01".equals(messageType) || "Af01".equals(messageType)) {
                return !afnemersbericht.getBsn().equals(CategorieUtil.findBsn(MessageUtil.getCategorie(verstrekkingAanAfnemer)));
            } else if ("Af11".equals(messageType)) {
                return !afnemersbericht.getANummer().equals(CategorieUtil.findANummer(MessageUtil.getCategorie(verstrekkingAanAfnemer)));
            }
        }
        return false;
    }

    private String marshallElement(VerstrekkingAanAfnemer verstrekkingAanAfnemer) {
        try {
            JAXBContext context = JAXBContext.newInstance(VerstrekkingAanAfnemer.class, ObjectFactory.class);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.formatted.output", Boolean.TRUE);
            StringWriter sw = new StringWriter();
            m.marshal(verstrekkingAanAfnemer, sw);
            return sw.toString();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return null;
    }
}
