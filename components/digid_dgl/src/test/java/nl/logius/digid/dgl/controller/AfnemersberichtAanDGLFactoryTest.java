
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

package nl.logius.digid.dgl.controller;

import nl.logius.digid.dgl.service.DglMessageFactory;
import nl.logius.digid.digilevering.api.model.AfnemersberichtAanDGL;
import nl.logius.digid.digilevering.api.model.Ap01;
import nl.logius.digid.digilevering.api.model.Av01;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AfnemersberichtAanDGLFactoryTest {

    @InjectMocks
    private AfnemersberichtAanDGLFactory classUnderTest;

    @InjectMocks
    private DglMessageFactory dglMessageFactory;

    @Test
    public void testcreateAfnemersberichtAanDGLAp01(){
        Ap01 testAp01 = dglMessageFactory.createAp01("SSSSSSSSS");
        AfnemersberichtAanDGL result = classUnderTest.createAfnemersberichtAanDGL(testAp01);

        assertEquals("Ap01", result.getStuurgegevens().getBerichtsoort().getNaam());
        assertEquals("1.0", result.getStuurgegevens().getBerichtsoort().getVersie());
        assertEquals(testAp01, result.getInhoud().getAp01());
    }

    @Test
    public void testcreateAfnemersberichtAanDGLAv01(){
        Av01 testAv01 = dglMessageFactory.createAv01("SSSSSSSSSS");
        AfnemersberichtAanDGL result = classUnderTest.createAfnemersberichtAanDGL(testAv01);

        assertEquals("Av01", result.getStuurgegevens().getBerichtsoort().getNaam());
        assertEquals("1.0", result.getStuurgegevens().getBerichtsoort().getVersie());
        assertEquals(testAv01, result.getInhoud().getAv01());
    }

}
