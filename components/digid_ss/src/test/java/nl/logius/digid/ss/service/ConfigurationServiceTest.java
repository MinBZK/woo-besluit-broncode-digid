
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

package nl.logius.digid.ss.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import nl.logius.digid.ss.model.db.Configuration;
import nl.logius.digid.ss.repository.ConfigurationRepository;

public class ConfigurationServiceTest {
    @Mock
    private ConfigurationRepository repo;
    @InjectMocks
    private ConfigurationService service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() throws IOException {
        Configuration conf1 = new Configuration();
        conf1.setName("1");
        conf1.setValue("1");
        conf1.setDefaultValue("2"); // updated defaultValue
        Configuration conf2 = new Configuration();
        conf2.setName("2");
        conf2.setValue("200");
        conf2.setDefaultValue("2"); // existing conf
        Configuration conf3 = new Configuration();
        conf3.setName("3");
        conf3.setValue("3");
        Configuration conf4 = new Configuration();
        conf4.setName("4");
        conf4.setValue("4");
        conf4.setDefaultValue("4"); // new conf

        List<Configuration> db = new ArrayList<>(); // removed conf
        db.add(conf1);
        db.add(conf2);
        db.add(conf3);

        Mockito.when(repo.findAll()).thenReturn(db);
        Mockito.when(repo.save(Mockito.isA(Configuration.class))).thenReturn(null);
        service.synchronize();
        Mockito.verify(repo, Mockito.times(1)).delete(conf3);
        Mockito.verify(repo, Mockito.times(0)).delete(conf1);
        Mockito.verify(repo, Mockito.times(0)).delete(conf2);
        Mockito.verify(repo, Mockito.times(0)).save(conf2);
        Mockito.verify(repo, Mockito.times(1)).save(conf1);

    }

}
