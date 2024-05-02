
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

package nl.logius.digid.saml.domain.credential;

import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.domain.metadata.BvdMetadataService;
import nl.logius.digid.saml.domain.metadata.EntranceMetadataService;
import nl.logius.digid.saml.domain.metadata.IdpMetadataService;
import nl.logius.digid.saml.exception.MetadataException;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SignatureService {
    private final IdpMetadataService idpMetadataService;
    private final BvdMetadataService bvdMetadataService;
    private final EntranceMetadataService entranceMetadataService;

    @Autowired
    public SignatureService(IdpMetadataService idpMetadataService, BvdMetadataService bvdMetadataService, EntranceMetadataService entranceMetadataService) {
        this.idpMetadataService = idpMetadataService;
        this.bvdMetadataService = bvdMetadataService;
        this.entranceMetadataService = entranceMetadataService;
    }

    public void validateSamlRequest(SamlRequest samlRequest, Signature signature) throws SamlValidationException {
        try {
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(signature);
        } catch (SignatureException e) {
            throw new SamlValidationException("Request check signature handler exception", e);
        }

        List<Credential> credentials = retrieveServiceProviderCredentials(samlRequest.getConnectionEntity());
        boolean canValidateWithOneOfTheCredentails = credentials.stream().anyMatch(credential -> {
            try {
                SignatureValidator.validate(signature, credential);
                return true;
            } catch (SignatureException e) {
                return false;
            }
        });

        if (!canValidateWithOneOfTheCredentails)
            throw new SamlValidationException("Request check signature handler exception");
    }

    public void signSAMLObject(SignableSAMLObject signableSamlObject) throws SamlValidationException {
        String signType = "IDP";
        signSAMLObject(signableSamlObject, signType);
    }

    public void signSAMLObject(SignableSAMLObject signableSamlObject, String signType) throws SamlValidationException {
        try {
            final var signature = OpenSAMLUtils.buildSAMLObject(Signature.class);
            Credential credential;
            KeyInfo keyInfo;

            switch (signType) {
                case "IDP":
                    credential = idpMetadataService.getCredential();
                    keyInfo = idpMetadataService.getKeyInfo(true);
                    break;
                case "BVD":
                    credential = bvdMetadataService.getCredential();
                    keyInfo = bvdMetadataService.getKeyInfo(true);
                    break;
                case "TD":
                    credential = entranceMetadataService.getCredential();
                    keyInfo = entranceMetadataService.getKeyInfo(true);
                    break;
                default:
                    throw new IllegalArgumentException("SignType is not supported: " + signType);
            }

            signature.setSigningCredential(credential);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
            signature.setKeyInfo(keyInfo);

            signableSamlObject.setSignature(signature);

            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signableSamlObject).marshall(signableSamlObject);

            Signer.signObject(signature);
        } catch (MarshallingException | SignatureException | MetadataException e) {
            throw new SamlValidationException("Signature failed", e);
        }
    }

    private List<Credential> retrieveServiceProviderCredentials(EntityDescriptor entity) {
        List<X509Certificate> certificates = getSigningCertificates(entity.getRoleDescriptors().get(0));
        return certificates.stream().map(c -> {
            try {
                return CredentialSupport.getSimpleCredential(KeyInfoSupport.getCertificate(c), null);
            } catch (CertificateException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<X509Certificate> getSigningCertificates(RoleDescriptor roleDescriptors) {
        List<KeyDescriptor> keyDescriptorList = roleDescriptors.getKeyDescriptors();

        return keyDescriptorList.stream()
                .filter(k -> k.getUse().getValue().equals("signing"))
                .map(KeyDescriptor::getKeyInfo)
                .map(KeyInfo::getX509Datas)
                .map(data -> data.get(0))
                .map(X509Data::getX509Certificates)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
