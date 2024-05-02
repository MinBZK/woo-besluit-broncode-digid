
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

package nl.logius.digid.ns.client;

public enum LogEnum {
    DEREGISTER_APP_OK("1453"),
    MNS_SWITCH_OFF("1459"),
    REGISTER_MNS_OK("1460"),
    REGISTER_MNS_ERROR("1461"),
    SEND_NOTIFICATION_SUCCES("1454"),
    SEND_NOTIFICATION_MNS_SWITCH_OFF("1462"),
    DEREGISTER_MNS_OK("1471"),
    DEREGISTER_MNS_ERROR("1472"),
    SCHEDULED_TASK_STARTED("1473"),
    SCHEDULED_TASK_FINISHED("1474"),
    SCHEDULED_TASK_CANCELLED("1475"),
    JWT_RETRIEVAL_SUCCES("1531"),
    JWT_RETRIEVAL_FAILED_MNS_UNAVAILABLE("1532"),
    JWT_RETRIEVAL_FAILED_ERROR("1533"),
    // FIXME: FO needs to specify logcode
    GOOGLE_SWITCH_OFF("1537"),
    APPLE_SWITCH_OFF("9999"),
    GENERIC_SEND_NOTIFICATION_KEY("digid_ns_send_notifications");

    private String value;

    public String getValue() {
        return value;
    }

    LogEnum(String value) {
        this.value = value;
    }
}
