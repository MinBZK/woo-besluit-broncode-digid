
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

package nl.logius.digid.rda.models;

import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.rda.models.card.App;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.UUID;

@RedisHash("RdaSession")
public class RdaSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private long created;

    /**
     * This will set the expiration of the object in redis. After the object will
     * automatically be deleted. Note that save or update will not reset the
     * expiration to ttl.
     */
    @TimeToLive
    private long expiration;

    private String returnUrl;
    private String confirmId;
    private String confirmSecret;
    private String clientIpAddress;
    private App.Session app;
    private Status status;
    private Protocol procotol;

    public static RdaSession create(String returnUrl, String confirmId, String clientIpAddress, int expiration) {
        final RdaSession session = new RdaSession();
        session.id = UUID.randomUUID().toString();
        session.confirmId = confirmId;
        session.confirmSecret = Base64.toBase64String(CryptoUtils.random(40));
        session.returnUrl = returnUrl;
        session.clientIpAddress = clientIpAddress;
        session.expiration = expiration;
        session.app = new App.Session();
        session.status = Status.INITIALIZED;
        return session;
    }

    public void reset() {
        app = new App.Session(app);
        status = Status.CHALLENGE;
    }

    public RdaSession() {
        created = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfirmId() {
        return confirmId;
    }

    public void setConfirmId(String confirmId) {
        this.confirmId = confirmId;
    }

    public String getConfirmSecret() {
        return confirmSecret;
    }

    public void setConfirmSecret(String confirmSecret) {
        this.confirmSecret = confirmSecret;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    public App.Session getApp() {
        return app;
    }

    public void setApp(App.Session app) {
        this.app = app;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Protocol getProcotol() {
        return procotol;
    }

    public void setProcotol(Protocol procotol) {
        this.procotol = procotol;
    }

    public boolean isFinished() {
        return status == Status.ABORTED || status == Status.CANCELLED || status == Status.VERIFIED
                || status == Status.FAILED;
    }


}
