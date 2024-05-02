
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

package nl.logius.digid.dgl.model;

import nl.logius.digid.digilevering.api.model.*;

import java.util.List;

public interface MessageUtil {

    static List<Container> getCategorie(AfnemersberichtAanDGL afnemersberichtAanDGL) {
        switch(afnemersberichtAanDGL.getStuurgegevens().getBerichtsoort().getNaam()) {
            case "Ap01":
                return afnemersberichtAanDGL.getInhoud().getAp01().getCategorie();
            case "Av01":
                return afnemersberichtAanDGL.getInhoud().getAv01().getCategorie();
        }
        return List.of();
    }

    static List<Container> getCategorie(VerstrekkingAanAfnemer verstrekkingAanAfnemer) {
        VerstrekkingInhoudType inhoud = verstrekkingAanAfnemer.getGebeurtenisinhoud();
        switch(verstrekkingAanAfnemer.getGebeurtenissoort().getNaam()) {
            case "Ag01":
                return inhoud.getAg01().getCategorie();
            case "Af01":
                return inhoud.getAf01().getCategorie();
            case "Af11":
                return inhoud.getAf11().getCategorie();
            case "Ng01":
                return inhoud.getNg01().getCategorie();
            case "Gv01":
                return inhoud.getGv01().getCategorie();
            case "Ag31":
                return inhoud.getAg31().getCategorie();
            case "Ag11":
                return inhoud.getAg11().getCategorie();
            case "Ag21":
                return inhoud.getAg21().getCategorie();
            case "Gv02":
                return inhoud.getGv02().getCategorie();
            case "Wa11":
                return inhoud.getWa11().getCategorie();
            // These dont have a Catogerie element;
            case "Pf01":
            case "Pf02":
            case "Pf03":
            case "Vb01":
            case "Null":
                return List.of();
        }
        return List.of();
    }

    static String getBerichttype(AfnemersberichtAanDGL afnemersberichtAanDGL) {
        return afnemersberichtAanDGL.getStuurgegevens().getBerichtsoort().getNaam();
    }

    static String getBerichttype(VerstrekkingAanAfnemer verstrekkingAanAfnemer) {
        return verstrekkingAanAfnemer.getGebeurtenissoort().getNaam();
    }
}
