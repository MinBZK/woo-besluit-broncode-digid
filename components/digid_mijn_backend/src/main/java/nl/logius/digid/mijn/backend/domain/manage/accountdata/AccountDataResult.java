
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

package nl.logius.digid.mijn.backend.domain.manage.accountdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.logius.digid.mijn.backend.domain.manage.AccountResult;
import nl.logius.digid.mijn.backend.domain.manage.email.EmailStatus;

public class AccountDataResult extends AccountResult {
        @JsonProperty("email_status")
        private EmailStatus emailStatus;

        @JsonProperty("current_email_address")
        private String currentEmailAddress;

        @JsonProperty("setting_2_factor")
        private Boolean setting2Factor;

        @JsonProperty("unread_notifications")
        private int unreadNotifications;

        @JsonProperty("classified_deceased")
        private Boolean classifiedDeceased;

        public EmailStatus getEmailStatus() {
            return emailStatus;
        }

        public void setEmailStatus(EmailStatus emailStatus) {
            this.emailStatus = emailStatus;
        }

        public String getCurrentEmailAddress() {
            return currentEmailAddress;
        }

        public void setCurrentEmailAddress(String currentEmailAddress) {
            this.currentEmailAddress = currentEmailAddress;
        }

        public Boolean getSetting2Factor() {
            return setting2Factor;
        }

        public void setSetting2Factor(Boolean setting2Factor) {
            this.setting2Factor = setting2Factor;
        }

        public int getUnreadNotifications() {
            return unreadNotifications;
        }

        public void setUnreadNotifications(int unreadNotifications) {
            this.unreadNotifications = unreadNotifications;
        }

        public Boolean getClassifiedDeceased() {
            return classifiedDeceased;
        }

        public void setClassifiedDeceased(Boolean classifiedDeceased) {
            this.classifiedDeceased = classifiedDeceased;
        }
}
