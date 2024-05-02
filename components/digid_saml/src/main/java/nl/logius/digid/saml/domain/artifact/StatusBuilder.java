
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

import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;

public class StatusBuilder {
    private final Status status;

    private StatusBuilder(Status status) {
        this.status = status;
    }

    static StatusBuilder newInstance(Status status) {
        return new StatusBuilder(status);
    }

    StatusBuilder addStatusCode(StatusCode statusCode, String statusCodeValue) {
        if (statusCode == null || statusCodeValue.isBlank()) {
            throw new IllegalArgumentException("-StatusCode or statusValue- in StatusBuilder cannot be empty");
        }
        statusCode.setValue(statusCodeValue);
        status.setStatusCode(statusCode);
        return this;
    }

    StatusBuilder addMessage(StatusMessage statusMessage) {
        if (statusMessage == null || statusMessage.getValue() == null) {
            return this;
        }
        status.setStatusMessage(statusMessage);
        return this;
    }

    public Status build() throws InstantiationException {
        if (status.getStatusCode() == null) {
            throw new InstantiationException("The status is not build correctly");
        }
        return status;
    }

}
