
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

package nl.logius.digid.card.asn1.models;

import java.io.Serializable;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1ObjectIdentifier;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.ObjectIdentifierConverter;
import nl.logius.digid.card.asn1.converters.SetOfIdentifiedConverter;

@Asn1Entity(tagNo = 0x31, converter = SetOfIdentifiedConverter.class, partial = true)
public class SecurityInfos implements Serializable {
    private static final long serialVersionUID = 1L;

    private EcPublicKey ecPublicKey;
    private int ecPublicKeyId;

    private int taVersion;

    private EcParameters caEcParameters;
    private int caKeyId;

    private int caVersion;
    private int caId;

    private int paceVersion;
    private int paceParameterId;

    private int aaVersion;
    private String aaAlgorithm;

    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.1.2")
    @Asn1Property(order = 11)
    public EcPublicKey getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(EcPublicKey ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.1.2")
    @Asn1Property(tagNo = 0x02, order = 12, optional = true)
    public int getEcPublicKeyId() {
        return ecPublicKeyId;
    }

    public void setEcPublicKeyId(int ecPublicKeyId) {
        this.ecPublicKeyId = ecPublicKeyId;
    }


    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.2")
    @Asn1Property(tagNo = 0x02, order = 21)
    public int getTaVersion() {
        return taVersion;
    }

    public void setTaVersion(int taVersion) {
        this.taVersion = taVersion;
    }


    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.3.2")
    @Asn1Property(order = 31, optional = true)
    public EcParameters getCaEcParameters() {
        return caEcParameters;
    }

    public void setCaEcParameters(EcParameters caEcParameters) {
        this.caEcParameters = caEcParameters;
    }

    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.3.2")
    @Asn1Property(tagNo = 0x02, order = 32)
    public int getCaKeyId() {
        return caKeyId;
    }

    public void setCaKeyId(int caKeyId) {
        this.caKeyId = caKeyId;
    }


    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.3.2.4")
    @Asn1Property(tagNo = 0x02, order = 41)
    public int getCaVersion() {
        return caVersion;
    }

    public void setCaVersion(int caVersion) {
        this.caVersion = caVersion;
    }

    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.3.2.4")
    @Asn1Property(tagNo = 0x02, order = 42, optional = true)
    public int getCaId() {
        return caId;
    }

    public void setCaId(int caId) {
        this.caId = caId;
    }


    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.4.2.4")
    @Asn1Property(tagNo = 0x02, order = 51)
    public int getPaceVersion() {
        return paceVersion;
    }

    public void setPaceVersion(int paceVersion) {
        this.paceVersion = paceVersion;
    }

    @Asn1ObjectIdentifier("0.4.0.127.0.7.2.2.4.2.4")
    @Asn1Property(tagNo = 0x02, order = 52)
    public int getPaceParameterId() {
        return paceParameterId;
    }

    public void setPaceParameterId(int paceParameterId) {
        this.paceParameterId = paceParameterId;
    }


    @Asn1ObjectIdentifier("2.23.136.1.1.5")
    @Asn1Property(tagNo = 0x02, order = 61)
    public int getAaVersion() {
        return aaVersion;
    }

    public void setAaVersion(int aaVersion) {
        this.aaVersion = aaVersion;
    }

    @Asn1ObjectIdentifier("2.23.136.1.1.5")
    @Asn1Property(tagNo = 0x06, converter = ObjectIdentifierConverter.class, order = 62)
    public String getAaAlgorithm() {
        return aaAlgorithm;
    }

    public void setAaAlgorithm(String aaAlgorithm) {
        this.aaAlgorithm = aaAlgorithm;
    }


}
