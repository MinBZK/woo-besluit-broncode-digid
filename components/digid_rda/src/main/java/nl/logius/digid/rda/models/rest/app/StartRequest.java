
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

package nl.logius.digid.rda.models.rest.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.models.SecurityInfos;
import nl.logius.digid.rda.models.DocumentType;

public class StartRequest extends AppRequest {
    private int allowedPaceVersion = 2;
    private byte[] cardAccessData;
    private DocumentType type;

    public StartRequest(byte[] cardAccessData) {
        super();
        this.cardAccessData = cardAccessData;
    }

    public StartRequest(byte[] cardAccessData, DocumentType type) {
        super();
        this.cardAccessData = cardAccessData;
        this.type = type;
    }

    public byte[] getCardAccessData() {
        return cardAccessData;
    }

    public void setCardAccessData(byte[] cardAccessData) {
        this.cardAccessData = cardAccessData;
    }


    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    @JsonIgnore
    public boolean supportsPace() {
        return getCardAccessData() != null && paceInfo().getPaceVersion() == allowedPaceVersion;
    }

    @JsonIgnore
    public SecurityInfos paceInfo() {
        final var mapper = new Asn1ObjectMapper();
        return mapper.read(getCardAccessData(), SecurityInfos.class);
    }
}
