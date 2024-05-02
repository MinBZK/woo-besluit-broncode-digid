
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

package nl.logius.digid.dc.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import nl.logius.digid.dc.Status;
import nl.logius.digid.dc.exception.CsvFormatException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public abstract class CsvImportBase {

    protected int currentRow;
    protected boolean validRow;

    protected int columnSize;
    protected int defaultCharSize;

    protected List<String> failedRows;
    protected List<String> succeededRows;
    protected List<String[]> entries;
    protected Map<String, Long> duplicateEntries;

    protected static final String SUCCEEDED = "succeeded";
    protected static final String FAILED = "failed";
    protected static final String ERROR = "error";
    protected static final String RESULT = "result";
    protected static final String INCORRECT = "Incorrect ";

    private static final String UUID_REGEX = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$";
    private static final String OIN_REGEX = "^[0-9]{20}$";

    public Map<String, Object> processCsvFile(String encodedCsvData, boolean dryRun) throws JsonProcessingException {
        succeededRows = new ArrayList<>();
        failedRows = new ArrayList<>();
        HashMap<String,  Object> result = new HashMap<>();
        currentRow = 0;

        try {
            String csvData = decodeCsvData(encodedCsvData);
            checkCsvFormat(csvData);
            validRow = true;
            getUniqueIdentifiers().forEach((column, columnIndex) -> {
                checkUniqueEntries(columnIndex);
                if (!duplicateEntries.isEmpty()) {
                    validRow = false;
                    duplicateEntries.forEach((k, v) -> failedRows.add(column + " " + k + " komt " + v + " keer voor"));
                }
            });
            result.put(FAILED, failedRows);
            if (validRow) {
                entries.forEach(line -> { currentRow++; processCsvLine(line, dryRun);});

                result.put(SUCCEEDED, succeededRows);
                result.put(RESULT, "Bestand verwerkt");
            } else {
                result.put(RESULT, "Bestand niet verwerkt");
            }
        } catch (CsvFormatException e) {
            result.put(RESULT, e.getMessage());
        } catch (Exception e) {
            getLogger().error("An error has occurred parsing the file: {}", e.getMessage());
            result.put(RESULT, "Bestand niet verwerkt: Er is een onverwachte fout opgetreden, bekijk de applicatie logging voor meer informatie");
        }
        return result;
    }

    protected abstract void processCsvLine(String[] line, boolean dryRun);

    protected Status buildStatus (Status status, Boolean active, ZonedDateTime activeFrom, ZonedDateTime activeUntil ) {
        status.setActive(active);
        if (activeFrom != null)
            status.setActiveFrom(activeFrom);
        if (activeUntil != null)
            status.setActiveUntil(activeUntil);
        return status;
    }

    protected void checkCsvFormat(String csvData) throws IOException, CsvValidationException {
        CSVReader reader = new CSVReader(new StringReader(csvData));
        String[] line;
        entries = new ArrayList<>();
        while ((line = reader.readNext()) != null) {
            if (line.length != columnSize) {
                throw new CsvFormatException(String.format("Het ingevoerde CSV-bestand heeft een onjuist formaat. We verwachten %s kolommen maar dit zijn er %s", columnSize, line.length));
            }
            entries.add(line);
        }
    }

    protected String decodeCsvData(String encodedData) throws UnsupportedEncodingException {
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(encodedData);
        return new String(decodedBytes, StandardCharsets.UTF_8.name());
    }

    protected boolean validatePresent(String value, String field) {
        if (value.length() == 0) {
            addResult(ERROR, field + " ontbreekt");
            return false;
        }

        return true;
    }

    protected boolean validateSize(String value, String field, Integer size) {
        if (value.length() > size) {
            addResult(ERROR, field + " heeft een limiet van " + size + " karakters");
            return false;
        }

        return true;
    }

    protected String extractMandatoryValue(String value, String field) {
        return extractMandatoryValue(value, field, defaultCharSize);
    }

    protected String extractMandatoryValue(String value, String field, Integer size) {
        if (!validatePresent(value, field)) {
            return "";
        }

        validateSize(value, field, size);

        return value;
    }

    protected Boolean extractBoolean(String value, String field) {
        if (!validatePresent(value, field)) {
            return false;
        }

        if (value.equals("0") || value.equals("1")) {
            return value.equals("1");
        }

        addResult(ERROR, INCORRECT + field + ": " + value);
        return false;
    }

    protected String extractStringEnum(String value, String field, String[] options, Boolean required) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, field)) {
            return "";
        }

        if (ArrayUtils.contains(options, value)) return value;

        addResult(ERROR, INCORRECT + field + ": waarde is " + value + " maar verwachten een van " + Arrays.toString(options));

        return options[0];
    }

    protected ZonedDateTime extractZonedDateTime(String value, String field, Boolean required) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, field)) {
            return null;
        }
        if (value.length() == 0) return null;

        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, dateTimeformatter);
            return dateTime.atZone(ZoneId.of("Europe/Amsterdam"));
        } catch (DateTimeParseException e) {
            addResult(ERROR, INCORRECT + field + ": waarde is " + value + " maar verwachten dit formaat: dd-MM-yyyy HH:mm");
        }

        return null;
    }

    protected String extractString(String value, String field, Boolean required, Integer size) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, field)) {
            return "";
        }
        validateSize(value, field, size);

        return value.length() == 0 ? null : value;
    }

    protected String failedPrefix() {
        return "Regel: " + currentRow + " Foutbericht: ";
    }

    protected void addResult(String result, String message) {
        switch (result) {
            case SUCCEEDED -> succeededRows.add("Regel: " + currentRow + " " + message);
            case ERROR -> {
                validRow = false;
                failedRows.add(failedPrefix() + message);
            }
            case FAILED -> failedRows.add(failedPrefix() + message);
            default -> {
            }
        }
    }

    protected Integer extractIntegerList(String value, String field, Set<Integer> options, Boolean required) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, field)) {
            return 0;
        }
        if (value.length() == 0) return null;
        if (options.contains(extractInteger(value, field, required)) && isParsable(value)) return Integer.parseInt(value);

        addResult(ERROR, INCORRECT + field + ": " + value);
        return null;
    }

    protected Integer extractInteger(String value, String field, Boolean required) {
        if (Boolean.TRUE.equals(required) && !validatePresent(value, field)) {
            return 0;
        }
        if (value.length() == 0) return null;
        if (isParsable(value)) return Integer.parseInt(value);

        addResult(ERROR, INCORRECT + field + ": " + value);
        return null;
    }

    public static boolean isParsable(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    protected void checkUUIDFormat(String value, String field) {
        if (!Pattern.compile(UUID_REGEX).matcher(value).matches()){
            addResult(ERROR, INCORRECT + field + " : " + value + " formaat onjuist");
        }
    }

    protected void checkOIN(String value, String field) {
        if (!Pattern.compile(OIN_REGEX).matcher(value).matches()){
            addResult(ERROR, INCORRECT + field + " : " + value + " formaat onjuist");
        }
    }

    protected void checkUniqueEntries(int uniqueColumn) {
        duplicateEntries = entries.stream()
            .map(line -> line[uniqueColumn])
            .filter(uniqueIdentifier -> !uniqueIdentifier.isEmpty()) // ignore empty string entries as these will be caught later on while processing each line (and not be inserted into the database)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .filter(m -> m.getValue() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected abstract Map<String, Integer> getUniqueIdentifiers();

    protected abstract Logger getLogger();
}
