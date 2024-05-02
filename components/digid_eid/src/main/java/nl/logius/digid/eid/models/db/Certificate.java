
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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import nl.logius.digid.card.crypto.X509Factory;
import nl.logius.digid.eid.models.DocumentType;
import nl.logius.digid.eid.models.PolymorphType;
import nl.logius.digid.eid.models.asn1.CvCertificate;
import nl.logius.digid.eid.models.db.converters.DocumentTypeConverter;
import nl.logius.digid.sharedlib.utils.DateUtils;

@Entity
public class Certificate implements Raw {
    public enum Type {
        CVCA, DVCA, AT, CSCA;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = DocumentTypeConverter.class)
    private DocumentType documentType;
    @Enumerated(EnumType.STRING)
    private Type type;

    private String issuer;
    private String subject;
    @Enumerated(EnumType.STRING)
    private PolymorphType authorization;

    private boolean trusted;
    private ZonedDateTime notBefore;
    private ZonedDateTime notAfter;

    @Lob
    private byte[] raw;

    public static Certificate from(X509Certificate certificate) throws CertificateEncodingException {
        final Certificate c = new Certificate();
        c.issuer = X509Factory.toCanonical(certificate.getIssuerX500Principal());
        c.subject = X509Factory.toCanonical(certificate.getSubjectX500Principal());
        c.raw = certificate.getEncoded();
        c.documentType = c.subject.indexOf("OU=RDW") >= 0 ? DocumentType.DL : DocumentType.NIK;
        c.type = Type.CSCA;
        c.authorization = null;
        c.notBefore = ZonedDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneOffset.UTC);
        c.notAfter = ZonedDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneOffset.UTC);
        return c;
    }

    public static Certificate from(CvCertificate certificate) {
        final Certificate c = new Certificate();
        c.issuer = certificate.getBody().getCar();
        c.subject = certificate.getBody().getChr();
        c.raw = certificate.getRaw();
        c.documentType = documentTypeFromReference(c.subject);
        c.type = typeFromReference(c.subject);
        if (c.type == Type.AT) {
            c.authorization = certificate.getBody().getAuthorization();
            if (c.authorization == null)
                c.authorization = PolymorphType.PIP;
        } else {
            c.authorization = null;
        }
        c.notBefore = ZonedDateTime.of(DateUtils.toLocalDate(certificate.getBody().getEffectiveDate()).atStartOfDay(), ZoneOffset.UTC);
        c.notAfter = ZonedDateTime.of(DateUtils.toLocalDate(certificate.getBody().getExpirationDate()).atStartOfDay(), ZoneOffset.UTC);
        return c;
    }

    public static DocumentType documentTypeFromReference(String reference) {
        return "IRIJ".equals(reference.substring(6, 10)) ? DocumentType.DL : DocumentType.NIK;
    }

    public static Type typeFromReference(String reference) {
        switch (reference.charAt(2)) {
            case 'C':
                return Type.CVCA;
            case 'D':
                return Type.DVCA;
            case 'A':
            case 'I':
                return Type.AT;
            default:
                throw new IllegalArgumentException("Unknown type from reference " + reference);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public PolymorphType getAuthorization() {
        return authorization;
    }

    public void setAuthorization(PolymorphType authorization) {
        this.authorization = authorization;
    }

    public boolean isTrusted() {
        return trusted;
    }

    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    public ZonedDateTime getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(ZonedDateTime notBefore) {
        this.notBefore = notBefore;
    }

    public ZonedDateTime getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(ZonedDateTime notAfter) {
        this.notAfter = notAfter;
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }

    @Override
    public void setRaw(byte[] raw) {
        this.raw = raw;
    }
}
