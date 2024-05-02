
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

import org.bouncycastle.asn1.DERBitString;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

@Asn1Entity(tagNo = 0x30)
public class PolymorphicInfo {
    private int pcaVersion;
    private ImplementationInfo implementationInfo;
    private int schemeVersion;
    private int schemeKeyVersion;
    private DERBitString flags;

    @Asn1Property(tagNo = 0x02, order = 1)
    public int getPcaVersion() {
        return pcaVersion;
    }

    public void setPcaVersion(int pcaVersion) {
        this.pcaVersion = pcaVersion;
    }

    @Asn1Property(tagNo = 0x30, order = 2)
    public ImplementationInfo getImplementationInfo() {
        return implementationInfo;
    }

    public void setImplementationInfo(ImplementationInfo implementationInfo) {
        this.implementationInfo = implementationInfo;
    }

    @Asn1Property(tagNo = 0x02, order = 3)
    public int getSchemeVersion() {
        return schemeVersion;
    }

    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    @Asn1Property(tagNo = 0x02, order = 4)
    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public void setSchemeKeyVersion(int schemeKeyVersion) {
        this.schemeKeyVersion = schemeKeyVersion;
    }

    @Asn1Property(tagNo = 0x03, order = 5)
    public DERBitString getFlags() {
        return flags;
    }

    public void setFlags(DERBitString flags) {
        this.flags = flags;
    }

    @Asn1Entity
    public static class ImplementationInfo {
        private String type;
        private int version;

        @Asn1Property(tagNo = 0x0c, order = 1)
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Asn1Property(tagNo = 0x02, order = 2)
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

    }
}
