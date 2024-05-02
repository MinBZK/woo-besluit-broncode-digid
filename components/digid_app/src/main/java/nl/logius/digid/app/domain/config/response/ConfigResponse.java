
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

package nl.logius.digid.app.domain.config.response;

public class ConfigResponse {

    private boolean digidAppSwitchEnabled;
    private boolean digidRdaEnabled;
    private boolean requestStationEnabled;
    private boolean ehaEnabled;
    private int letterRequestDelay = 3;
    private int maxPinChangePerDay = 1;

    public ConfigResponse() {

    }

    public ConfigResponse(boolean digidAppSwitchEnabled, boolean digidRdaEnabled, boolean requestStationEnabled, int letterRequestDelay, int maxPinChangePerDay, boolean ehaEnabled) {
        this.digidAppSwitchEnabled = digidAppSwitchEnabled;
        this.digidRdaEnabled = digidRdaEnabled;
        this.requestStationEnabled = requestStationEnabled;
        this.letterRequestDelay = letterRequestDelay;
        this.maxPinChangePerDay = maxPinChangePerDay;
        this.ehaEnabled = ehaEnabled;
    }

    public boolean isDigidAppSwitchEnabled() {
        return digidAppSwitchEnabled;
    }

    public void setDigidAppSwitchEnabled(boolean digidAppSwitchEnabled) {
        this.digidAppSwitchEnabled = digidAppSwitchEnabled;
    }

    public boolean isDigidRdaEnabled() {
        return digidRdaEnabled;
    }

    public void setDigidRdaEnabled(boolean digidRdaEnabled) {
        this.digidRdaEnabled = digidRdaEnabled;
    }

    public boolean isRequestStationEnabled() {
        return requestStationEnabled;
    }

    public void setRequestStationEnabled(boolean requestStationEnabled) {
        this.requestStationEnabled = requestStationEnabled;
    }

    public int getLetterRequestDelay() {
        return letterRequestDelay;
    }

    public void setLetterRequestDelay(int letterRequestDelay) {
        this.letterRequestDelay = letterRequestDelay;
    }

    public int getMaxPinChangePerDay() {
        return maxPinChangePerDay;
    }

    public void setMaxPinChangePerDay(int maxPinChangePerDay) {
        this.maxPinChangePerDay = maxPinChangePerDay;
    }

    public boolean isEhaEnabled() {
        return ehaEnabled;
    }

    public void setEhaEnabled(boolean ehaEnabled) {
        this.ehaEnabled = ehaEnabled;
    }
}
