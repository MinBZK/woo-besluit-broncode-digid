
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

package nl.logius.digid.eid.service;

import java.math.BigInteger;
import java.util.List;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import nl.logius.digid.card.asn1.models.EcSignature;
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.exceptions.ServerException;
import nl.logius.digid.eid.models.EcPublicParams;
import nl.logius.digid.eid.models.EcSignable;
import nl.logius.digid.sharedlib.client.HsmClient;

@Component
public class SignatureService {
    private static final String GROUP_AT = "AT";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("hsm-eac")
    private HsmClient hsmClient;

    /**
     * Serves the public key and generates key if necessary
     * @param subject subject of certificate
     * @return public key of signature
     */
    public byte[] getOrGenerateKey(String subject) {
        try {
            byte[] pk = publicKey(subject);
            return (pk == null ? hsmClient.generateKey(GROUP_AT, subject).getPublicKey() : pk);
        } catch (nl.logius.digid.sharedlib.exception.ClientException e) {
            logger.error("Could not generate key", e);
            throw new ServerException("Could not generate key");
        }
    }

    /**
     * Requests the public key
     * @param subject subject of certificate
     * @return public key of signature
     */
    public byte[] publicKey(String subject) {
        try {
            return hsmClient.keyInfo(GROUP_AT, subject).getPublicKey();
        } catch (nl.logius.digid.sharedlib.exception.ClientException e) {
            if (e.getStatus() == 404) {
                logger.warn(e.getMessage());
                return null;
            }
            logger.error("Could not get info of key", e);
            throw new ServerException("Could not get info of key");
        }
    }

    /**
     * Requests list of AT keys
     * @return List of public key names
     */
    public List<String> keyList() {
        try {
            return hsmClient.keyList(GROUP_AT).getKeys();
        } catch (nl.logius.digid.sharedlib.exception.ClientException e) {
            logger.error("Could not resolve key list", e);
            throw new ServerException("Could not fetch resolve key list");
        }
    }


    /**
     * Create the signature.
     * @param data data to be signed
     * @param signer subject of certificate that signs
     * @param raw true if a raw signature needs to created otherwise ASN1 signature (DER sequence of two ASN1 integers)
     * @return signature
     */
    public byte[] sign(byte[] data, String signer, boolean raw) {
        try {
            return hsmClient.sign(GROUP_AT, signer, data, raw);
        } catch (nl.logius.digid.sharedlib.exception.ClientException e) {
            logger.error("Could not sign data", e);
            throw new ServerException("Could not sign data");
        }
    }

    /**
     * Create signature of the object
     * @param obj signable object
     * @param signer subject of certificate that signs
     * @param raw true if a raw signature needs to created otherwise ASN1 signature (DER sequence of two ASN1 integers)
     * @return signature
     */
    public byte[] sign(EcSignable obj, String signer, boolean raw) {
        return sign(obj.getTBS(), signer, raw);
    }

    /**
     * Verify signature of data
     * @param data signed data (if hash is needed, assumed that it is already done)
     * @param r point r of signature
     * @param s point s of signature
     * @param publicPoint public point on the curve
     * @param domainParams domain parameters of elliptic curve
     */
    public void verify(byte[] data, BigInteger r, BigInteger s, ECPoint publicPoint, ECDomainParameters domainParams) {
        final ECDSASigner signer = new ECDSASigner();
        signer.init(false,  new ECPublicKeyParameters(publicPoint, domainParams));
        if (!signer.verifySignature(data, r, s)) {
            throw new ClientException("Invalid signature");
        }
    }

    /**
     * Verify signature of data
     * @param data signed data (if hash is needed, assumed that it is already done)
     * @param signature signature that needs to be checked
     * @param publicParams public point and digest algorithm
     * @param domainParams domain parameters of elliptic curve
     */
    public void verify(byte[] data, EcSignature signature, EcPublicParams publicParams,
                       ECDomainParameters domainParams) {
        final byte[] digest = DigestUtils.digest(publicParams.getDigestAlgorithm()).digest(data);
        verify(digest, signature.r, signature.s, publicParams.getPoint(domainParams.getCurve()),
            domainParams);
    }

    /**
     * Verify signature of data
     * @param obj signable object
     * @param publicParams public point and digest algorithm
     * @param domainParams domain parameters of elliptic curve
     */
    public void verify(EcSignable obj, EcPublicParams publicParams, ECDomainParameters domainParams) {
        verify(obj.getTBS(), obj.getSignature(), publicParams, domainParams);
    }
}
