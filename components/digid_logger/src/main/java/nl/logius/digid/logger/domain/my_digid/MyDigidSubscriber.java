
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

package nl.logius.digid.logger.domain.my_digid;

import nl.logius.digid.logger.model.LogRequest;
import nl.logius.digid.logger.subscribers.BaseSubscriber;
import org.springframework.beans.factory.annotation.Autowired;

public class MyDigidSubscriber<T> extends BaseSubscriber<T> {

    @Autowired
    private MyDigidLogRepository repository;

    public MyDigidSubscriber(String name) {
        super(name);
    }

    @Override
    public void onNext(T msg) {
        if (!isIncluded((LogRequest) msg)){
            subscription.request(1);
            return;
        }

        try {
            logger.info(name + ": " + msg.toString() + " received in onNext");
            MyDigidLog log = transformLog((LogRequest) msg);
            repository.saveAndFlush(log);
        }
        catch(Exception e) {
            logger.error(name + ": Error: " + e.getMessage());
        }
        finally {
            subscription.request(1);
        }
   }

    private MyDigidLog transformLog(LogRequest logRequest) {
        MyDigidLog log = new MyDigidLog(
            logRequest.getNr(),
            logRequest.getIpAddress(),
            logRequest.getSessionId(),
            logRequest.getTransactionId(),
            logRequest.getData(),
            logRequest.getCreatedAt()
        );

        MyDigidLogAccount logAccount = new MyDigidLogAccount(logRequest.getAccountId());
        logAccount.setMyDigidLog(log);
        log.setMyDigidLogAccount(logAccount);

        return log;
    }
}
