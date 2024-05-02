
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

package nl.logius.digid.logger.domain.transaction_log;

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.logius.digid.logger.model.LogBase;
import nl.logius.digid.logger.utils.Constants;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@Entity
@Table(name = "transaction_logs")
public class TransactionLog extends LogBase {
    private static final Pattern PATTERN = Pattern.compile("\\$([a-zA-Z_]+)");

    @OneToOne(mappedBy = "transactionLog", cascade = CascadeType.ALL)
    private TransactionLogAccount transactionLogAccount;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public TransactionLog(){}

    public TransactionLog(Integer nr, String ipAddress, String sessionId, String transactionId, Map<String, String> data, ZonedDateTime createdAt) {
        this.nr = nr;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.transactionId = transactionId;
        this.data = data;
        this.createdAt = createdAt;
    }

    @JsonProperty
    public Long getId() {
        return id;
    }

    public String getText() {
        Map<String, Object> log = (Map<String, Object> ) Constants.LOG_MAP.get(nr);
        return replaceVariables((String) log.get("text"), data);
    }

    public TransactionLogAccount getTransactionLogAccount() {
        return transactionLogAccount;
    }

    public void setTransactionLogAccount(TransactionLogAccount transactionLogAccount) {
        this.transactionLogAccount = transactionLogAccount;
    }

    private static String replaceVariables(final CharSequence s, final Map<? super String, String> variables) {
        return PATTERN.matcher(s).replaceAll(mr -> variables.getOrDefault(mr.group(1), ""));
    }
}
