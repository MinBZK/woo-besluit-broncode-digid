
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

import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.digilevering.api.model.*;
import nl.logius.digid.dgl.controller.AfnemersberichtAanDGLFactory;
import nl.logius.digid.dgl.model.Afnemersbericht;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class AfnemersindicatieService {

    private final AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory;
    private final DglMessageFactory dglMessageFactory;
    private final AfnemersberichtRepository afnemersberichtRepository;
    private final DglSendService dglSendService;

    public static String RESENT_TASK_NAME = "resend_unsent_afnemerindicaties";

    public AfnemersindicatieService(AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory, DglMessageFactory dglMessageFactory, AfnemersberichtRepository afnemersberichtRepository, DglSendService dglSendService) {
        this.afnemersberichtAanDGLFactory = afnemersberichtAanDGLFactory;
        this.dglMessageFactory = dglMessageFactory;
        this.afnemersberichtRepository = afnemersberichtRepository;
        this.dglSendService = dglSendService;
    }

    public void createAfnemersindicatie(String bsn) {
        AfnemersberichtAanDGL afnemersberichtAanDGL = afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(dglMessageFactory.createAp01(bsn));

        Afnemersbericht afnemersbericht = new Afnemersbericht();
        afnemersbericht.setBsn(bsn);
        afnemersbericht.setType(Afnemersbericht.Type.Ap01);
        afnemersbericht.setOnzeReferentie(afnemersberichtAanDGL.getBerichtHeader().getKenmerkVerstrekker());
        afnemersbericht.setStatus(Afnemersbericht.Status.INITIAL);
        afnemersberichtRepository.save(afnemersbericht);

        dglSendService.sendAfnemersBerichtAanDGL(afnemersberichtAanDGL, afnemersbericht);
    }


    public void deleteAfnemersindicatie(String aNummer) {
        AfnemersberichtAanDGL afnemersberichtAanDGL = afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(dglMessageFactory.createAv01(aNummer));

        Afnemersbericht afnemersbericht = new Afnemersbericht();
        afnemersbericht.setANummer(aNummer);
        afnemersbericht.setType(Afnemersbericht.Type.Av01);
        afnemersbericht.setOnzeReferentie(afnemersberichtAanDGL.getBerichtHeader().getKenmerkVerstrekker());
        afnemersbericht.setStatus(Afnemersbericht.Status.INITIAL);
        afnemersberichtRepository.save(afnemersbericht);

        dglSendService.sendAfnemersBerichtAanDGL(afnemersberichtAanDGL, afnemersbericht);
    }

    @Async
    public void resendUnsentMessages() {
         List<Afnemersbericht> failedSendBerichten = afnemersberichtRepository.findByStatus(Afnemersbericht.Status.SEND_FAILED);

        for (Afnemersbericht afnemersbericht: failedSendBerichten){
            if (afnemersbericht.getType() == Afnemersbericht.Type.Ap01) {
                createAfnemersindicatie(afnemersbericht.getBsn());
            } else if (afnemersbericht.getType() == Afnemersbericht.Type.Av01) {
                deleteAfnemersindicatie(afnemersbericht.getANummer());
            }
            afnemersberichtRepository.delete(afnemersbericht);
        }
    }
}
