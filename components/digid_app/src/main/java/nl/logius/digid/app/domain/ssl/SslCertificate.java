
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

package nl.logius.digid.app.domain.ssl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.logius.digid.app.shared.SQLObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.*;
import java.security.cert.*;
import java.util.*;

@Entity
@Table(name = "ssl_certificates")
@DynamicUpdate
public class SslCertificate extends SQLObject implements Serializable {

    private String subject;
    private String rawCertificate;

    public String getDomain() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRawCertificate() {
        return rawCertificate;
    }

    public void setRawCertificate(String rawCertificate) {
        this.rawCertificate = rawCertificate;
    }

    public String getFingerprint() {
        try {
            return Base64.getEncoder().encodeToString(DigestUtils.sha256(Objects.requireNonNull(getX509Certificate()).getEncoded()));
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    @JsonIgnore
    private X509Certificate getX509Certificate() {
        try {
            String string = "-----BEGIN CERTIFICATE-----\n" + getRawCertificate() + "-----END CERTIFICATE-----";
            final InputStream targetStream = new ByteArrayInputStream(string.getBytes());
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(targetStream);
        } catch (CertificateException e) {
            return null;
        }
    }

    public List<Object> getSans()  {
        try {
            return (List<Object>) getX509Certificate().getSubjectAlternativeNames().stream().toList().stream().map(list -> list.get(1)).toList();
        } catch (CertificateParsingException e) {
            return List.of();
        }
    }

    public Date getExpirationDate()  {
        return getX509Certificate().getNotAfter();
    }
}
