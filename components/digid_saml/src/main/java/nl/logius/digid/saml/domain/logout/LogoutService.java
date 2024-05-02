
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

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import nl.logius.digid.saml.AbstractService;
import nl.logius.digid.saml.domain.authentication.ProtocolType;
import nl.logius.digid.saml.domain.session.AdSession;
import nl.logius.digid.saml.domain.session.FederationSession;
import nl.logius.digid.saml.domain.session.FederationSessionRepository;
import nl.logius.digid.saml.domain.session.SamlSession;
import nl.logius.digid.saml.exception.*;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_LOGOUT_URL;

@Service
public class LogoutService extends AbstractService {
    private static Logger logger = LoggerFactory.getLogger(LogoutService.class);
    private final ApplicationContext ctx;
    private final FederationSessionRepository federationSessionRepository;
    private final VelocityEngine velocityEngine;

    @Value("${metadata.idp_entity_id}")
    private String idpEntityId;
    @Value("${urls.external.ad}")
    private String frontChannel;

    @Autowired
    public LogoutService(ApplicationContext ctx, FederationSessionRepository federationSessionRepository, VelocityEngine velocityEngine) {
        this.ctx = ctx;
        this.federationSessionRepository = federationSessionRepository;
        this.velocityEngine = velocityEngine;
    }

    public LogoutRequestModel parseLogoutRequest(HttpServletRequest request) throws SamlValidationException, SamlParseException, SamlSessionException, DienstencatalogusException {

        final LogoutRequestModel logoutRequestModel = new LogoutRequestModel();

        try {
            final BaseHttpServletRequestXMLMessageDecoder decoder = decodeRequest(request);
            var logoutRequest = (LogoutRequest) decoder.getMessageContext().getMessage();

            final SAMLBindingContext bindingContext = decoder.getMessageContext().getSubcontext(SAMLBindingContext.class);
            logoutRequestModel.setLogoutRequest(logoutRequest);
            logoutRequestModel.setRequest(request);

            validateRequest(logoutRequestModel);

            var id = logoutRequest.getNameID() != null ? logoutRequest.getNameID().getValue() : logoutRequest.getSessionIndexes().get(0).getValue();

            var samlSession = samlSessionRepository.findById(id)
                    .orElseThrow(() -> new SamlSessionException("LogoutRequest no saml session found for nameID: " + id));

            logoutRequestModel.setConnectionEntityId(samlSession.getConnectionEntityId());
            logoutRequestModel.setServiceEntityId(samlSession.getServiceEntityId());
            logoutRequestModel.setServiceUuid(samlSession.getServiceUuid());
            logoutRequestModel.setRelayState(bindingContext.getRelayState());
            logoutRequestModel.setEntranceSession(samlSession.getProtocolType().equals(ProtocolType.SAML_COMBICONNECT));

            dcMetadataService.resolveDcMetadata(logoutRequestModel);
            if (!logoutRequestModel.getConnectionEntityId().equals(logoutRequestModel.getLogoutRequest().getIssuer().getValue())) {
                throw new SamlValidationException("Issuer not equal to connectorEntityId");
            }

            verifySignature(logoutRequestModel, logoutRequestModel.getLogoutRequest().getSignature());

            logout(samlSession);

            if (logger.isDebugEnabled())
                OpenSAMLUtils.logSAMLObject((LogoutRequest) decoder.getMessageContext().getMessage());

        } catch (MessageDecodingException e) {
            throw new SamlParseException("Authentication deflate decode exception", e);
        } catch (ComponentInitializationException e) {
            throw new SamlParseException("Authentication deflate initialization exception", e);
        }

        return logoutRequestModel;
    }

    private void validateRequest(LogoutRequestModel logoutRequestModel) throws SamlValidationException {
        // Validation
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(logoutRequestModel.getLogoutRequest(), "logoutRequest");

        var logoutRequest = logoutRequestModel.getLogoutRequest();

        ValidationUtils.invokeValidator(new LogoutRequestValidator(frontChannel.concat(ENTRANCE_REQUEST_LOGOUT_URL)), logoutRequest, result);

        if (logoutRequestModel.getRequest().getMethod().equals("GET") &&
                (logoutRequestModel.getRequest().getParameter("SigAlg") == null || logoutRequestModel.getRequest().getParameter("Signature") == null)) {
            throw new SamlValidationException("Request check signature handler exception");
        }

        if (result.hasErrors()) throw new SamlValidationException("LogoutRequest validation error", result);
    }

    public void generateResponse(LogoutRequestModel logoutRequestModel, HttpServletResponse response) throws SamlParseException, SamlValidationException {

        try {
            final LogoutResponseFactory factory = new LogoutResponseFactory(logoutRequestModel, ctx);
            final LogoutResponse logoutResponse = factory.getLogoutResponse();

            final MessageContext context = new MessageContext();
            context.setMessage(logoutResponse);

            SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
            SAMLEndpointContext endpointContext = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);
            endpointContext.setEndpoint(factory.getEndpoint());

            SecurityParametersContext signingContext = context.getSubcontext(SecurityParametersContext.class, true);
            SignatureSigningParameters signingParams = new SignatureSigningParameters();
            Signature signature = logoutResponse.getSignature();
            signingParams.setSigningCredential(signature.getSigningCredential());
            signingParams.setSignatureAlgorithm(signature.getSignatureAlgorithm());
            signingContext.setSignatureSigningParameters(signingParams);

            SAMLBindingSupport.setRelayState(context, logoutRequestModel.getRelayState());

            if (!factory.getEndpoint().getBinding().equals(SAMLConstants.SAML2_POST_BINDING_URI)) {
                throw new SamlParseException("LogoutResponse binding not supported");
            }
            HTTPPostEncoder encoder = new HTTPPostEncoder();
            encoder.setVelocityEngine(velocityEngine);

            encoder.setHttpServletResponse(response);
            encoder.setMessageContext(context);

            encoder.prepareContext();
            encoder.initialize();
            encoder.encode();

        } catch (MessageEncodingException e) {
            throw new SamlParseException("LogoutResponse decode exception", e);
        } catch (ComponentInitializationException e) {
            throw new SamlParseException("LogoutResponse initialization exception", e);
        }
    }

    // logout single saml session and federation session if present
    public void logout(SamlSession samlSession) {
        samlSessionService.deleteSingleSamlSession(samlSession);
        if (samlSession.getFederationName() != null) {
            FederationSession federationSession;
            Optional<FederationSession> federationSessionResult =
                    federationSessionRepository.findByHttpSessionIdAndFederationName(samlSession.getHttpSessionId(), samlSession.getFederationName());
            if (federationSessionResult.isPresent()) {
                federationSession = federationSessionResult.get();
                federationSessionRepository.delete(federationSession);
            }

            for (SamlSession session : samlSessionRepository.findByHttpSessionIdAndFederationName(samlSession.getHttpSessionId(), samlSession.getFederationName())) {
                samlSessionService.deleteSingleSamlSession(session);
            }

            try {
                AdSession adSession = adService.getAdSession(samlSession.getHttpSessionId());
                adService.removeAdSession(adSession);
            } catch (AdException e) {
                logger.error("AdSession does not exist");
            }
        }
    }
}
