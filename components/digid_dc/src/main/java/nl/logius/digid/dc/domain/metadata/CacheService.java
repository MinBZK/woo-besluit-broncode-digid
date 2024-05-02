
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

package nl.logius.digid.dc.domain.metadata;

import nl.logius.digid.dc.domain.connection.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class CacheService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheService.class);

    private final CacheManager cacheManager;
    private final RedisTemplate template;

    @Autowired
    public CacheService(CacheManager cacheManager, @Qualifier("redisTemplate") RedisTemplate template) {
        this.cacheManager = cacheManager;
        this.template = template;
    }

    @Cacheable(value = "metadata", key = "#connection.getEntityId()")
    public String getCacheableMetadata(Connection connection) {
        var metadata = new String(Base64.getDecoder().decode(connection.getSamlMetadata()));

        LOGGER.info("cache not found for {}", connection.getEntityId());

        return metadata;
    }

    public void evictSingleCacheValue(String cacheName, String cacheKey) {
        if (cacheKey != null) {
            var cache = cacheManager.getCache(cacheName);

            if (cache != null) {
                cache.evictIfPresent(cacheKey);
            }
        }
    }

    public void evictRelatedCacheValues(String cacheName, String prefix) {
        var cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            template.delete(template.keys(String.format("%s::%s-*", cacheName, prefix)));
        }
    }
}
