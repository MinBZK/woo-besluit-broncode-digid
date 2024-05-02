
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

package nl.logius.digid.hsm.service;

import java.io.IOException;

import nl.logius.digid.hsm.crypto.keys.ListKeys;
import nl.logius.digid.hsm.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nl.logius.digid.hsm.crypto.keys.GenerateKey;
import nl.logius.digid.hsm.crypto.keys.InfoKey;
import nl.logius.digid.hsm.crypto.keys.Signing;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;

@Service
public class KeysService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionPool pool;

    public KeyInfoResponse generate(KeyRequest request) {
        logger.info("GenerateKey <{},{}>", request.getGroup(), request.getName());
        try (final GenerateKey action = new GenerateKey(pool)) {
            return action.generate(request.getGroup(), request.getName());
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the signature
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    public KeyInfoResponse info(KeyRequest request) {
        logger.info("InfoKey <{},{}>", request.getGroup(), request.getName());
        try (final InfoKey action = new InfoKey(pool)) {
            return action.info(request.getGroup(), request.getName());
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the signature
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    public KeyListResponse list(KeyListRequest request) {
        logger.info("InfoKey <{},{}>", request.getGroup());
        try (final ListKeys action = new ListKeys(pool)) {
            return action.list(request.getGroup());
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the signature
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    public byte[] sign(SignRequest request) {
        logger.info("SignRequest <{},{},{}>", request.getGroup(), request.getName(), request.getHash());
        try (final Signing action = new Signing(pool)) {
            return action.sign(request.getGroup(), request.getName(), request.getHash(),
                request.getData(), request.getType());
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the signature
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

}
