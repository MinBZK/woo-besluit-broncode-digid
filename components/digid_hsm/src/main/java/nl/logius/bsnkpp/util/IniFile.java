
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

package nl.logius.bsnkpp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniFile {

    private final Pattern _section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
    private final Pattern _keyValue = Pattern.compile("\\s*([^=]*)=(.*)");

    private ArrayList<String> lines = new ArrayList<>();

    private final LinkedHashMap< String, LinkedHashMap< String, String>> _entries = new LinkedHashMap<>();

    private String path_name = null;

    public IniFile(String path) throws IOException {
        path_name = path;
        load(path);
    }

    public void load(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        parse();
    }

    public void save() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path_name))) {
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                bw.write("\r\n");
            }
        }

        parse();
    }

    void parse() {
        _entries.clear();

        String section = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Skip empty lines
            if (line.trim().length() == 0) {
                continue;
            }

            // Skip comments
            if (line.trim().charAt(0) == '#') {
                continue;
            }

            Matcher m = _section.matcher(line);
            if (m.matches()) {
                section = m.group(1).trim();
            } else if (section != null) {
                m = _keyValue.matcher(line);
                if (m.matches()) {
                    String key = m.group(1).trim();
                    String value = m.group(2).trim();
                    LinkedHashMap< String, String> kv = _entries.get(section);
                    if (kv == null) {
                        _entries.put(section, kv = new LinkedHashMap<>());
                    }
                    kv.put(key, value);
                }
            }
        }
    }

    public void addEntry(String target_section, String key, String value) throws IOException {
        ArrayList<String> updated_ini = new ArrayList<>();
        int state = 0;

        for (int i = 0; i < lines.size(); i++) {
            Matcher m = _section.matcher(lines.get(i));
            if (m.matches()) {
                switch (state) {
                    case 0: {
                        String section = m.group(1).trim();
                        if (section.equalsIgnoreCase(target_section) == true) {
                            state = 1;    // Found target section
                        }
                    }
                    break;
                    case 1: // Section after target section
                    {
                        updated_ini.add(key + "=" + value);
                        state = 2;  // added
                    }
                }
            }

            updated_ini.add(lines.get(i));
        }

        if (state == 1) {
            // target section found but not a next section. Just add to end of file
            updated_ini.add(key + "=" + value);
        } else if (state == 0) {
            updated_ini.add(target_section);
            updated_ini.add(key + "=" + value);
        }

        lines = updated_ini;

        save();
        parse();
    }

    public void deleteEntry(String target_section, String key, String value) throws IOException {
        ArrayList<String> updated_ini = new ArrayList<>();
        int state = 0;

        for (int i = 0; i < lines.size(); i++) {
            Matcher m = _section.matcher(lines.get(i));
            if (m.matches()) {
                switch (state) {
                    case 0: {
                        String section = m.group(1).trim();
                        if (section.equalsIgnoreCase(target_section) == true) {
                            // Found target section

                            state = 1;
                        }
                    }
                    break;
                    case 1: // Section after target section
                    {
                        state = 2;
                        break;
                    }
                }
            } else if (state == 1) {
                m = _keyValue.matcher(lines.get(i));
                if (m.matches()) {
                    String key_src = m.group(1).trim();
                    String value_src = m.group(2).trim();
                    
                    if (key_src.equalsIgnoreCase(key) && value_src.equalsIgnoreCase(value)) {
                        continue;   // do not add to new list
                    }
                }
            }

            updated_ini.add(lines.get(i));
        }

        lines = updated_ini;

        save();
        parse();
    }

    public String getString(String section, String key, String defaultvalue) {
        Map< String, String> kv = _entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return kv.get(key);
    }

    public int getInt(String section, String key, int defaultvalue) {
        Map< String, String> kv = _entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Integer.parseInt(kv.get(key));
    }

    public float getFloat(String section, String key, float defaultvalue) {
        Map< String, String> kv = _entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Float.parseFloat(kv.get(key));
    }

    public double getDouble(String section, String key, double defaultvalue) {
        Map< String, String> kv = _entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Double.parseDouble(kv.get(key));
    }

    public Map getSection(String section) {
        return _entries.get(section);
    }
}
