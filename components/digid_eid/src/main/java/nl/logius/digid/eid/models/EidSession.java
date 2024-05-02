
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

package nl.logius.digid.eid.models;

import java.io.Serializable;
import java.util.UUID;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.card.crypto.EcPrivateKey;
import nl.logius.digid.sharedlib.model.ByteArray;

/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
@RedisHash("EidSession")
public class EidSession implements Serializable {
    private static final long serialVersionUID = 1;

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

    private String confirmId;
    private String confirmSecret;
    private String atReference;
    private PolymorphType userConsentType;
    private EcPrivateKey ephemeralKey;
    private ByteArray idpicc;
    private int keyReference;
    private int taVersion;
    private int paceVersion;
    private ByteArray kEnc;
    private ByteArray kMac;
    private String returnUrl;
    private String clientIpAddress;

    /**
     * EidSession constructor.
     */
    public EidSession() {
        created = System.currentTimeMillis();
    }

    public static EidSession create(String returnUrl, String confirmId, String clientIpAddress, int expiration) {
        EidSession eidSession = new EidSession();
        eidSession.id =  UUID.randomUUID().toString();
        eidSession.confirmId = confirmId;
        eidSession.confirmSecret = Base64.toBase64String(CryptoUtils.random(40));
        eidSession.returnUrl = returnUrl;
        eidSession.clientIpAddress = clientIpAddress;
        eidSession.expiration = expiration;
        return eidSession;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getAtReference() {
        return atReference;
    }

    public void setAtReference(String atReference) {
        this.atReference = atReference;
    }

    public PolymorphType getUserConsentType() {
        return userConsentType;
    }

    public void setUserConsentType(PolymorphType userConsentType) {
        this.userConsentType = userConsentType;
    }

    public EcPrivateKey getEphemeralKey() {
        return ephemeralKey;
    }

    public void setEphemeralKey(EcPrivateKey ephemeralKey) {
        this.ephemeralKey = ephemeralKey;
    }

    public ByteArray getIdpicc() {
        return idpicc;
    }

    public void setIdpicc(ByteArray idpicc) {
        this.idpicc = idpicc;
    }

    public int getKeyReference() {
        return keyReference;
    }

    public void setKeyReference(int keyReference) {
        this.keyReference = keyReference;
    }

    public int getTaVersion() {
        return taVersion;
    }

    public void setTaVersion(int taVersion) {
        this.taVersion = taVersion;
    }

    public int getPaceVersion() {
        return paceVersion;
    }

    public void setPaceVersion(int paceVersion) {
        this.paceVersion = paceVersion;
    }

    public ByteArray getkEnc() {
        return kEnc;
    }

    public void setkEnc(ByteArray kEnc) {
        this.kEnc = kEnc;
    }

    public ByteArray getkMac() {
        return kMac;
    }

    public void setkMac(ByteArray kMac) {
        this.kMac = kMac;
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
}
