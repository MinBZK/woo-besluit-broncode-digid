
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

package nl.logius.digid.eid.models.db;

import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.google.common.io.Resources;
import nl.logius.digid.card.crypto.X509Factory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CRLTest {
    @Test
    public void shouldConvertCrl() throws IOException, CRLException {
        byte[] data = Resources.toByteArray(Resources.getResource("test/root.crl"));
        final X509CRL x509 = X509Factory.toCRL(data);
        final CRL crl = CRL.from(x509);
        assertEquals("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", crl.getIssuer());
        assertEquals(ZonedDateTime.of(2018, 3, 23, 13, 27, 07, 0, ZoneOffset.UTC), crl.getThisUpdate());
        assertEquals(ZonedDateTime.of(2051, 7, 15, 13, 27, 07, 0, ZoneOffset.UTC), crl.getNextUpdate());
        assertArrayEquals(x509.getEncoded(), crl.getRaw());
    }
}
