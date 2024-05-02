
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

package nl.logius.digid.hsm.crypto;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CryptoServerAPI.CryptoServerException;

import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.UnavailableException;

abstract class Action implements Closeable {

    private final List<Connection> connections;
    private final ConnectionPool pool;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Exception lastException;
    private Connection lastExceptionConnection;

    protected Action(ConnectionPool pool, boolean getAll) {
        this.pool = pool;
        try {
            connections = getAll ? pool.borrowAllObjects() : ImmutableList.of(pool.borrowObject());
        } catch (UnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Could not borrow connection from pool", e);
            throw new UnavailableException("Error connecting to HSM", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            for (final Connection connection : connections) {
                if (checkBroken(connection)) {
                    pool.invalidateObject(connection);
                } else {
                    pool.returnObject(connection);
                }
            }
        } catch (Exception e) {
            logger.error("Could not return connection to pool", e);
        }
    }

    protected Connection getConnection() {
        return connections.get(0);
    }

    protected List<Connection> getConnections() {
        return connections;
    }

    protected void setLastException(Exception exception) {
        this.lastException = exception;
        this.lastExceptionConnection = getConnection();
    }

    protected void setLastException(Connection connection, Exception exception) {
        this.lastException = exception;
        this.lastExceptionConnection = connection;
    }

    private boolean checkBroken(Connection connection) {
        if (connection != lastExceptionConnection) {
            return false;
        }

        if (lastException instanceof IOException) {
            logger.warn("Connection is broken because of IO error");
            return true;
        }
        if (lastException instanceof CryptoServerException) {
            final CryptoServerException exception = (CryptoServerException) lastException;
            if (Connection.isBroken(exception)) {
                logger.warn(String.format("Connection is broken because of CXI error %X", exception.ErrorCode));
                return true;
            }
        }
        return false;
    }
}
