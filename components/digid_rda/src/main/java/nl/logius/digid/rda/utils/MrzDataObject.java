
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

import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.models.DocumentType;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MrzDataObject {

    private static final String MRZ_CHARS = "<0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private char compositeCheckDigit;
    private String dateOfBirth;
    private char dateOfBirthCheckDigit;
    private String dateOfExpiry;
    private char dateOfExpiryCheckDigit;
    private String documentCode;
    private String documentNumber;
    private char documentNumberCheckDigit;
    private DocumentType documentType;
    private String firstNames;
    private String gender;
    private String issuingState;
    private String lastName;
    private String nationality;
    private String optionalData1;
    private String optionalData2;

    public MrzDataObject(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Null string");
        }
        str = str.trim().replace("\n", "");
        try {
            readObject(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)), str.length());
        } catch (IOException | RdaException ioe) {
            throw new IllegalArgumentException("Exception", ioe);
        }
    }

    private static String trimFillerChars(String str) {
        byte[] chars = str.trim().getBytes();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '<') {
                chars[i] = ' ';
            }
        }
        return (new String(chars)).trim();
    }

    private static String mrzFormat(String str, int width) {
        if (str == null) {
            return "";
        }
        if (str.length() > width) {
            throw new IllegalArgumentException("Argument too wide (" + str.length() + " > " + width + ")");
        }
        str = str.toUpperCase().trim();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (MRZ_CHARS.indexOf(c) == -1) {
                result.append('<');
            } else {
                result.append(c);
            }
        }
        while (result.length() < width) {
            result.append("<");
        }
        return result.toString();
    }

    public String getMrzIdentifier() {
        StringBuilder composite = new StringBuilder();
        composite.append(getGender());
        composite.append(getDateOfBirth());
        composite.append(getLastName().replace(" ", ""));

        for (String name : getFirstNames()) {
            composite.append(name.replace(" ", ""));
        }

        return composite.toString();
    }

    public char getCompositeCheckDigit() {
        return compositeCheckDigit;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public char getDateOfBirthCheckDigit() {
        return dateOfBirthCheckDigit;
    }

    public String getDateOfExpiry() {
        return dateOfExpiry;
    }

    public char getDateOfExpiryCheckDigit() {
        return dateOfExpiryCheckDigit;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public char getDocumentNumberCheckDigit() {
        return documentNumberCheckDigit;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String[] getFirstNames() {
        return firstNames.split(" |<");
    }

    public String getGender() {
        return gender;
    }

    public String getIssuingState() {
        return issuingState;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNationality() {
        return nationality;
    }

    public String getOptionalData1() {
        return optionalData1;
    }

    public String getOptionalData2() {
        return optionalData2;
    }

    private String readStringWithFillers(DataInputStream inputStream, int count) throws IOException {
        return trimFillerChars(readString(inputStream, count));
    }

    private String readString(DataInputStream inputStream, int count) throws IOException {
        byte[] data = new byte[count];
        inputStream.readFully(data);
        return new String(data).trim();
    }

    private void readObject(InputStream inputStream, int length) throws IOException, RdaException {
        DataInputStream dataIn = new DataInputStream(inputStream);

        this.documentCode = readStringWithFillers(dataIn, 2);
        this.issuingState = readString(dataIn, 3);
        this.documentType = getDocumentTypeFromCode(documentCode, length);

        if (this.documentType == DocumentType.ID_CARD) {
            readIdCard(dataIn);
        } else if (this.documentType == DocumentType.PASSPORT) {
            readPassport(dataIn);
        } else if (this.documentType == DocumentType.DRIVING_LICENCE) {
            readDrivingLicence(dataIn);
        }
    }

    private void readDrivingLicence(DataInputStream dataIn) throws IOException {
        this.documentNumber = trimFillerChars(readString(dataIn, 10));
    }

    private void readPassport(DataInputStream dataIn) throws IOException {
        readNameIdentifiers(readString(dataIn, 39));

        this.documentNumber = trimFillerChars(readString(dataIn, 9));
        this.documentNumberCheckDigit = (char) dataIn.readUnsignedByte();
        this.nationality = readString(dataIn, 3);
        this.dateOfBirth = readString(dataIn, 6);
        this.dateOfBirthCheckDigit = (char) dataIn.readUnsignedByte();
        this.gender = readString(dataIn, 1);
        this.dateOfExpiry = readString(dataIn, 6);
        this.dateOfExpiryCheckDigit = (char) dataIn.readUnsignedByte();
        String personalNumber = readStringWithFillers(dataIn, 14);
        char personalNumberCheckDigit = (char) dataIn.readUnsignedByte();
        this.optionalData1 = mrzFormat(personalNumber, 14) + personalNumberCheckDigit;
        this.compositeCheckDigit = (char) dataIn.readUnsignedByte();
    }

    private void readIdCard(DataInputStream dataIn) throws IOException {
        this.documentNumber = readString(dataIn, 9);
        this.documentNumberCheckDigit = (char) dataIn.readUnsignedByte();
        this.optionalData1 = readStringWithFillers(dataIn, 15);

        if (documentNumberCheckDigit == '<' && !optionalData1.isEmpty()) {
            this.documentNumber += optionalData1.substring(0, optionalData1.length() - 1);
            this.documentNumberCheckDigit = optionalData1.charAt(optionalData1.length() - 1);
            this.optionalData1 = null;
        }

        this.documentNumber = trimFillerChars(this.documentNumber);
        this.dateOfBirth = readString(dataIn, 6);
        this.dateOfBirthCheckDigit = (char) dataIn.readUnsignedByte();
        this.gender = readString(dataIn, 1);
        this.dateOfExpiry = readString(dataIn, 6);
        this.dateOfExpiryCheckDigit = (char) dataIn.readUnsignedByte();
        this.nationality = readString(dataIn, 3);
        this.optionalData2 = readString(dataIn, 11);
        this.compositeCheckDigit = (char) dataIn.readUnsignedByte();

        readNameIdentifiers(readString(dataIn, 30));
    }

    private DocumentType getDocumentTypeFromCode(String documentCode, int length) throws RdaException {
        if (documentCode.startsWith("D") || length == 29) {
            return DocumentType.DRIVING_LICENCE;
        } else if (documentCode.startsWith("I")) {
            return DocumentType.ID_CARD;
        } else if (documentCode.startsWith("P")) {
            return DocumentType.PASSPORT;
        } else {
            throw new RdaException(null, "The document is not a valid type of traveldocument");
        }
    }


    private void readNameIdentifiers(String mrzNameString) {
        int delimIndex = mrzNameString.indexOf("<<");
        if (delimIndex < 0) {
            /* Only a primary identifier. */
            lastName = trimFillerChars(mrzNameString);
            firstNames = "";
            return;
        }
        lastName = trimFillerChars(mrzNameString.substring(0, delimIndex));
        firstNames = mrzNameString.substring(mrzNameString.indexOf("<<") + 2);
    }

    public String getPersonalNumber() {
        if (optionalData1 == null || optionalData1.length() == 0)
            return null;

        var bsn = (optionalData1.substring(0, optionalData1.length() - 1)).replace("<", "").trim();
        if (bsn.length() != 9)
            return null;

        return bsn;
    }
}
