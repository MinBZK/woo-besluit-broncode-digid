
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

package nl.logius.digid.hsm.config;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.logius.digid.hsm.cryptoserver.Connection;
import nl.logius.digid.hsm.cryptoserver.ConnectionConfig;
import nl.logius.digid.hsm.cryptoserver.ConnectionFactory;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.cryptoserver.DummyObjectPool;

@Configuration
public class HsmPoolConfig {
    @Value("${hsm.pool.enabled}")
    private boolean enabled;

    @Value("${hsm.pool.testOnBorrow}")
    private boolean testOnBorrow;

    @Value("${hsm.pool.testOnCreate}")
    private boolean testOnCreate;

    @Value("${hsm.pool.testOnReturn}")
    private boolean testOnReturn;

    @Value("${hsm.pool.evictionInterval}")
    private int evictionInterval;

    @Value("${hsm.pool.idleTime}")
    private int idleTime;

    @Value("${hsm.pool.maxWait}")
    private int maxWait;

    @Value("${hsm.pool.maxTotal}")
    private int maxTotal;

    @Value("${hsm.pool.maxIdle}")
    private int maxIdle;

    @Value("${hsm.pool.minIdle}")
    private int minIdle;

    @Value("${hsm.lb.testInterval}")
    private int testInterval;

    @Value("${hsm.lb.timeout}")
    private int timeout;

    @Autowired
    private ConnectionFactory factory;

    @Resource(name = "connectionConfigs")
    Map<String, ConnectionConfig> configs;

    @Bean
    public ConnectionPool pool() {
        return new ConnectionPool(objectPool(), configs, testInterval, timeout);
    }

    private KeyedObjectPool<String, Connection> objectPool() {
        if (enabled) {
            return new GenericKeyedObjectPool<>(factory, poolConfig());
        } else {
            return new DummyObjectPool(factory);
        }
    }

    private GenericKeyedObjectPoolConfig poolConfig() {
        final GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();

        // JMX
        config.setJmxEnabled(false);

        // Set when to test on connections
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnCreate(testOnCreate);
        config.setTestOnReturn(testOnReturn);

        // Test connections and abandon when running eviction
        if (evictionInterval > 0) {
            config.setTestWhileIdle(true);
            config.setTimeBetweenEvictionRunsMillis(evictionInterval);
            config.setMinEvictableIdleTimeMillis(idleTime);
            config.setNumTestsPerEvictionRun(-1); // Means all idle connections
        }

        // Set max wait for available connection
        config.setMaxWaitMillis(maxWait);

        // Set pool size
        config.setMaxTotalPerKey(maxTotal);
        config.setMaxIdlePerKey(maxIdle);
        config.setMinIdlePerKey(minIdle);

        return config;
    }
}
