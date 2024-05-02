
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

import nl.logius.digid.saml.domain.session.SamlSession;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AuthenticationRequest extends SamlRequest {

    private AuthnRequest authnRequest;
    private SamlSession samlSession;
    private int minimumRequestedAuthLevel;
    private boolean validSsoSession;
    private int ssoAuthLevel;
    private String assertionConsumerURL;
    private List<String> intendedAudiences;
    private Assertion idpAssertion;

    private Boolean appActive;
    private String appReturnUrl;
    private String entityId;
    private String serviceName;
    private String encryptionIdType;

    public AuthenticationRequest() {
        this.validSsoSession = false;
        this.minimumRequestedAuthLevel = -1;
        this.intendedAudiences = new ArrayList<>();
    }

    public AuthnRequest getAuthnRequest() {
        return authnRequest;
    }
    public void setAuthnRequest(AuthnRequest authnRequest) {
        this.authnRequest = authnRequest;
    }

    public SamlSession getSamlSession() {
        return samlSession;
    }
    public void setSamlSession(SamlSession samlSession) {
        this.samlSession = samlSession;
    }

    public int getMinimumRequestedAuthLevel() {
        return minimumRequestedAuthLevel;
    }
    public void setMinimumRequestedAuthLevel(int minimumRequestedAuthLevel) {
        this.minimumRequestedAuthLevel = minimumRequestedAuthLevel;
    }

    public boolean isValidSsoSession() {
        return validSsoSession;
    }

    public void setValidSsoSession(boolean validSsoSession) {
        this.validSsoSession = validSsoSession;
    }

    public int getSsoAuthLevel() {
        return ssoAuthLevel;
    }
    public void setSsoAuthLevel(int ssoAuthLevel) {
        this.ssoAuthLevel = ssoAuthLevel;
    }

    public String getAssertionConsumerURL() {
        return assertionConsumerURL;
    }

    public void setAssertionConsumerURL(String assertionConsumerURL) {
        this.assertionConsumerURL = assertionConsumerURL;
    }

    public List<String> getIdpEntries() {
        if (authnRequest.getScoping() == null || authnRequest.getScoping().getIDPList() == null)
            return Collections.emptyList();

        return authnRequest.getScoping().getIDPList().getIDPEntrys().stream().map(IDPEntry::getProviderID).collect(Collectors.toList());
    }

    public List<String> getIntendedAudience() {
        return intendedAudiences;
    }

    public void addIntendedAudience(String audience) {
        intendedAudiences.add(audience);
    }

    public void setIdpAssertion(Assertion assertion) {
        idpAssertion = assertion;
    }
    public Assertion getIdpAssertion() {
        return idpAssertion;
    }

    public Boolean getAppActive() {
        return appActive;
    }

    public void setAppActive(Boolean appActive) {
        this.appActive = appActive;
    }

    public String getAppReturnUrl() {
        return appReturnUrl;
    }

    public void setAppReturnUrl(String appReturnUrl) {
        this.appReturnUrl = appReturnUrl;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEncryptionIdType() {
        return encryptionIdType;
    }

    public void setEncryptionIdType(String encryptionIdType) {
        this.encryptionIdType = encryptionIdType;
    }
}
