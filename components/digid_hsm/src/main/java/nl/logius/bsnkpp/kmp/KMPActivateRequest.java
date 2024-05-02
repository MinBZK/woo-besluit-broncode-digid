
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

package nl.logius.bsnkpp.kmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;


/*-------------------------------------------------
KMPActivateRequest ::= SEQUENCE {
    schemeKeySetVersion            INTEGER,
    identity                       A5String,
    identityType                   INTEGER,
    signingKeyVersion              INTEGER,
    identityProvider  [0] IMPLICIT KMPParticipant OPTIONAL,
    statusProvider    [1] IMPLICIT KMPParticipant OPTIONAL,
    meansProvider     [2] IMPLICIT KMPParticipant OPTIONAL,
    authorizedParty   [3] IMPLICIT KMPParticipant OPTIONAL,
    diversifier       [4] IMPLICIT SEQUENCE OF KMPKeyValue OPTIONAL,
    extraElements     [5]           IMPLICIT  OCTET STRING OPTIONAL
}
-------------------------------------------------*/
public class KMPActivateRequest {

    static private final int TAG_IDENTITY_PROVIDER = 0;
    static private final int TAG_STATUS_PROVIDER = 1;
    static private final int TAG_MEANS_PROVIDER = 2;
    static private final int TAG_AUTHORIZED_PARTY = 3;
    static private final int TAG_DIVERSIFIER = 4;
    static private final int TAG_EXTRA_ELEMENTS = 5;
    static private final int TAG_TARGET_MSG_VERSION = 6;

    Integer schemeKeyVersion;
    String identity;
    Integer identityType;
    int signingKeyVersion;
    KMPParticipant identityProvider;
    KMPParticipant statusProvider;
    KMPParticipant meansProvider;
    KMPParticipant authorizedParty;
    ArrayList<KMPKeyValue> diversifier = null;
    List<KMPKeyObjectValue> extraElements = null;
    Integer targetMsgVersion = null;

    public Integer getSchemeKeyVersion() {
        return schemeKeyVersion;
    }

    public String getIdentity() {
        return identity;
    }

    public Integer getIdentityType() {
        return identityType;
    }

    public String getIdentityProviderOIN() {
        if (identityProvider == null) {
            return null;
        }
        return identityProvider.getOIN();

    }

    public Integer getIdentityProviderKeySetVersion() {
        if (identityProvider == null) {
            return null;
        }
        return identityProvider.getKeySetVersion();
    }

    public String getStatusProviderOIN() {
        if (statusProvider == null) {
            return null;
        }
        return statusProvider.getOIN();
    }

    public Integer getStatusProviderKeySetVersion() {
        if (statusProvider == null) {
            return null;
        }
        return statusProvider.getKeySetVersion();
    }

    public KMPActivateRequest(String identity, int identityType, int scheme_key_version, int sig_key_version) {
        this.extraElements = new ArrayList<>();
        this.diversifier = new ArrayList<>();

        this.identity = identity;
        this.identityType = identityType;
        this.schemeKeyVersion = scheme_key_version;
        this.signingKeyVersion = sig_key_version;
        this.identityProvider = null;
        this.statusProvider = null;
        this.meansProvider = null;
        this.authorizedParty = null;
    }

    public void setIdentityProvider(String oin, int ksv) {
        this.identityProvider = new KMPParticipant(oin, ksv);
    }

    public void setStatusProvider(String oin, int ksv) {
        this.statusProvider = new KMPParticipant(oin, ksv);
    }

    // Authorized party does not have KSV
    public void setAuthorizedParty(String oin) {
        this.authorizedParty = new KMPParticipant(oin);
    }

    // Activator (BSNk) does not have KSV
    // (Used for DEP)
    public void setActivator(String oin) {
        this.meansProvider = new KMPParticipant(oin);
    }

    public String getActivatorOIN() {
        if (meansProvider == null) {
            return null;
        }
        return meansProvider.getOIN();
    }

    @Deprecated
    public void setDiversifier(ArrayList<KMPKeyValue> diversifier) {
        this.diversifier = diversifier;
    }

    public void addDiversifierKeyValue(String key, String value) {
        this.diversifier.add(new KMPKeyValue(key, value));
    }

    public ArrayList<KMPKeyValue> getDiversifier() {
        if(diversifier==null || diversifier.isEmpty())
            return null;
        Collections.sort(diversifier);
        return diversifier;
    }
     
    @Deprecated
    public void setExtraElements(List<KMPKeyObjectValue> extraElements) {
        this.extraElements = extraElements;
    }

    public void addExtraElement(String key, String value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    public void addExtraElement(String key, byte[] value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    public void addExtraElement(String key, int value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    public void setTargetMessageVersion(int msg_version) {
        this.targetMsgVersion = msg_version;
    }

    public int getTargetMessageVersion() {
        if (this.targetMsgVersion == null) {
            return -1;
        }
        return this.targetMsgVersion;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1Integer(schemeKeyVersion));
        derSequencer.addObject(new DERIA5String(identity));
        derSequencer.addObject(new ASN1Integer(identityType));
        derSequencer.addObject(new ASN1Integer(signingKeyVersion));

        // Optional elements (construct as IMPLICIT
        if (identityProvider != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(identityProvider.encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_IDENTITY_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (statusProvider != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(statusProvider.encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_STATUS_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (meansProvider != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(meansProvider.encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_MEANS_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (authorizedParty != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(authorizedParty.encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_AUTHORIZED_PARTY, asn1Stream.readObject()));
            }
        }

        if (diversifier != null && !diversifier.isEmpty()) {
            // The USvE specs require a sorted diversifier    
            Collections.sort(diversifier);
            ASN1EncodableVector v = new ASN1EncodableVector();
            for (int i = 0; i < diversifier.size(); i++) {
                KMPKeyValue kv = diversifier.get(i);
                ASN1EncodableVector v_kv = new ASN1EncodableVector();
                v_kv.add(new DERIA5String(kv.key));
                v_kv.add(new DERIA5String(kv.value));
                v.add(new DLSequence(v_kv));
            }
            DLSequence diversifier_sequence = new DLSequence(v);
            derSequencer.addObject(new DLTaggedObject(false, TAG_DIVERSIFIER, diversifier_sequence));
        }
        // Optional elements (construct as IMPLICIT)
        if (extraElements != null && !extraElements.isEmpty()) {
            ASN1EncodableVector v = new ASN1EncodableVector();
            for (int i = 0; i < extraElements.size(); i++) {
                KMPKeyObjectValue kvp = extraElements.get(i);

                ASN1EncodableVector v_kv = new ASN1EncodableVector();
                v_kv.add(new DERIA5String(kvp.key));

                if (kvp.value instanceof Integer) {
                    v_kv.add(new ASN1Integer((Integer) kvp.value));
                } else if (kvp.value instanceof String) {
                    v_kv.add(new DERUTF8String((String) kvp.value));
                } else if (kvp.value instanceof byte[]) {
                    v_kv.add(new DEROctetString((byte[]) kvp.value));
                }
                v.add(new DLSequence(v_kv));
            }

            DLSequence elements_sequence = new DLSequence(v);
            derSequencer.addObject(new DLTaggedObject(false, TAG_EXTRA_ELEMENTS, elements_sequence));
        }

        if (targetMsgVersion != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_TARGET_MSG_VERSION, new ASN1Integer(targetMsgVersion)));
        }

        derSequencer.close();

        return buffer.toByteArray();
    }
}
