
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

package nl.logius.digid.msc.config;

import nl.logius.digid.msc.model.CertificateInfo;
import nl.logius.digid.msc.util.X509CertificateDataParser;
import nl.logius.digid.sharedlib.client.BsnkClient;
import nl.logius.digid.sharedlib.client.HsmClient;
import nl.logius.digid.sharedlib.decryptor.BsnkPseudonymDecryptor;
import nl.logius.digid.sharedlib.utils.CryptoUtils;
import okhttp3.HttpUrl;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

@Configuration
public class DecryptConfig {

    @Value("${urls.internal.hsm-bsnk}")
    private String hsmUrl;

    @Value("${hsm-bsnk.timeout}")
    private int hsmTimeout;

    @Value("${iapi.token}")
    private String iapiToken;

    @Value("${decrypt.private_key}")
    private String privateKeyEncoded;

    @Value("${decrypt.certificate}")
    private String certificateEncoded;

    @Value("${decrypt.closing_key}")
    private int closingKeyVersion;

    @Value("${urls.internal.bsnk}")
    private String bsnkUrl;

    @Value("${bsnk.timeout}")
    private int bsnkTimeout;

    @Bean
    public BsnkPseudonymDecryptor bsnkPseudonymDecryptor() throws GeneralSecurityException, IOException {
        Security.addProvider(new BouncyCastleProvider());
        final PrivateKey privateKey = CryptoUtils.loadPrivateKey(Base64.decode(privateKeyEncoded));
        final X509Certificate certificate = certificate();
        return BsnkPseudonymDecryptor.createBSNKPseudonymDecryptor(hsmClient(), bsnkClient(), privateKey, certificate, closingKeyVersion);
    }

    @Bean
    public CertificateInfo getVersion() throws IOException, GeneralSecurityException {
        X509Certificate cert = certificate();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        CertificateInfo info = new CertificateInfo();
        info.setKsv(formatter.format(cert.getNotBefore()));

        info.setOin(X509CertificateDataParser.getOin(cert.getEncoded()));

        return info;
    }

    private X509Certificate certificate() throws IOException, GeneralSecurityException {
        try (final InputStream is = new ByteArrayInputStream(Base64.decode(certificateEncoded))) {
            return CryptoUtils.loadCertificate(is);
        }
    }

    private HsmClient hsmClient() {
        return new HsmClient(HttpUrl.get(hsmUrl), iapiToken, hsmTimeout);
    }

    private BsnkClient bsnkClient() {
        return new BsnkClient(HttpUrl.get(bsnkUrl), bsnkTimeout);
    }
}
