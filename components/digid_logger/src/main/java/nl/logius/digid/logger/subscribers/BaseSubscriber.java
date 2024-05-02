
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

package nl.logius.digid.logger.subscribers;

import nl.logius.digid.logger.model.LogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Flow;

public abstract class BaseSubscriber<T> implements Flow.Subscriber<T> {
   protected Flow.Subscription subscription;
   protected String name;
   protected final Logger logger = LoggerFactory.getLogger(getClass());

   public BaseSubscriber(String name) {
      this.name = name;
   }

   @Override
   public void onComplete() {
      logger.debug(name + ": onComplete");
   }

   @Override
   public void onError(Throwable t) {
      logger.error(name + ": Error: " + t.getCause().getMessage());
   }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
      logger.info(name + ": onSubscribe");

      this.subscription = subscription;
      subscription.request(1);
   }

   protected boolean isIncluded(LogRequest log) {
       return log.getCategories().contains(name);
   }
}
