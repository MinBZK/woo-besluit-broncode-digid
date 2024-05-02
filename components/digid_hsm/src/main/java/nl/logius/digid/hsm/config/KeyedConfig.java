
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

import nl.logius.digid.hsm.exception.ConfigException;
import nl.logius.digid.pp.crypto.CMS;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

abstract class KeyedConfig {
    private final Logger logger = LoggerFactory.getLogger(KeyedConfig.class);

    @Value("${keyFormat}")
    private String keyFormat;

    @Autowired
    private Environment environment;

    protected String[] getLoadKeys(boolean allowEmpty) {
        final String value = StringUtils.trimAllWhitespace(
            environment.getProperty(String.format("%s.load", getPrefix()), ""));
        if (value.isEmpty()) {
            if (allowEmpty)
                logger.warn("No load keys specified for " + getPrefix());
            else
                throw new ConfigException("No load keys specified for " + getPrefix());
        }
        return value.split("\\s*,\\s*");
    }

    abstract protected String getPrefix();

    protected byte[] getBase64Property(String loadKey, String name, boolean useDefault, boolean optional) {
        final String encoded = getProperty(loadKey, name, String.class, useDefault, optional);
        return encoded != null ? Base64.decode(encoded) : null;
    }

    protected byte[] prepareKey(String loadKey, String name, boolean useDefault, boolean optional, X509CertificateHolder certificateHolder) {
        byte[] encryptedKed = null;

        switch (keyFormat) {
            case "PKCS7":
                encryptedKed = getBase64Property(loadKey, name, useDefault, optional);
                break;
            case "BASE64":
                final byte[] base64Encoded = getBase64Property(loadKey, name, useDefault, optional);
                encryptedKed=  encryptWithCertificate(base64Encoded, certificateHolder);
                break;
            case "RAW":
                final String rawKey = getProperty(loadKey, name, String.class, useDefault, optional);
                if (rawKey == null) return null;
                encryptedKed = encryptWithCertificate(rawKey.getBytes(), certificateHolder);
                break;
            default:
                throw new ConfigException("Key format not supported");
        }

        return encryptedKed;
    }

    protected byte[] encryptWithCertificate(byte[] key, X509CertificateHolder certificateHolder) {
        try {
            X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
            if (key != null) {
                InputStream targetStream = new ByteArrayInputStream(key);
                final String encoded = CMS.encrypt(cert, targetStream);
                return encoded != null ? Base64.decode(encoded) : null;
            }
        } catch (IOException | CertificateException e) {
            throw new ConfigException("Could not encrypt key with certificate");
        }
        return null;
    }

    protected <T> T getProperty(String loadKey, String name, Class<T> klass) {
        return getProperty(loadKey, name, klass, false, false);
    }

    protected <T> T getProperty(String loadKey, String name, Class<T> klass, boolean useDefault) {
        return getProperty(loadKey, name, klass, useDefault, false);
    }

    protected <T> T getProperty(String loadKey, String name, Class<T> klass, boolean useDefault, boolean optional) {
        T value = environment.getProperty(String.format("%s.%s.%s", getPrefix(), loadKey, name), klass);
        if (value == null && useDefault) {
            value = environment.getProperty(String.format("%s.default.%s", getPrefix(), name), klass);
        }
        if (value == null && !optional) {
            throw new ConfigException(
                String.format("Could not find property '%s' for key '%s' in '%s'", name, loadKey, getPrefix()));
        }
        return value;
    }
}
