
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

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.hsm.crypto.KeysAction;
import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.KeyInfoResponse;

import CryptoServerAPI.CryptoServerException;

public class GenerateKey extends KeysAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public GenerateKey(ConnectionPool pool) {
        super(pool, true);
    }

    public KeyInfoResponse generate(String group, String name) {
        final KeyInfoResponse response = new KeyInfoResponse();
        final List<Connection> connections = getConnections();
        try {
            response.setPublicKey(connections.get(0).generateKey(group, name));
        } catch (IOException | CryptoServerException e) {
            setLastException(connections.get(0), e);
            logger.error(String.format("Error generating key <%s,%s>", group, name), e);
            throw new CryptoError("Error generating key", e);
        }
        replicate(connections, group, name);
        return response;
    }

    private void replicate(List<Connection> connections, String group, String name) {
        if (connections.size() == 1) {
            return;
        }

        int i = 0;
        try {
            final byte[] backup = connections.get(0).backupKey(group, name);
            for (i = 1; i < connections.size(); i++) {
                connections.get(i).restoreKey(backup);
            }
        } catch (IOException | CryptoServerException e) {
            setLastException(connections.get(i), e);
            logger.error(String.format("Error replicating key <%s,%s>", group, name), e);
            throw new CryptoError("Error replicating key", e);
        }
    }
}
