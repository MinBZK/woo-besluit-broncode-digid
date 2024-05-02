
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

package nl.logius.digid.app.config;

import nl.logius.digid.sharedlib.converter.ByteArrayReaderConverter;
import nl.logius.digid.sharedlib.converter.ByteArrayWriterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Arrays;

/**
 * Redis configuration
 */
@Configuration
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedisConfig {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.cluster.host:}")
    private String clusterHost;

    @Value("${redis.cluster.port:6379}")
    private int clusterPort;

    @Value("${redis.pool.maxTotal}")
    private int poolMaxTotal;

    @Value("${redis.pool.maxIdle}")
    private int poolMaxIdle;

    @Value("${redis.pool.minIdle}")
    private int poolMinIdle;

    @Value("${redis.cache.minutesToCache:15}")
    private int minutesToCache;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
          .entryTtl(Duration.ofMinutes(minutesToCache))
          .disableCachingNullValues();
    }

    @Bean
    public JedisConnectionFactory connectionFactory() {
        if (clusterHost.isEmpty()) {
            logger.info("JedisConnectionFactory created with Redis server: {}:{}", redisHost, redisPort);
            return new JedisConnectionFactory(standaloneConfig());
        } else {
            logger.info("Pooling with max total {}, max idle {}, min idle {}", poolMaxTotal, poolMaxIdle, poolMinIdle);
            logger.info("JedisConnectionFactory created with Redis cluster: {}:{}", clusterHost, clusterPort);
            return new JedisConnectionFactory(clusterConfig(), poolConfig());
        }
    }

    private RedisStandaloneConfiguration standaloneConfig() {
        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(RedisPassword.of(redisPassword));
        }
        return config;
    }

    private RedisClusterConfiguration clusterConfig() {
        final RedisClusterConfiguration config = new RedisClusterConfiguration();
        config.addClusterNode(new RedisNode(clusterHost, clusterPort));
        if (!redisPassword.isEmpty()) {
            config.setPassword(RedisPassword.of(redisPassword));
        }
        return config;
    }

    private JedisPoolConfig poolConfig() {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(poolMaxTotal);
        config.setMaxIdle(poolMaxIdle);
        config.setMinIdle(poolMinIdle);
        return config;
    }

    @Bean
    public RedisTemplate redisTemplate(JedisConnectionFactory factory) {
        final RedisTemplate template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashKeySerializer(template.getKeySerializer());
        template.setHashValueSerializer(template.getValueSerializer());
        return template;
    }

    @Bean
    public RedisCustomConversions redisCustomConversions() {
        return new RedisCustomConversions(Arrays.asList(
                new ByteArrayReaderConverter(),
                new ByteArrayWriterConverter()
        ));
    }
}
