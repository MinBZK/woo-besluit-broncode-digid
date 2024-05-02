
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

package nl.logius.digid.dws.util;

import nl.logius.digid.dws.exception.SoapValidationException;
import nl.rdw.eid_wus_crb._1.EIDSTATINFO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class EidStatInfoBuilderTest {

    @InjectMocks
    private EidStatInfoBuilder eidStatInfoBuilder;

    @BeforeEach
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void eidstatinfoBuilder_validParameters_infoCorrect() throws SoapValidationException {
        EIDSTATINFO result = eidStatInfoBuilder.eidstatinfoBuilder("PPPPPPPPP", "PPPPPPPPPPPP");

        assertEquals("PPPPPPPPP", result.getEIDSTATAGEG().getBURGSERVNRA().toString());
        assertEquals("PPPPPPPPPPPP", result.getEIDSTATAGEG().getEIDVOLGNRA().toString());
        assertEquals("RIJBEWIJS", result.getEIDSTATAGEG().getEIDDOCTYPE().toString());
    }

    @Test
    public void eidstatinfoBuilder_invalidParameters_exceptionThrown() throws SoapValidationException {
        assertThrows(SoapValidationException.class, () -> {
            eidStatInfoBuilder.eidstatinfoBuilder("AAAAA", "PPPPPPPPPPPP");
        });
    }
}
