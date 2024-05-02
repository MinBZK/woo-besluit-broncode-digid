
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

package nl.logius.digid.hsm.crypto.keys;

import CryptoServerAPI.CryptoServerException;
import nl.logius.digid.hsm.crypto.KeysAction;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.KeyInfoResponse;
import nl.logius.digid.hsm.model.KeyListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ListKeys extends KeysAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ListKeys(ConnectionPool pool) {
        super(pool, false);
    }

    public KeyListResponse list(String group) {
        final KeyListResponse response = new KeyListResponse();
        try {
            response.setKeys(getConnection().listKeys(group));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error(String.format("Error getting info of key <%s>", group), e);
            throw new CryptoError("Error getting info of key", e);
        }
        return response;
    }
}
