
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

package nl.logius.digid.rda.models.card;

import nl.logius.digid.card.asn1.Asn1Constructed;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1Property;
import nl.logius.digid.card.asn1.converters.SubjectPublicKeyInfoConverter;
import nl.logius.digid.card.crypto.CryptoException;
import nl.logius.digid.card.crypto.DssRsaSignatureVerifier;
import nl.logius.digid.card.crypto.EcSignatureVerifier;
import nl.logius.digid.card.crypto.SignatureVerifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import java.io.IOException;

@Asn1Entity(tagNo = 0x6f)
public class AaPublicKey implements Asn1Constructed {
    private SubjectPublicKeyInfo keyInfo;
    private AsymmetricKeyParameter keyParams;

    @Override
    public void constructed(Asn1ObjectMapper mapper) {
        try {
            keyParams = PublicKeyFactory.createKey(keyInfo);
        } catch (IOException e) {
            throw new Asn1Exception("Unexpected IO exception", e);
        }
    }

    public SignatureVerifier verifier() {
        if (keyParams instanceof ECPublicKeyParameters) {
            return new EcSignatureVerifier((ECPublicKeyParameters) keyParams);
        } else if (keyParams instanceof RSAKeyParameters) {
            return new DssRsaSignatureVerifier((RSAKeyParameters) keyParams);
        } else {
            throw new CryptoException("Unsupported signature method");
        }
    }

    @Asn1Property(tagNo = 0x30, converter = SubjectPublicKeyInfoConverter.class)
    public SubjectPublicKeyInfo getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(SubjectPublicKeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }

    public AsymmetricKeyParameter getKeyParams() {
        return keyParams;
    }



}
