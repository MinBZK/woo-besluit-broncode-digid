
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

package nl.logius.digid.hsm.cryptoserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionConfig {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(\\d+)@(.+)");

    private String host;
    private int port;
    private int timeout;
    private String pwdUsername;
    private String pwdPassword;
    private String keyUsername;
    private byte[] keyFile;
    private String keyPin;

    private Path keyPath;

    public String getAddress() {
        return String.format("%d@%s", port, host);
    }

    public void setAddress(String address) {
        final Matcher matcher = ADDRESS_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Illegal address " + address);
        }
        host = matcher.group(2);
        port = Integer.parseInt(matcher.group(1));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getPwdUsername() {
        return pwdUsername;
    }

    public void setPwdUsername(String pwdUsername) {
        this.pwdUsername = pwdUsername;
    }

    public String getPwdPassword() {
        return pwdPassword;
    }

    public void setPwdPassword(String pwdPassword) {
        this.pwdPassword = pwdPassword;
    }

    public String getKeyUsername() {
        return keyUsername;
    }

    public void setKeyUsername(String keyUsername) {
        this.keyUsername = keyUsername;
    }

    public Path getKeyPath() {
        return keyPath;
    }

    public void setKeyFile(byte[] keyFile) throws IOException {
        if (keyPath == null) {
            final File file = File.createTempFile("hsm", ".key");
            keyPath = file.toPath().toAbsolutePath();
        }
        Files.write(keyPath, keyFile != null ? keyFile : new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
    }

    public String getKeyPin() {
        return keyPin;
    }

    public void setKeyPin(String keyPin) {
        this.keyPin = keyPin;
    }
}
