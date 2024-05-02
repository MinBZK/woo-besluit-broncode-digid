
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

import nl.logius.digid.saml.client.AdClient;
import nl.logius.digid.saml.domain.session.AdService;
import nl.logius.digid.saml.domain.session.AdSession;
import nl.logius.digid.saml.exception.AdException;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthenticationAppToAppService {
    private final AdClient adClient;
    private final AdService adService;

    @Autowired
    public AuthenticationAppToAppService(AdClient adClient, AdService adService) {
        this.adClient = adClient;

        this.adService = adService;
    }

    public Map<String, Object> createAuthenticationParameters(String relayState, AuthenticationRequest authenticationRequest) throws AdException {

        HashMap<String, String> digidApp = new HashMap<>();
        digidApp.put("name", authenticationRequest.getServiceName());
        digidApp.put("url", authenticationRequest.getAppReturnUrl());

        Map<String, Object> authenticationParameters = new HashMap<>();
        authenticationParameters.put("app_session_id", retrieveAppSessionIdFromAd(authenticationRequest.getAppReturnUrl(), authenticationRequest));
        authenticationParameters.put("SAMLart", authenticationRequest.getSamlSession().getArtifact());
        authenticationParameters.put("apps", authenticationRequest.getAppActive() ? Arrays.asList(digidApp) : Collections.emptyList());
        authenticationParameters.put("authentication_level", authenticationRequest.getSamlSession().getAuthenticationLevel());
        authenticationParameters.put("image_domain", authenticationRequest.getAppReturnUrl());
        authenticationParameters.put("RelayState", relayState);

        return authenticationParameters;
    }

    private String retrieveAppSessionIdFromAd(String returnUrl, AuthenticationRequest authenticationRequest) throws AdException {
        AdSession adSession = generateAdSession(returnUrl, authenticationRequest);
        adService.save(adSession);

        return adClient.startAppSession(adSession.getSessionId()).getAppSessionId();
    }

    private AdSession generateAdSession(String returnUrl, AuthenticationRequest authenticationRequest) {
        AdAuthenticationMapper adAuthenticationMapper = Mappers.getMapper(AdAuthenticationMapper.class);
        return adAuthenticationMapper.authenticationRequestToAdSession(
                returnUrl,
                authenticationRequest,
                List.of());
    }
}
