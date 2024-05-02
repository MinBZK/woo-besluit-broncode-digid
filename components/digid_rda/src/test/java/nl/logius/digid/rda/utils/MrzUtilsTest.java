
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

package nl.logius.digid.rda.utils;

import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.rda.models.MrzInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MrzUtilsTest {

    @Test
    public void checkMrzCheckDigit() {
        MrzUtils.checkMrzCheckDigit("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        MrzUtils.checkMrzCheckDigit("PPPPPPPPP");
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkMrzCheckDigit("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }

    @Test
    public void createTravelDocumentSeedMrzPositive() {
        MrzUtils.createTravelDocumentSeed(new MrzInfo("SPECI2014", "SSSSSS", "SSSSSS"));
    }
    
    @Test
    public void checkDrivingLicenceMrzPositive() {
        MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
    }

    @Test
    public void checkDrivingLicenceMrzCheckFirstLetterWrong() {
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }

    @Test
    public void checkDrivingLicenceMrzCheckBapWrong() {
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }

    @Test
    public void checkDrivingLicenceMrzCountryWrong() {
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }

    @Test
    public void checkDrivingLicenceMrzLengthWrong() {
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }

    @Test
    public void checkDrivingLicenceMrzCheckDigitWrong() {
        assertThrows(VerificationException.class, () -> {
            MrzUtils.checkDrivingLicenceMrz("PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
        });
    }
}
