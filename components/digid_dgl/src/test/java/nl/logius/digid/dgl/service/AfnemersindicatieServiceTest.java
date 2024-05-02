
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

package nl.logius.digid.dgl.service;

import nl.logius.digid.dgl.controller.AfnemersberichtAanDGLFactory;
import nl.logius.digid.dgl.model.Afnemersbericht;
import nl.logius.digid.dgl.repository.AfnemersberichtRepository;
import nl.logius.digid.digilevering.api.model.AfnemersberichtAanDGL;
import nl.logius.digid.digilevering.api.model.Ap01;
import nl.logius.digid.digilevering.api.model.Av01;
import nl.logius.digid.digilevering.api.model.BerichtHeaderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AfnemersindicatieServiceTest {

    @InjectMocks
    private AfnemersindicatieService classUnderTest;

    @Mock
    private AfnemersberichtRepository afnemersberichtRepository;

    @Mock
    private AfnemersberichtAanDGLFactory afnemersberichtAanDGLFactory;

    @Mock
    private DglMessageFactory dglMessageFactory;

    @Mock
    private DglSendService dglSendService;

    @Test
    public void testCreateAfnemersindicatie(){
        Ap01 ap01 = new Ap01();
        when(dglMessageFactory.createAp01(anyString())).thenReturn(ap01);

        AfnemersberichtAanDGL afnemersberichtAanDGL = new AfnemersberichtAanDGL();
        BerichtHeaderType berichtHeader = new BerichtHeaderType();
        berichtHeader.setKenmerkVerstrekker("TestKernMerkVerstrekker");
        afnemersberichtAanDGL.setBerichtHeader(berichtHeader);
        when(afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(any(Ap01.class))).thenReturn(afnemersberichtAanDGL);

        classUnderTest.createAfnemersindicatie("SSSSSSSSS");

        verify(afnemersberichtRepository, times(1)).save(any(Afnemersbericht.class));
        verify(dglSendService, times(1)).sendAfnemersBerichtAanDGL(any(AfnemersberichtAanDGL.class),any(Afnemersbericht.class));
    }

    @Test
    public void testDeleteAfnemersindicatie(){
        Av01 av01 = new Av01();
        when(dglMessageFactory.createAv01(anyString())).thenReturn(av01);

        AfnemersberichtAanDGL afnemersberichtAanDGL = new AfnemersberichtAanDGL();
        BerichtHeaderType berichtHeader = new BerichtHeaderType();
        berichtHeader.setKenmerkVerstrekker("TestKernMerkVerstrekker");
        afnemersberichtAanDGL.setBerichtHeader(berichtHeader);
        when(afnemersberichtAanDGLFactory.createAfnemersberichtAanDGL(any(Av01.class))).thenReturn(afnemersberichtAanDGL);

        classUnderTest.deleteAfnemersindicatie("SSSSSSSSSS");

        verify(afnemersberichtRepository, times(1)).save(any(Afnemersbericht.class));
        verify(dglSendService, times(1)).sendAfnemersBerichtAanDGL(any(AfnemersberichtAanDGL.class),any(Afnemersbericht.class));
    }
}
