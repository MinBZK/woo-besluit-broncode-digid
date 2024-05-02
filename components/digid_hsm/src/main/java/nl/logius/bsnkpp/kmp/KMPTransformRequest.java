
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;

/*------------------------------------------------------------------------
KMPTransformRequest ::= SEQUENCE {
    schemeKeySetVersion         INTEGER,
    polymorphicData             OCTET STRING,
    identityProviderOIN         IA5STRING,  // Obsolete / deprecated. Now extracted from PI/PP
    identityProviderKeyVersion  INTEGER,    // Obsolete / deprecated. Now extracted from PI/PP
    serviceProviderOIN          IA5STRING,
    serviceProviderKeyVersion   INTEGER,
    diversifier      [0]        IMPLICIT  SEQUENCE OF KMPKeyValue OPTIONAL,
    signatureTypeOID [1]        IMPLICIT  IA5STRING OPTIONAL,
    extraElements [2]           IMPLICIT  OCTET STRING OPTIONAL,
    targetSKSV [3]              IMPLICIT  INTEGER OPTIONAL,
    targetMsgVersion [4]        IMPLICIT  INTEGER OPTIONAL, // Defaults to version 1
}

------------------------------------------------------------------------*/
public class KMPTransformRequest {

    static final int TAG_DIVERSIFIER = 0;
    static final int TAG_SIG_TYPE = 1;
    static final int TAG_EXTRA_ELEMENTS = 2;
    static final int TAG_TARGET_SKSV = 3;
    static final int TAG_TARGET_MSG_VERSION = 4;

    byte[] pxxData;  // PI/PP/PIP

    Integer schemeKeyVersion;

    String identityProvider;
    int identityProviderKeySetVersion;

    String recipient;
    int recipientKeySetVersion;
    String sigTypeOID = null;
    ArrayList<KMPKeyValue> diversifier = null;
    List<KMPKeyObjectValue> extraElements;
    Integer targetSKSV = null;
    Integer targetMsgVersion = null;

    public KMPTransformRequest(byte[] pxx, int scheme_key_version, String identity_prov, int identity_prov_ksv, String recipient, int recipient_ksv) {
        this.extraElements = new ArrayList<>();
        this.diversifier = new ArrayList<>();
        this.pxxData = pxx;  // PI/PP/PIP
        this.schemeKeyVersion = scheme_key_version;
        this.identityProvider = identity_prov;
        this.identityProviderKeySetVersion = identity_prov_ksv;
        this.recipient = recipient;
        this.recipientKeySetVersion = recipient_ksv;
    }

    public KMPTransformRequest(byte[] pxx, String recipient, int recipient_ksv) {
        this.extraElements = new ArrayList<>();
        this.diversifier = new ArrayList<>();

        this.pxxData = pxx;  // PI/PP/PIP
        this.schemeKeyVersion = -1;
        this.identityProvider = "";
        this.identityProviderKeySetVersion = -1;
        this.recipient = recipient;
        this.recipientKeySetVersion = recipient_ksv;
    }

    public void addDiversifierKeyValue(String key, String value) {
        this.diversifier.add(new KMPKeyValue(key, value));
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

    public void setSignatureType(String sigType) {
        this.sigTypeOID = sigType;
    }

    public void setTargetSKSV(int sksv) {
        this.targetSKSV = sksv;
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

    public byte[] encode() throws IOException, KMPException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        /*       
        if(identityProvider==null || identityProvider.isEmpty())
            throw new KMPException("[KMPTransformRequest::encode] Empty identity provider oin ");
        if(identityProviderKeySetVersion<=0)
            throw new KMPException("[KMPTransformRequest::encode] Invalid identity provider key set version");
         */
        if (recipient == null || recipient.isEmpty()) {
            throw new KMPException("[KMPTransformRequest::encode] Empty recipient oin");
        }

        derSequencer.addObject(new ASN1Integer(schemeKeyVersion));
        derSequencer.addObject(new DEROctetString(pxxData));
        if (identityProvider == null || identityProvider.isEmpty()) {
            derSequencer.addObject(new DERIA5String("[extracted from PI]"));
        } else {
            derSequencer.addObject(new DERIA5String(identityProvider));
        }

        derSequencer.addObject(new ASN1Integer(identityProviderKeySetVersion));
        derSequencer.addObject(new DERIA5String(recipient));
        derSequencer.addObject(new ASN1Integer(recipientKeySetVersion));

        // Optional elements (construct as IMPLICIT
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

        if (sigTypeOID != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_SIG_TYPE, new DERIA5String(sigTypeOID)));
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

        if (targetSKSV != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_TARGET_SKSV, new ASN1Integer(targetSKSV)));
        }

        if (targetMsgVersion != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_TARGET_MSG_VERSION, new ASN1Integer(targetMsgVersion)));
        }

        derSequencer.close();

        return buffer.toByteArray();
    }
}
