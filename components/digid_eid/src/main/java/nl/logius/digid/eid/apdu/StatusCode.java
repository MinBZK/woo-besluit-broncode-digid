
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

package nl.logius.digid.eid.apdu;

import java.util.Arrays;


/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
public enum StatusCode {

    SUCCESS(0x9000), APP_SELECT_FAILED(0x6999), NO_PKCS15_APP(0x6200), END_OF_FILE(0x6282), PIN_DEACTIVATED(0x6283),
    FCI_NO_ISO7816_4(0x6284), VERIFICATION_FAILED(0x6300), INPUT_TIMEOUT(0x6400), INPUT_CANCELLED(0x6401),
    PASSWORDS_DIFFER(0x6402), PASSWORD_OUTOF_RANGE(0x6403), CARD_EJECTED_AND_REINSERTED(0x64a2),
    EEPROM_CELL_DEFECT(0x6581), SECURITY_ENVIRONMENT(0x6600), WRONG_LENGTH(0x6700), NO_BINARY_FILE(0x6981),
    ACCESS_DENIED(0x6982), PASSWORD_COUNTER_EXPIRED(0x6983), DIRECTORY_OR_PASSWORD_LOCKED_OR_NOT_ALLOWED(0x6984),
    NO_PARENT_FILE(0x6985), NOT_YET_INITIALIZED(0x6985), NO_CURRENT_DIRECTORY_SELECTED(0x6986),
    DATAFIELD_EXPECTED(0x6987), INVALID_SM_OBJECTS(0x6988), COMMAND_NOT_ALLOWED(0x69f0), INVALID_DATAFIELD(0x6a80),
    ALGORITHM_ID(0x6a81), FILE_NOT_FOUND(0x6a82), RECORD_NOT_FOUND(0x6a83), INVALID_PARAMETER(0x6a86),
    LC_INCONSISTANT(0x6a87), PASSWORD_NOT_FOUND(0x6a88), ILLEGAL_OFFSET(0x6b00), UNSUPPORTED_CLA(0x6e00),
    CANT_DISPLAY(0x6410), INVALID_P1P2(0x6a00), UNSUPPORTED_INS(0x6d00),  PIN_BLOCKED(0x63c0), PIN_SUSPENDED(0x63c1),
    PIN_RETRY_COUNT_2(0x63c2), SUCCESS_AFTER_RETRIES_3(0x63c3), SUCCESS_AFTER_RETRIES_4(0x63c4),
    SUCCESS_AFTER_RETRIES_A(0x63ca), SUCCESS_AFTER_RETRIES_B(0x63cb), SUCCESS_AFTER_RETRIES_C(0x63cc),
    SUCCESS_AFTER_RETRIES_D(0x63cd), SUCCESS_AFTER_RETRIES_E(0x63ce);

    public final int val;

    StatusCode(int val) {
        this.val = val;
    }

    public static StatusCode decode(int v) {
        return Arrays.stream(values()).filter(t -> t.val == v).findFirst().orElse(null);
    }
}
