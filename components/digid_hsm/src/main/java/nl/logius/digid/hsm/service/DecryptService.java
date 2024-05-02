
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

package nl.logius.digid.hsm.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.google.common.collect.ImmutableMap;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.model.DecryptRequest;
import nl.logius.digid.hsm.model.ServiceProviderKeysInput;
import nl.logius.digid.hsm.model.VerificationPointsRequest;
import nl.logius.digid.hsm.provider.KeysProvider;
import nl.logius.digid.pp.BsnkType;
import nl.logius.digid.pp.crypto.CMS;
import nl.logius.digid.pp.entity.EncryptedEntity;
import nl.logius.digid.pp.entity.EncryptedIdentity;
import nl.logius.digid.pp.entity.EncryptedPseudonym;
import nl.logius.digid.pp.entity.Identity;
import nl.logius.digid.pp.entity.Pseudonym;
import nl.logius.digid.pp.key.DecryptKey;
import nl.logius.digid.pp.key.IdentityDecryptKey;
import nl.logius.digid.pp.key.PseudonymClosingKey;
import nl.logius.digid.pp.key.PseudonymDecryptKey;
import nl.logius.digid.pp.parser.Asn1Parser;

@Service
public class DecryptService {
    @Autowired
    private BsnkService bsnkService;

    @Autowired
    private KeysProvider keysProvider;

    private final KeyPair key;
    private final ContentSigner signer;

    public DecryptService() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        key = generator.generateKeyPair();
        signer = new JcaContentSignerBuilder("SHA256withRSA").build(key.getPrivate());
    }

    public Map<String, String> decrypt(DecryptRequest request) {
        if (!keysProvider.anyKey()) {
            throw new CryptoError("No suitable keys provider for decryption");
        }

        try {
            final Map<String, byte[]> keys = fetchKeys(request.getEncrypted());

            if (keys.containsKey("ID")) {
                final Identity identity = decryptIdentity(keys, request.getEncrypted());
                return ImmutableMap.of("identifier", identity.getIdentifier(),
                    "type", Character.toString(identity.getType()),
                    "version", Integer.toString(identity.getVersion()));
            } else {
                final Pseudonym pseudonym = decryptPseudonym(keys, request.getEncrypted());
                return ImmutableMap.of("standard", pseudonym.getStandard(),
                    "short", pseudonym.getShort());
            }
        } catch (IOException e) {
            throw new CryptoError("Could not fetch keys from encrypted entity", e);
        }
    }

    private String decryptCms(byte[] data) throws IOException {
        try (final InputStream is = new ByteArrayInputStream(data)) {
            return CMS.read(key.getPrivate(), is);
        }
    }

    private Identity decryptIdentity(Map<String, byte[]> keys, byte[] polymorph) throws IOException {
        final IdentityDecryptKey key = DecryptKey.fromPem(decryptCms(keys.get("ID")), IdentityDecryptKey.class);
        final EncryptedIdentity encrypted = EncryptedEntity.fromBase64(
            Base64.toBase64String(polymorph), key.toVerifiers(verificationPoint(key)), EncryptedIdentity.class);

        return encrypted.decrypt(key);
    }

    private Pseudonym decryptPseudonym(Map<String, byte[]> keys, byte[] polymorph) throws IOException {
        final PseudonymDecryptKey decryptKey = DecryptKey.fromPem(decryptCms(keys.get("PD")), PseudonymDecryptKey.class);
        final PseudonymClosingKey closingKey = DecryptKey.fromPem(decryptCms(keys.get("PC")), PseudonymClosingKey.class);


        final EncryptedPseudonym encrypted = EncryptedEntity.fromBase64(
            Base64.toBase64String(polymorph), decryptKey.toVerifiers(verificationPoint(decryptKey)),
            EncryptedPseudonym.class);

        return encrypted.decrypt(decryptKey, closingKey);
    }

    private Map<String, byte[]> fetchKeys(byte[] polymorph) throws IOException {
        final ServiceProviderKeysInput input = new ServiceProviderKeysInput();

        final Asn1Parser parser = new Asn1Parser(polymorph);
        final BsnkType type = parser.checkHeader();
        final boolean pseudonym = type == BsnkType.ENCRYPTED_PSEUDONYM || type == BsnkType.SIGNED_ENCRYPTED_PSEUDONYM;

        if (type == BsnkType.SIGNED_ENCRYPTED_IDENTITY || type == BsnkType.SIGNED_ENCRYPTED_PSEUDONYM) {
            parser.readObject(DLSequenceParser.class);
            parser.readObject(DLSequenceParser.class);
            parser.readObject(ASN1ObjectIdentifier.class);
        }

        input.setSchemeVersion(parser.readObject(ASN1Integer.class).getValue().intValue());
        input.setSchemeKeyVersion(parser.readObject(ASN1Integer.class).getValue().intValue());
        input.setClosingKeyVersion(pseudonym ? 1 : 0);
        parser.readObject(DERIA5String.class);
        input.setCertificate(generateCertificate(
            parser.readObject(DERIA5String.class).getString(),
            parser.readObject(ASN1Integer.class).getValue().intValue()
        ));

        return keysProvider.serviceProviderKeys(input, pseudonym);
    }

    @Cacheable(cacheNames="decrypt-certificate")
    private X509CertificateHolder generateCertificate(String oin, int ksv) {
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(ksv / 10000, (ksv / 100) % 100 - 1, ksv % 100, 0, 0, 0);
        final Date notBefore = cal.getTime();
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 31);
        final Date notAfter = cal.getTime();
        final X500Name name = new X500Name("CN=digid,serialNumber=" + oin);
        return new JcaX509v3CertificateBuilder(name, BigInteger.ONE, notBefore, notAfter, name, key.getPublic()).build(signer);
    }

    private String verificationPoint(DecryptKey key) {
        final VerificationPointsRequest request = new VerificationPointsRequest();
        request.setSchemeVersion(key.getSchemeVersion());
        request.setSchemeKeyVersion(key.getSchemeKeyVersion());
        final Map<String, byte[]> points = bsnkService.verificationPoints(request);
        if (key instanceof IdentityDecryptKey) {
            return Base64.toBase64String(points.get("IPp"));
        } else {
            return Base64.toBase64String(points.get("PPp"));
        }
    }
}
