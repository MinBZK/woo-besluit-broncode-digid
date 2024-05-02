
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

package nl.logius.digid.oidc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jwt.SignedJWT;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.text.ParseException;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public final class AuthenticateRequest {
    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-5][0-9a-fA-F]{3}-[0-9abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")
    private String clientId;

    @NotBlank
    @Pattern(regexp = "code")
    private String responseType;

    @NotBlank
    @Pattern(regexp = "openid")
    private String scope;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String state;

    @NotBlank
    private String nonce;

    @NotBlank
    private String codeChallenge;

    @NotBlank
    @Pattern(regexp = "S256")
    private String codeChallengeMethod;

    private String request;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) throws ParseException {
        this.request = request;

        if (request != null) {
            SignedJWT signedJWT = getSignedJwt();
            setClientId((String) signedJWT.getJWTClaimsSet().getClaim("client_id"));
            setResponseType((String) signedJWT.getJWTClaimsSet().getClaim("response_type"));
            setNonce((String) signedJWT.getJWTClaimsSet().getClaim("nonce"));
            setState((String) signedJWT.getJWTClaimsSet().getClaim("state"));
            setScope((String) signedJWT.getJWTClaimsSet().getClaim("scope"));
            setRedirectUri((String) signedJWT.getJWTClaimsSet().getClaim("redirect_uri"));
            setCodeChallenge((String) signedJWT.getJWTClaimsSet().getClaim("code_challenge"));
            setCodeChallengeMethod((String) signedJWT.getJWTClaimsSet().getClaim("code_challenge_method"));

        }
    }

    @JsonIgnore
    public SignedJWT getSignedJwt() throws ParseException {
        return SignedJWT.parse(request);
    }
}
