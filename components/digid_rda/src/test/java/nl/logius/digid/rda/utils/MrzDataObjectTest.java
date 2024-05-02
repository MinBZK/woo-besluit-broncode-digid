
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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MrzDataObjectTest {

    private static final String MRZ_PPPPPPPPPPPPPPPPPPPP_ID1 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPPPPP_ID1 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPP_ID1 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPP_ID1 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPP_ID1 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP";


    private static final String MRZ_PPPPPPPPPPPPPPPPPPP_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP_CHECKDIGIT = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP_CHECKDIGIT = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_VZOR_SPECIMEN_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_HAPPY_TRAVELER_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP",
        MRZ_PPPPPPPPPPPPPPPPP_2LINE_ID3 = "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP" + "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP";


    @Test
    void testDecodeEncodeAll() {
        testDecodeEncode(MRZ_PPPPPPPPPPPPP_3LINE_ID1, "I", "NLD", "PPPPPPP", new String[]{"PPPPP", "N", "J", "P"}, "SSSSSSSSS", "PPPPPP", "P", "PPPPPP", "NLD", "PPPPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
        testDecodeEncode(MRZ_PPPPPPPPPPPPPP_3LINE_ID1, "PP", "PPP", "PPPPPP", new String[]{"PPPPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPPPPPPPPP_3LINE_ID1, "ID", "PPP", "PPPPP", new String[]{"PPPPPPPP", "PPPPPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPPPPPP_3LINE_ID1, "ID", "PPP", "PPPPPPPP", new String[]{"PPPPP", "PPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPP_3LINE_ID1, "I", "PPP", "PPPPPPPP", new String[]{"PPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPP", null);

        testDecodeEncode(MRZ_PPPPPPPPPPPPPPP_2LINE_ID3_ZERO_CHECKDIGIT, "P", "NLD", "PPPPPPPPPP", new String[]{"PPPP", "PPPPPPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "NLD", "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
        testDecodeEncode(MRZ_PPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPPPPP", new String[]{"PPPP", "PPPPP"}, "PPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPPPPPP", new String[]{"PPPPPPPPP"}, "PPPPPPPPP", "PPPPPP", "P", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPPPPP", new String[]{"PPPP"}, "PPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPP", new String[]{"PPPPP"}, "PPPPPPPPP", "PPPPPP", "P", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPP", null);

        testDecodeEncode(MRZ_PPPPPPPPPPPPPPP_2LINE_ID3_FILLER_CHECKDIGIT, "P", "NLD", "PPPPPPPPPP", new String[]{"PPPP", "PPPPPPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "NLD", "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP_2LINE_ID3, "P", "NLD", "PPPPPPPPPPPPPPPPP", new String[]{"PPPPPP", "PPPPPPP", "PPPPP"}, "PPPPPPPPP", "PPPPPP", "P", "PPPPPP", "NLD", "PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPPPPPPP", new String[]{"PPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_HAPPY_TRAVELER_2LINE_ID3, "P", "PPP", "PPPPPPPP", new String[]{"PPPPP"}, "PPPPPPPPP", "PPPPPP", "P", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPPPPPP", new String[]{"PPPPPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPPPPP", null);
        testDecodeEncode(MRZ_PPPPPPPPPPPPPPPPP_2LINE_ID3, "P", "PPP", "PPPPP", new String[]{"PPPP", "PPP"}, "PPPPPPPPP", "PPPPPP", "F", "PPPPPP", "PPP", "PPPPPPPPPPPPPPPPPPP", "PPPPPPPPP");
    }


    public void testDecodeEncode(String mrz, String documentCode, String nationality, String lastName, String[] firstNames, String documentNumber, String dateOfBirth, String gender, String dateOfExpiry, String issuingState, String mrzIdentifier, String bsn) {
        MrzDataObject mrzInfo = new MrzDataObject(mrz);
        assertEquals(documentCode, mrzInfo.getDocumentCode());
        assertEquals(nationality, mrzInfo.getNationality());
        assertEquals(lastName, mrzInfo.getLastName());
        assertTrue(Arrays.equals(firstNames, mrzInfo.getFirstNames()));
        assertEquals(documentNumber, mrzInfo.getDocumentNumber());
        assertEquals(issuingState, mrzInfo.getIssuingState());
        assertEquals(dateOfBirth, mrzInfo.getDateOfBirth());
        assertEquals(gender, mrzInfo.getGender());
        assertEquals(dateOfExpiry, mrzInfo.getDateOfExpiry());

        assertEquals(mrzIdentifier, mrzInfo.getMrzIdentifier());
        assertEquals(bsn, mrzInfo.getPersonalNumber());
    }
}
