
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

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import nl.logius.digid.hsm.cryptoserver.ConnectionConfig;
import nl.logius.digid.hsm.cryptoserver.ConnectionFactory;

@Configuration
public class HsmConfig extends KeyedConfig {
    @Override
    protected String getPrefix() {
        return "hsm.device";
    }

    @Bean(name = "connectionConfigs")
    public Map<String, ConnectionConfig> configs() throws IOException {
        final ImmutableMap.Builder builder = new ImmutableMap.Builder<>();
        for (String loadKey : getLoadKeys(false)) {
            builder.put(loadKey, create(loadKey));
        }
        return builder.build();
    }

    private ConnectionConfig create(String loadKey) throws IOException {
        final ConnectionConfig config = new ConnectionConfig();
        config.setAddress(getProperty(loadKey, "address", String.class));
        config.setTimeout(getProperty(loadKey, "timeout", Integer.class, true));
        config.setPwdUsername(getProperty(loadKey, "auth_pwd.username", String.class, true, true));
        config.setPwdPassword(getProperty(loadKey, "auth_pwd.password", String.class, true, true));
        config.setKeyUsername(getProperty(loadKey, "auth_key.username", String.class, true, true));
        config.setKeyFile(getBase64Property(loadKey, "auth_key.file", true, true));
        config.setKeyPin(getProperty(loadKey, "auth_key.pin", String.class, true, true));
        return config;
    }

    @Bean
    public ConnectionFactory factory() throws IOException {
        return new ConnectionFactory(configs());
    }


}
