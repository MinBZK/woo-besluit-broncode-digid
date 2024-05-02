
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

package nl.logius.digid.eid.models.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1OutputStream;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.eid.models.EcSignable;

@Asn1Entity(tagNo = 0x67)
public class CvCertificateRequest implements EcSignable {
    private CvCertificate certificate;
    private String car;
    private EcSignature signature;

    @Override
    public byte[] getTBS() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(certificate.getRaw());
            try (final Asn1OutputStream aos = new Asn1OutputStream(bos, 0x42)) {
                aos.write(car.getBytes(StandardCharsets.UTF_8));
            }
            bos.close();
        } catch (IOException e) {
            throw new Asn1Exception("Unexpected IOException", e);
        }
        return bos.toByteArray();
    }

    @Asn1Property(order = 0)
    public CvCertificate getCertificate() {
        return certificate;
    }

    public void setCertificate(CvCertificate certificate) {
        this.certificate = certificate;
    }

    @Asn1Property(order = 1, tagNo = 0x42)
    public String getCar() {
        return car;
    }

    public void setCar(String car) {
        this.car = car;
    }

    @Asn1Property(order = 2)
    @Override
    public EcSignature getSignature() {
        return signature;
    }

    public void setSignature(EcSignature signature) {
        this.signature = signature;
    }
}
