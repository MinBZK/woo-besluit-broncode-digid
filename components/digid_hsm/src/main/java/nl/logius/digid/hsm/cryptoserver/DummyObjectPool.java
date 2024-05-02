
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

import org.apache.commons.pool2.KeyedObjectPool;

public class DummyObjectPool implements KeyedObjectPool<String, Connection> {
    private final ConnectionFactory factory;

    public DummyObjectPool(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Connection borrowObject(String key) throws Exception {
        return factory.create(key);
    }

    @Override
    public void returnObject(String key, Connection conn) throws Exception {
        conn.close();
    }

    @Override
    public void invalidateObject(String key, Connection conn) throws Exception {
        conn.close();
    }

    @Override
    public void addObject(String key) {
    }

    @Override
    public int getNumIdle(String key) {
        return 0;
    }

    @Override
    public int getNumActive(String key) {
        return 0;
    }

    @Override
    public int getNumIdle() {
        return 0;
    }

    @Override
    public int getNumActive() {
        return 0;
    }

    @Override
    public void clear()  {
    }

    @Override
    public void clear(String key) {
    }

    @Override
    public void close() {
    }
}
