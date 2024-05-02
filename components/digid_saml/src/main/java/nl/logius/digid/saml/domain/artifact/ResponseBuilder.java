
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

package nl.logius.digid.saml.domain.artifact;

import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;

import java.time.Instant;

public class ResponseBuilder {
    private final Response response;

    private ResponseBuilder(Response response) {
        this.response = response;
    }

    static ResponseBuilder newInstance(Response response) {
        return new ResponseBuilder(response);
    }

    ResponseBuilder addID() {
        response.setID(new RandomIdentifierGenerationStrategy().generateIdentifier());
        return this;
    }

    ResponseBuilder addIssueInstant() {
        response.setIssueInstant(Instant.now());
        return this;
    }

    ResponseBuilder addInResponseTo(String authnId) {
        if (authnId.isBlank()) {
            throw new IllegalArgumentException("-authnId- in Response cannot be empty");
        }
        response.setInResponseTo(authnId);
        return this;
    }

    ResponseBuilder addIssuer(Issuer issuer, String issuerValue) {
        if (issuer == null || issuerValue.isBlank()) {
            throw new IllegalArgumentException("-Issuer or issuerValue- is Response cannot be empty");
        }
        issuer.setValue(issuerValue);
        response.setIssuer(issuer);
        return this;
    }

    ResponseBuilder addStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("-Status- in Response cannot be empty");
        }
        response.setStatus(status);
        return this;
    }

    ResponseBuilder addDestination(String destination) {
        if (destination.isBlank()) {
            throw new IllegalArgumentException("-Destination- in Response cannot be empty");
        }
        response.setDestination(destination);
        return this;
    }

    ResponseBuilder addAssertion(Assertion assertion) {
        if (assertion == null) {
            return this;
        }
        response.getAssertions().add(assertion);
        return this;
    }

    Response build() throws InstantiationException {
        if (response.getID() == null ||
                response.getIssuer() == null ||
                response.getInResponseTo().isBlank() ||
                response.getStatus() == null ||
                response.getDestination().isBlank()
        ) {
            throw new InstantiationException("The response is not build correctly");
        }
        return response;
    }
}
