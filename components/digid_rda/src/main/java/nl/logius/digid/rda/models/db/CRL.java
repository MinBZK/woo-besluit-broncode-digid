
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

package nl.logius.digid.rda.models.db;

import nl.logius.digid.card.crypto.X509Factory;

import javax.persistence.*;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
public class CRL implements Raw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String issuer;

    private ZonedDateTime thisUpdate;
    private ZonedDateTime nextUpdate;

    @Lob
    private byte[] raw;

    public static CRL from(X509CRL crl) {
        final CRL c = new CRL();
        c.issuer = X509Factory.toCanonical(crl.getIssuerX500Principal());
        c.thisUpdate = ZonedDateTime.ofInstant(crl.getThisUpdate().toInstant(), ZoneOffset.UTC);
        c.nextUpdate = ZonedDateTime.ofInstant(crl.getNextUpdate().toInstant(), ZoneOffset.UTC);
        try {
            c.raw = crl.getEncoded();
        } catch (CRLException e) {
            throw new RuntimeException("Could not encode CRL", e);
        }
        return c;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public ZonedDateTime getThisUpdate() {
        return thisUpdate;
    }

    public void setThisUpdate(ZonedDateTime thisUpdate) {
        this.thisUpdate = thisUpdate;
    }

    public ZonedDateTime getNextUpdate() {
        return nextUpdate;
    }

    public void setNextUpdate(ZonedDateTime nextUpdate) {
        this.nextUpdate = nextUpdate;
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
