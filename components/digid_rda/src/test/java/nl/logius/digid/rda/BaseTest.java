
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

package nl.logius.digid.rda;

import com.google.common.io.Resources;
import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.rda.models.db.CRL;
import nl.logius.digid.rda.models.db.Certificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.TimeZone;

public class BaseTest {
    protected static final Date OCT_10_2014 = new Date(1412935810000L);
    protected static final Date OCT_10_2018 = new Date(1539166210000L);
    protected static final Date AUG_24_2021 = new Date(1629812749000L);

    @BeforeAll
    public static void addBouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeAll
    public static void setUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    protected static byte[] readFixture(String path) throws IOException {
        return Resources.toByteArray(Resources.getResource(path));
    }

    protected X509Certificate readCertificate(String path) throws IOException {
        return X509Factory.toCertificate(readFixture("csca/" + path));
    }

    protected Certificate loadCertificate(String path, boolean trusted) throws CertificateException, IOException {
        final Certificate cert = Certificate.from(readCertificate(path));
        cert.setTrusted(trusted);
        return cert;
    }

    protected X509CRL readCRL(String path) throws IOException {
        return X509Factory.toCRL(readFixture("csca/" + path));
    }

    protected CRL loadCRL(String path) throws IOException {
        return CRL.from(readCRL(path));
    }
}
