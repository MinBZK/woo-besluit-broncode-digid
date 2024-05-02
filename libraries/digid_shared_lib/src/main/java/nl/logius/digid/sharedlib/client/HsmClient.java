
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

package nl.logius.digid.sharedlib.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import nl.logius.digid.sharedlib.exception.ClientException;
import okhttp3.HttpUrl;
import org.bouncycastle.util.encoders.Base64;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public class HsmClient extends IapiClient {
    private static final String DEFAULT_HASH = "SHA384";

    private int schemeVersion = 1;
    private int schemeKeyVersion = 1;

    public HsmClient(HttpUrl baseUrl, String authToken, int timeout, ObjectMapper objectMapper) {
        super(baseUrl, authToken, timeout, objectMapper);
    }

    public HsmClient(HttpUrl baseUrl, String authToken, int timeout) {
        super(baseUrl, authToken, timeout);
    }

    public KeyInfo generateKey(String group, String name) {
        final Map<String, Object> body = ImmutableMap.of("group", group, "name", name);
        return execute("keys/generate", body, KeyInfo.class);
    }

    public KeyInfo keyInfo(String group, String name) {
        final Map<String, Object> body = ImmutableMap.of("group", group, "name", name);
        return execute("keys/info", body, KeyInfo.class);
    }

    public KeyList keyList(String group) {
        final Map<String, Object> body = ImmutableMap.of("group", group);
        return execute("keys/list", body, KeyList.class);
    }

    public byte[] sign(String group, String name, String hash, byte[] data, boolean raw) {
        final ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder()
            .put("group", group).put("name", name).put("data", data);

        if (hash != null) {
            builder.put("hash", hash);
        }
        builder.put("type", raw ? "RAW" : "ASN1");

        final ObjectNode result = (ObjectNode) execute("keys/sign", builder.build());
        return Base64.decode(result.get("signature").asText());
    }

    public byte[] sign(String group, String name, byte[] data, boolean raw) {
        return sign(group, name, DEFAULT_HASH, data, raw);
    }

    public byte[] sign(String group, String name, byte[] data) {
        return sign(group, name, DEFAULT_HASH, data, false);
    }

    public ServiceProviderKeys fetchDecryptKeys(X509Certificate certificate, int closingKeyVersion,
                                                boolean pseudonym, boolean identity) {
        final byte[] encoded;
        try {
            encoded = certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new ClientException("Could not encode certificate", e);
        }

        final Map<Object, Object> body = ImmutableMap.builder()
            .put("schemeVersion", schemeVersion)
            .put("schemeKeyVersion", schemeKeyVersion)
            .put("certificate", encoded)
            .put("closingKeyVersion", closingKeyVersion)
            .put("pseudonym", pseudonym).put("identity", identity)
            .build();

        return execute("bsnk/service-provider-keys", body, ServiceProviderKeys.class);
    }

    public VerificationPoints fetchVerificationPoints() {
        final Map<String,Object> body = ImmutableMap.of(
                "schemeVersion", schemeVersion, "schemeKeyVersion", schemeKeyVersion
        );

        return execute("bsnk/verification-points", body, VerificationPoints.class);
    }

    public int getSchemeVersion() {
        return schemeVersion;
    }

    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    public int getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public void setSchemeKeyVersion(int schemeKeyVersion) {
        this.schemeKeyVersion = schemeKeyVersion;
    }

    public static class KeyInfo {
        private byte[] publicKey;

        public byte[] getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(byte[] publicKey) {
            this.publicKey = publicKey;
        }
    }

    public static class ServiceProviderKeys {
        private byte[] identity;
        private byte[] pseudonymDecrypt;
        private byte[] pseudonymClosing;
        private byte[] directPseudonymDecrypt;

        @JsonProperty("ID")
        public byte[] getIdentity() {
            return identity;
        }

        public void setIdentity(byte[] identity) {
            this.identity = identity;
        }

        @JsonProperty("PD")
        public byte[] getPseudonymDecrypt() {
            return pseudonymDecrypt;
        }

        public void setPseudonymDecrypt(byte[] pseudonymDecrypt) {
            this.pseudonymDecrypt = pseudonymDecrypt;
        }

        @JsonProperty("PC")
        public byte[] getPseudonymClosing() {
            return pseudonymClosing;
        }

        public void setPseudonymClosing(byte[] pseudonymClosing) {
            this.pseudonymClosing = pseudonymClosing;
        }

        @JsonProperty("DRKi")
        public byte[] getDirectPseudonymDecrypt() {
            return directPseudonymDecrypt;
        }

        public void setDirectPseudonymDecrypt(byte[] directPseudonymDecrypt) {
            this.directPseudonymDecrypt = directPseudonymDecrypt;
        }
    }

    public static class KeyList {
        private List<String> keys;

        public List<String> getKeys() {
            return keys;
        }

        public void setKeys(List<String> keys) {
            this.keys = keys;
        }
    }

    public static class VerificationPoints {
        private String identity;
        private String pseudonym;

        @JsonProperty("IPp")
        public String getIdentity() {
            return identity;
        }

        public void setIdentity(String identity) {
            this.identity = identity;
        }

        @JsonProperty("PPp")
        public String getPseudonym() {
            return pseudonym;
        }

        public void setPseudonym(String pseudonym) {
            this.pseudonym = pseudonym;
        }
    }
}
