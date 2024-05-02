
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

import nl.ictu.bsn.AfzenderDE;
import nl.ictu.bsn.ArrayOfVerificatieIdenDocumentenVraagDE;
import nl.ictu.bsn.VerificatieIdenDocumentenBI;
import nl.ictu.bsn.VerificatieIdenDocumentenVraagDE;
import nl.logius.digid.dws.exception.SoapValidationException;
import nl.logius.digid.dws.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerificatieIdenDocumentenBIBuilder {

    private VerificatieIdenDocumentenBIBuilder() {
    }

    private static final Logger logger = LoggerFactory.getLogger(VerificatieIdenDocumentenBIBuilder.class);

    public static VerificatieIdenDocumentenBI verificatieIdenDocumentenBIBuilder(DocumentType documentType, String documentNumber, String sender, String indicationEndUser) throws SoapValidationException {
        final VerificatieIdenDocumentenBI verificatieIdenDocumentenBI = new VerificatieIdenDocumentenBI();

        AfzenderDE afzenderDE = new AfzenderDE();
        afzenderDE.setAfzender(sender);
        afzenderDE.setBerichtNr("1");
        verificatieIdenDocumentenBI.setAfzender(afzenderDE);

        verificatieIdenDocumentenBI.setIndicatieEindgebruiker(indicationEndUser);

        ArrayOfVerificatieIdenDocumentenVraagDE arrayOfVerificatieIdenDocumentenVraagDE = new ArrayOfVerificatieIdenDocumentenVraagDE();
        VerificatieIdenDocumentenVraagDE verificatieIdenDocumentenVraagDE = new VerificatieIdenDocumentenVraagDE ();
        verificatieIdenDocumentenVraagDE.setDocumentNummer(documentNumber);
        verificatieIdenDocumentenVraagDE.setDocumentType(String.valueOf(documentType.getValue()));
        verificatieIdenDocumentenVraagDE.setVraagnummer(1);
        arrayOfVerificatieIdenDocumentenVraagDE.getVerificatieIdenDocumentenVraagDE().add(verificatieIdenDocumentenVraagDE);

        verificatieIdenDocumentenBI.setIdenDocumentenVraag(arrayOfVerificatieIdenDocumentenVraagDE);

        return verificatieIdenDocumentenBI;
    }

}
