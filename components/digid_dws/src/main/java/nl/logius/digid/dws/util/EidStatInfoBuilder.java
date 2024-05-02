
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

package nl.logius.digid.dws.util;

import nl.logius.digid.dws.exception.SoapValidationException;
import nl.rdw.eid_wus_crb._1.EIDDOCTYPE;
import nl.rdw.eid_wus_crb._1.EIDSTATAGEG;
import nl.rdw.eid_wus_crb._1.EIDSTATINFO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class EidStatInfoBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EidStatInfoBuilder.class);

    private EidStatInfoBuilder() {

    }

    public static EIDSTATINFO eidstatinfoBuilder(String bsn, String sequenceNo) throws SoapValidationException {
        final EIDSTATINFO eidstatinfo = new EIDSTATINFO();
        final EIDSTATAGEG gegevens = new EIDSTATAGEG();
        try {
            gegevens.setBURGSERVNRA(new BigInteger(bsn));
            gegevens.setEIDVOLGNRA(new BigInteger(sequenceNo));
            gegevens.setEIDDOCTYPE(EIDDOCTYPE.RIJBEWIJS);
            eidstatinfo.setEIDSTATAGEG(gegevens);
        } catch (NumberFormatException e) {
            final String errorMessage = "Cannot convert bsn/sequenceNo to a BigInteger";
            logger.error(errorMessage);
            throw new SoapValidationException(errorMessage, e);
        }
        return eidstatinfo;
    }

}
