
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

package nl.logius.digid.hsm.cryptoserver;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import CryptoServerAPI.CryptoServerException;

import nl.logius.digid.hsm.exception.NotFoundError;

public class ConnectionFactory extends BaseKeyedPooledObjectFactory<String, Connection> {
    private final Map<String, ConnectionConfig> configs;

    public ConnectionFactory(Map<String, ConnectionConfig> configs) {
        this.configs = configs;
    }

    @Override
    public Connection create(String key) throws Exception {
        final ConnectionConfig config = configs.get(key);
        if (config == null) {
            throw new NotFoundError(String.format("Could not find hsm with key '%s'", key));
        }
        return new Connection(config, key);
    }

    @Override
    public PooledObject<Connection> wrap(Connection conn) {
        return (PooledObject<Connection>) new DefaultPooledObject(conn);
    }

    @Override
    public void destroyObject(String key, PooledObject<Connection> obj) throws Exception {
        obj.getObject().close();
    }

    @Override
    public boolean validateObject(String key, PooledObject<Connection> obj) {
        try {
            return obj.getObject().test();
        } catch (IOException | CryptoServerException e) {
            return false;
        }
    }
}
