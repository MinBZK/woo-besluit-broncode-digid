
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableList;

import org.apache.commons.pool2.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.logius.digid.hsm.exception.UnavailableException;

/**
 * Wrapped KeyedObjectPool by providing key less borrow, return and invalidate.
 */
public class ConnectionPool {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final KeyedObjectPool<String, Connection> pool;

    private final AtomicBoolean[] actives;
    private final String[] keys;
    private final Random random = new Random();

    private final ScheduledExecutorService executorService;
    private final int timeout;

    private class Tester implements Runnable {
        private final String key;
        private final ConnectionConfig config;
        private final AtomicBoolean active;

        private Tester(int index, ConnectionConfig config) {
            active = actives[index];
            key = keys[index];
            this.config = config;
        }

        @Override
        public void run() {
            /*
             * We test here by plain socket connection because the CXI library does not use the timeout for connecting,
             * only for reading.
             */
            try (final Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(config.getHost(), config.getPort()), timeout);
            } catch (SocketTimeoutException e) {
                if (active.getAndSet(false)) {
                    logger.warn("Could not connect to hsm '{}' within timeout, deactivated from pool", key);
                }
                return;
            } catch (IOException e) {
                if (active.getAndSet(false)) {
                    logger.error(String.format("Could not connect to hsm '%s', deactivated from pool", key), e);
                }
                return;
            }
            if (!active.getAndSet(true)) {
                logger.warn("Activate hsm '{}' to pool", key);
            }
        }

    }

    public ConnectionPool(KeyedObjectPool<String, Connection> pool, Map<String, ConnectionConfig> configs,
                          int testInterval, int timeout) {
        this.pool = pool;
        this.actives = new AtomicBoolean[configs.size()];
        this.keys = new String[configs.size()];

        this.executorService = Executors.newScheduledThreadPool(configs.size());
        this.timeout = timeout;

        int i = 0;
        for (final Map.Entry<String, ConnectionConfig> entry : configs.entrySet()) {
            this.keys[i] = entry.getKey();
            this.actives[i] = new AtomicBoolean(false);
            executorService.scheduleWithFixedDelay(new Tester(i, entry.getValue()),
                0, testInterval, TimeUnit.SECONDS);
            i++;
        }
    }

    public Connection borrowObject() throws Exception {
        return pool.borrowObject(getKey());
    }

    public List<Connection> borrowAllObjects() throws Exception {
        for (int i = 0; i < actives.length; i++) {
            if (!actives[i].get()) {
                throw new UnavailableException(String.format("HSM '%s' is not active", keys[i]));
            }
        }

        final ImmutableList.Builder<Connection> builder = ImmutableList.builder();
        try {
            for (final String key : keys) {
                builder.add(pool.borrowObject(key));
            }
        } catch (Exception e) {
            for (final Connection conn : builder.build()) {
                try {
                    returnObject(conn);
                } catch (Exception ignore) {
                    logger.error("Failed to return object", ignore);
                }
            }
            throw e;
        }
        return builder.build();
    }

    public void returnObject(Connection conn) throws Exception {
        pool.returnObject(conn.getTag(), conn);
    }

    public void invalidateObject(Connection conn) throws Exception {
        pool.invalidateObject(conn.getTag(), conn);
    }

    private String getKey() {
        int offset = random.nextInt(keys.length);
        for (int n = 0; n < keys.length; n++) {
            final int i = (offset + n) % keys.length;
            if (actives[i].get()) {
                return keys[i];
            }
        }
        logger.error("No active connection to hsm");
        throw new UnavailableException("No active connection to hsm");
    }
}
