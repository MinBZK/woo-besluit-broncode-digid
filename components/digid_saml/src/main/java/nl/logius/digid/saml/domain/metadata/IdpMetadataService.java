
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

package nl.logius.digid.saml.domain.metadata;

import nl.logius.digid.saml.exception.MetadataException;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_AUTHENTICATION_URL;
import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_LOGOUT_URL;

@Service
public class IdpMetadataService extends MetadataService {
    private static final String METADATA_TYPE = "idp";

    @Autowired
    public IdpMetadataService(
            XMLObjectBuilderFactory builderFactory,
            @Value("${metadata.idp_entity_id}") String entityID,
            @Value("${saml.idp_store}") String keystoreFile,
            @Value("${saml.idp_store_passphrase}") String keystorePassword) {
        super(builderFactory);
        this.entityID = entityID;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
    }

    public Endpoint getIDPEndpoint() {
        return getEndpointService(frontChannel.concat(ENTRANCE_REQUEST_AUTHENTICATION_URL), SAMLConstants.SAML2_REDIRECT_BINDING_URI, SingleSignOnService.DEFAULT_ELEMENT_NAME);
    }

    @Override
    protected IDPSSODescriptor buildIDPSSODescriptor() throws MetadataException {
        final IDPSSODescriptor idpDescriptor = super.buildIDPSSODescriptor();
        idpDescriptor.getSingleLogoutServices().add(getEndpointService(frontChannel.concat(ENTRANCE_REQUEST_LOGOUT_URL), SAMLConstants.SAML2_POST_BINDING_URI, SingleLogoutService.DEFAULT_ELEMENT_NAME));

        return idpDescriptor;
    }

    @Override
    protected String getMetadataType() {
        return METADATA_TYPE;
    }
}
