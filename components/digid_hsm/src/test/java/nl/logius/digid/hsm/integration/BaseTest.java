
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

package nl.logius.digid.hsm.integration;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import nl.logius.digid.pp.crypto.BrainpoolP320r1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.TimeZone;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "default", "integration-test" })
public abstract class BaseTest {
    protected static final String GROUP = "AT";
    protected static final String NAME = "SSSSSSSSSSSSSSSS";

    protected static final ECPoint PUBLIC_KEY = BrainpoolP320r1.CURVE.decodePoint(Base64.decode(
        "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
    ));

    protected static final List<String> KEY_LIST = ImmutableList.of("SSSSSSSSSSSSS","SSSSSSSSSSSSSSSS","SSSSSSSSSSSSSSSS","SSSSSSSSSSSSSSSS","SSSSSSSSSSSSSSSS");

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${iapi.token}")
    private String token;

    @BeforeAll
    public static void addBouncyCastleProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeAll
    public static void setUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public TestRestTemplate getTemplate() {
        return restTemplate;
    }

    public String getToken() {
        return token;
    }

    protected String getPolymorph(String type, boolean signed) throws IOException {
        final String path = String.format("111111110%s-%s.txt", signed ? "-signed" : "", type);
        return Resources.toString(Resources.getResource(path), StandardCharsets.US_ASCII);
    }

    protected String getEncrypted(String type, boolean signed, String receiver) throws IOException {
        final String path = String.format("111111110%s-%s-%s.txt", signed ? "" : "-unsigned", type, receiver);
        return Resources.toString(Resources.getResource(path), StandardCharsets.US_ASCII);
    }

    protected String getKey(String oin, String type) throws IOException {
        final String path = String.format("%s-%s.pem", oin, type);
        return Resources.toString(Resources.getResource(path), StandardCharsets.US_ASCII);
    }

    protected X509Certificate getCertificate() throws Exception {
        try (final InputStream is = Resources.getResource("hsm.crt").openStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        }
    }

    protected PrivateKey getPrivateKey() throws Exception {
        final byte[] encoded = Resources.toByteArray(Resources.getResource("hsm.key"));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}
