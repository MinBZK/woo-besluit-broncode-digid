
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import nl.logius.digid.card.asn1.Asn1Constructed;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.BitStringToByteArrayConverter;

@Asn1Entity(tagNo = 0x30)
public class EcPublicKey implements Asn1Constructed, Serializable {
    private static final long serialVersionUID = 1L;

    private EcParameters parameters;
    private byte[] key;
    private transient ECPoint publicPoint;

    @Override
    public void constructed(Asn1ObjectMapper mapper) {
        initialize();
    }

    public void initialize() {
        publicPoint = parameters.getEcCurve().decodePoint(key);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initialize();
    }

    @Asn1Property(tagNo = 0x30, order = 1)
    public EcParameters getParameters() {
        return parameters;
    }

    public void setParameters(EcParameters parameters) {
        this.parameters = parameters;
    }

    @Asn1Property(tagNo = 0x03, converter = BitStringToByteArrayConverter.class, order = 2)
    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public ECPoint getPublicPoint() {
        return publicPoint;
    }

    /**
     * Returns Java security EC public key spec
     * @return public key spec
     */
    public ECPublicKeySpec getPublicKeySpec() {
        return new ECPublicKeySpec(getPublicPoint(), parameters.getParameterSpec());
    }

    /**
     * Returns Bouncy Castle EC public key parameters
     * @return public key parameters
     */
    public ECPublicKeyParameters getPublicParameters() {
        return new ECPublicKeyParameters(publicPoint, parameters.getDomainParameters());
    }
}
