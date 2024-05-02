
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

package nl.logius.digid.saml.domain.logout;

import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.springframework.context.ApplicationContext;

import java.time.Instant;

public class LogoutResponseFactory {

    private final LogoutRequestModel logoutRequestModel;
    private final String idpEntityId;
    private final String entranceEntityId;
    private final SignatureService signatureService;

    public LogoutResponseFactory(LogoutRequestModel logoutRequestModel, ApplicationContext ctx) {
        this.logoutRequestModel = logoutRequestModel;
        this.signatureService = (SignatureService)ctx.getBean("signatureService");
        this.idpEntityId = ctx.getEnvironment().getProperty("metadata.idp_entity_id");
        this.entranceEntityId = ctx.getEnvironment().getProperty("metadata.entrance_entity_id");
    }

    public LogoutResponse getLogoutResponse() throws SamlValidationException {
        return buildLogoutResponse(logoutRequestModel);
    }

    public Endpoint getEndpoint() {
        return logoutRequestModel.getConnectionEntity().getRoleDescriptors().get(0).getEndpoints(SingleLogoutService.DEFAULT_ELEMENT_NAME).get(0);
    }

    private LogoutResponse buildLogoutResponse(LogoutRequestModel logoutRequestModel) throws SamlValidationException {
        final LogoutResponse logoutResponse = OpenSAMLUtils.buildSAMLObject(LogoutResponse.class);
        logoutResponse.setID((new RandomIdentifierGenerationStrategy()).generateIdentifier());
        logoutResponse.setInResponseTo(logoutRequestModel.getLogoutRequest().getID());
        logoutResponse.setIssueInstant(Instant.now());
        logoutResponse.setDestination(getEndpoint().getLocation());

        final Issuer issuer = OpenSAMLUtils.buildSAMLObject(Issuer.class);

        if (logoutRequestModel.isEntranceSession()) {
            issuer.setValue(entranceEntityId);
        } else {
            issuer.setValue(idpEntityId);
        }

        logoutResponse.setIssuer(issuer);

        final Status status = OpenSAMLUtils.buildSAMLObject(Status.class);
        final StatusCode statusCode = OpenSAMLUtils.buildSAMLObject(StatusCode.class);
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);
        logoutResponse.setStatus(status);

        signatureService.signSAMLObject(logoutResponse);

        return logoutResponse;
    }
}
