
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

package nl.logius.digid.dc.domain.certificate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.logius.digid.dc.Base;
import nl.logius.digid.dc.domain.connection.Connection;
import nl.logius.digid.dc.domain.organization.Organization;
import nl.logius.digid.dc.domain.service.Service;

import javax.persistence.*;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Entity
@Table(name = "certificates")
public class Certificate extends Base {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cachedCertificate;
    private CertificateType certType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "connection_id")
    private Connection connection;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id")
    private Service service;

    private String fingerprint;
    private String distinguishedName;

    private ZonedDateTime activeFrom;
    private ZonedDateTime activeUntil;

    public Certificate() {
    }

    public Certificate(String x509Cert, CertificateType type) {
        certType = type;
        setCachedCertificate(x509Cert);
    }

    public Certificate(String cachedCertificate, String fingerprint, String distinguishedName, CertificateType certType, ZonedDateTime activeFrom, ZonedDateTime activeUntil, Connection connection) {
        this.cachedCertificate = cachedCertificate;
        this.fingerprint = fingerprint;
        this.distinguishedName = distinguishedName;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
        this.connection = connection;
        this.certType = certType;
    }

    public void setCachedCertificate(String cachedCertificate) {
        this.cachedCertificate = cachedCertificate;
        X509Certificate parsedCertificate = getParsedCertificate();

        if (parsedCertificate != null) {
            this.fingerprint = getFingerprintFromParsedCert(parsedCertificate);
            this.distinguishedName = parsedCertificate.getSubjectDN().getName();
            this.activeFrom = parsedCertificate.getNotBefore().toInstant().atZone(ZoneId.of("Europe/Amsterdam"));
            this.activeUntil = parsedCertificate.getNotAfter().toInstant().atZone(ZoneId.of("Europe/Amsterdam"));
        }
    }

    @JsonIgnore
    public X509Certificate getParsedCertificate() {
        String x509Cert = "-----BEGIN CERTIFICATE-----\n" + cachedCertificate + "-----END CERTIFICATE-----";
        final InputStream targetStream = new ByteArrayInputStream(x509Cert.getBytes());
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(targetStream);
        } catch (CertificateException e) {
            logger.error("Error parsing x509 cert: {}", e.getMessage());
        }

        return null;
    }

    @JsonIgnore
    public String getFingerprintFromParsedCert(X509Certificate cert) {
        try {
            return DatatypeConverter.printHexBinary(
                MessageDigest.getInstance("SHA-1").digest(cert.getEncoded())
            ).toUpperCase().replaceAll("..", "$0:").replaceAll(".$", "");
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            logger.error("Error getting fingerprint from cert: {}", e.getMessage());
        }

        return null;
    }

    public Optional<String> getOrganizationName() {
        return Optional.ofNullable(connection)
            .map(Connection::getOrganization)
            .map(Organization::getName);
    }

    public Boolean isConnectionCert() {
        return connection != null;
    }

    public Boolean isServiceCert() {
        return service != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCachedCertificate() {
        return cachedCertificate;
    }

    public CertificateType getCertType() {
        return certType;
    }

    public void setCertType(CertificateType certType) {
        this.certType = certType;
    }
    @JsonIgnore
    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    @JsonIgnore
    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getDistinguishedName() {
        return distinguishedName;
    }

    public void setDistinguishedName(String distinguishedName) {
        this.distinguishedName = distinguishedName;
    }

    public ZonedDateTime getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(ZonedDateTime activeFrom) {
        this.activeFrom = activeFrom;
    }

    public ZonedDateTime getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(ZonedDateTime activeUntil) {
        this.activeUntil = activeUntil;
    }
}

