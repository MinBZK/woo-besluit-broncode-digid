
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

package nl.logius.digid.saml.domain.authentication;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.AttributeTypes;
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.*;
import nl.logius.digid.saml.exception.DienstencatalogusException;
import nl.logius.digid.saml.exception.SamlSessionException;
import nl.logius.digid.saml.exception.SamlValidationException;
import nl.logius.digid.saml.exception.SharedServiceClientException;
import nl.logius.digid.saml.util.OpenSAMLUtils;
import org.mapstruct.factory.Mappers;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSStringImpl;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static nl.logius.digid.saml.util.Constants.ENTRANCE_REQUEST_AUTHENTICATION_URL;

@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final ParserPool parserPool;
    private final SignatureService signatureService;
    private final DcMetadataService dcMetadataService;
    private final SamlSessionService samlSessionService;
    private final AdClient adClient;
    private final AdService adService;
    private final AssertionConsumerServiceUrlService assertionConsumerServiceUrlService;

    @Autowired
    public AuthenticationService(ParserPool parserPool, SignatureService signatureService, DcMetadataService dcMetadataService, SamlSessionService samlSessionService, AdClient adClient, AdService adService, AssertionConsumerServiceUrlService assertionConsumerServiceUrlService){
        this.parserPool = parserPool;
        this.signatureService = signatureService;
        this.dcMetadataService = dcMetadataService;
        this.samlSessionService = samlSessionService;
        this.adClient = adClient;
        this.adService = adService;
        this.assertionConsumerServiceUrlService = assertionConsumerServiceUrlService;
    }

    @Value("${urls.external.ad}/inloggen")
    private String digidAdFrontendUrl;
    @Value("${http_scheme}")
    private String httpScheme;
    @Value("${urls.external.ad}")
    private String frontChannel;

    public AuthenticationRequest startAuthenticationProcess(HttpServletRequest httpRequest) throws ComponentInitializationException, MessageDecodingException, SamlValidationException, SharedServiceClientException, DienstencatalogusException, SamlSessionException {
        BaseHttpServletRequestXMLMessageDecoder decoder = decodeXMLRequest(httpRequest);
        AuthenticationRequest authenticationRequest = createAuthenticationRequest(httpRequest, decoder);
        SAMLBindingContext bindingContext = createAndValidateBindingContext(decoder);
        validateAuthenticationRequest(authenticationRequest);
        parseAuthentication(authenticationRequest);
        validateWithOtherDomainServices(authenticationRequest, bindingContext);
        return authenticationRequest;
    }

    private BaseHttpServletRequestXMLMessageDecoder decodeXMLRequest(HttpServletRequest httpRequest) throws ComponentInitializationException, MessageDecodingException {
        BaseHttpServletRequestXMLMessageDecoder decoder = new HTTPPostDecoder();
        decoder.setParserPool(parserPool);
        decoder.setHttpServletRequest(httpRequest);
        decoder.initialize();
        decoder.decode();

        logger.info("Decode XML request complete");
        return decoder;
    }

    private AuthenticationRequest createAuthenticationRequest(HttpServletRequest httpRequest, BaseHttpServletRequestXMLMessageDecoder decoder) {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setAuthnRequest((AuthnRequest) decoder.getMessageContext().getMessage());
        authenticationRequest.setRequest(httpRequest);
        logger.info("AuthnRequest decode complete");
        if (logger.isDebugEnabled())
            OpenSAMLUtils.logSAMLObject((AuthnRequest) decoder.getMessageContext().getMessage());

        return authenticationRequest;
    }

    private SAMLBindingContext createAndValidateBindingContext(BaseHttpServletRequestXMLMessageDecoder decoder) throws SamlValidationException {
        SAMLBindingContext bindingContext = decoder.getMessageContext().getSubcontext(SAMLBindingContext.class);
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(bindingContext, "bindingContext");
        ValidationUtils.invokeValidator(new BindingContextValidator(), bindingContext, result);

        if (result.getFieldError("relayState") != null) {
            throw new SamlValidationException("Parameter 'relayState': " + result.getFieldError("relayState").getCode(), result);
        }
        return bindingContext;
    }

    protected BeanPropertyBindingResult validateAuthenticationRequest(AuthenticationRequest authenticationRequest) throws SamlValidationException {
        BeanPropertyBindingResult resultAuthnRequest = new BeanPropertyBindingResult(authenticationRequest.getAuthnRequest(), "authnRequest");

        ValidationUtils.invokeValidator(getValidator(authenticationRequest.getRequest().getMethod()),
                authenticationRequest.getAuthnRequest(),
                resultAuthnRequest);

        if (resultAuthnRequest.hasErrors())
            throw new SamlValidationException("AuthnRequest validation error", resultAuthnRequest);
        logger.info("AuthnRequest message validated");
        return resultAuthnRequest;
    }

    protected void parseAuthentication(AuthenticationRequest authenticationRequest) {
        authenticationRequest.setConnectionEntityId(authenticationRequest.getAuthnRequest().getIssuer().getValue());

        if (authenticationRequest.getAuthnRequest().getExtensions() == null) {
            authenticationRequest.setServiceEntityId(authenticationRequest.getAuthnRequest().getIssuer().getValue());
            authenticationRequest.setAttributeConsumingServiceIdx(authenticationRequest.getAuthnRequest().getAttributeConsumingServiceIndex());
        } else {
            parseAuthenticationWithExtensions(authenticationRequest);
        }

        logger.info("AuthnRequest message parsed");
    }

    protected void parseAuthenticationWithExtensions(AuthenticationRequest authenticationRequest) {
        for (XMLObject xmlObject : authenticationRequest.getAuthnRequest().getExtensions().getUnknownXMLObjects(Attribute.DEFAULT_ELEMENT_NAME)) {
            Attribute attribute = (Attribute) xmlObject;
            switch (attribute.getName()) {
                case AttributeTypes.INTENDED_AUDIENCE -> {
                    authenticationRequest.setServiceEntityId(getStringValue(attribute.getAttributeValues().get(0)));
                    for (XMLObject entityId : attribute.getAttributeValues())
                        authenticationRequest.addIntendedAudience(getStringValue(entityId));
                }
                case AttributeTypes.SERVICE_UUID -> authenticationRequest.setServiceUuid(getStringValue(attribute.getAttributeValues().get(0)));
                case AttributeTypes.IDP_ASSERTION -> {
                    XSAny any = (XSAny) attribute.getAttributeValues().get(0);
                    Assertion assertion = (Assertion) any.getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME).get(0);
                    authenticationRequest.setIdpAssertion(assertion);
                }
                default -> {}
            }
        }
    }

    private void validateWithOtherDomainServices(AuthenticationRequest authenticationRequest, SAMLBindingContext bindingContext) throws SamlValidationException, DienstencatalogusException, SamlSessionException, SharedServiceClientException {
        dcMetadataService.resolveDcMetadata(authenticationRequest);

        signatureService.validateSamlRequest(authenticationRequest, authenticationRequest.getAuthnRequest().getSignature());
        logger.info("AuthnRequest signature validated");

        assertionConsumerServiceUrlService.resolveAssertionConsumerService(authenticationRequest);
        logger.info("AuthnRequest assertion consumer service location set");

        samlSessionService.initializeSession(authenticationRequest, bindingContext);
        logger.info("Saml session initialized");
    }

    protected String prepareAuthenticationToAd(String returnUrl, AuthenticationRequest authenticationRequest) {
        if (authenticationRequest.getFederationName() != null && authenticationRequest.getAuthnRequest().isForceAuthn()) {
            adClient.remoteLog("105", buildPayload("domain", authenticationRequest.getFederationName()));
        }

        AdSession adSession = generateAdSession(returnUrl, authenticationRequest);
        adService.createAuthenticationSession(adSession);
        return digidAdFrontendUrl;
    }

    protected String cancelAuthenticationToAd(AuthenticationRequest authenticationRequest, String artifact) throws SamlSessionException, UnsupportedEncodingException {
        SamlSession samlSession = authenticationRequest.getSamlSession();
        AdSession adSession = generateAdSession(null, authenticationRequest);
        AdAuthenticationStatus adStatus = AdAuthenticationStatus.valueOfLabel(samlSession.getAuthenticationStatus());
        adService.createAuthenticationSession(adSession);
        adService.updateAuthenticationStatus(adSession, adStatus);

        return assertionConsumerServiceUrlService.generateRedirectUrl(
                artifact,
                samlSession.getTransactionId(),
                samlSession.getHttpSessionId(),
                null);
    }

    protected String generateReturnUrl(HttpServletRequest httpRequest, String artifact, String url) throws UnsupportedEncodingException {
        final String serverName = httpRequest.getServerName();
        return httpScheme + "://" + serverName + url + "?SAMLart=" + URLEncoder.encode(artifact, "UTF-8");
    }

    private AdSession generateAdSession(String returnUrl, AuthenticationRequest authenticationRequest) {
        SamlSession samlSession = authenticationRequest.getSamlSession();
        AdAuthenticationMapper adAuthenticationMapper = Mappers.getMapper(AdAuthenticationMapper.class);
        return adAuthenticationMapper.authenticationRequestToAdSession(
                returnUrl,
                authenticationRequest,
                samlSessionService.getActiveSsoWebserviceSessions(samlSession));
    }

    private Map<String, String> buildPayload(String key, String value) {
        final Map<String, String> payload = new HashMap<>();
        payload.put(key, value);
        return payload;
    }

    private String getStringValue(XMLObject object) {
        if (object instanceof XSStringImpl)
            return ((XSStringImpl) object).getValue();

        return ((XSAny) object).getTextContent();
    }

    protected AuthnRequestValidator getValidator(String method)  {
        return new AuthnRequestValidator(frontChannel.concat(ENTRANCE_REQUEST_AUTHENTICATION_URL), method);
    }
}
