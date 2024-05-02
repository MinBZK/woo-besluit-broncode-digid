
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

package nl.logius.digid.scheduler.service;

import nl.logius.digid.scheduler.model.db.Task;
import nl.logius.digid.scheduler.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class TaskServiceTest {
    @Mock
    private TaskRepository repo;
    @InjectMocks
    private TaskService service;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void synchronizeTasksBetweenYamlAndDatabase() throws IOException {
        Task conf1 = new Task();
        conf1.setName("1");
        conf1.setCron("1");
        Task conf2 = new Task();
        conf2.setName("2");
        conf2.setCron("200");
        Task conf3 = new Task();
        conf3.setName("3");
        conf3.setCron("3");
        Task conf4 = new Task();
        conf4.setName("4");
        conf4.setCron("4");

        List<Task> db = new ArrayList<>(); // removed conf
        db.add(conf1);
        db.add(conf2);
        db.add(conf3);
        db.add(conf4);

        Mockito.when(repo.findAll()).thenReturn(db);
        Mockito.when(repo.save(Mockito.isA(Task.class))).thenReturn(null);

        service.synchronize();

        Mockito.verify(repo, times(1)).save(any(Task.class)); // save 5
        Mockito.verify(repo, times(2)).delete(any(Task.class)); // 3 + 4
    }

}
