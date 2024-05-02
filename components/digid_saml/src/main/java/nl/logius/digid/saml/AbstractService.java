
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

package nl.logius.digid.saml;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.client.BvdClient;
import nl.logius.digid.saml.domain.artifact.AssertionConsumerServiceUrlService;
import nl.logius.digid.saml.domain.authentication.SamlRequest;
import nl.logius.digid.saml.domain.credential.SignatureService;
import nl.logius.digid.saml.domain.metadata.DcMetadataService;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.SamlSessionRepository;
import nl.logius.digid.saml.domain.session.SamlSessionService;
import nl.logius.digid.saml.exception.SamlValidationException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXMLMessageDecoder;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.xmlsec.signature.Signature;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractService {

    @Autowired
    protected ParserPool parserPool;

    @Autowired
    protected SignatureService signatureService;

    @Autowired
    protected DcMetadataService dcMetadataService;

    @Autowired
    protected SamlSessionService samlSessionService;

    @Autowired
    protected AdClient adClient;

    @Autowired
    protected BvdClient bvdClient;

    @Autowired
    protected AdService adService;

    @Autowired
    protected SamlSessionRepository samlSessionRepository;

    @Autowired
    protected AssertionConsumerServiceUrlService assertionConsumerServiceUrlService;

    protected BaseHttpServletRequestXMLMessageDecoder decodeRequest(HttpServletRequest request) throws ComponentInitializationException, MessageDecodingException {
        BaseHttpServletRequestXMLMessageDecoder decoder = new HTTPPostDecoder();

        decoder.setParserPool(parserPool);
        decoder.setHttpServletRequest(request);
        decoder.initialize();
        decoder.decode();

        return decoder;
    }

    protected void verifySignature(SamlRequest samlRequest, Signature signature) throws SamlValidationException {
        signatureService.validateSamlRequest(samlRequest, signature);
    }
}
