
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
KMPRecipientTransformInfo ::= SEQUENCE {
    oin             IA5STRING,
    keyVersion      INTEGER,
    identifierType  INTEGER,    
    referenceNeeded BOOLEAN,
    extraElements       [0] IMPLICIT  OCTET STRING OPTIONAL,
    schemeKeysetVersion [1] IMPLICIT  INTEGER OPTIONAL  // If not set the SKSV is used from the provided PI/PP structure
}

KMPMultiTransformRequest ::= SEQUENCE {
    
    recipients              SEQUENCE OF KMPTransformRecipientInfo,
    unsignedPI       [0]    IMPLICIT  OCTET STRING,   
    unsignedPP       [1]    IMPLICIT  OCTET STRING,
    diversifier      [2]    IMPLICIT  SEQUENCE OF KMPKeyValue OPTIONAL,
}

------------------------------------------------------------------------*/
public class KMPMultiTransformRequest {

    static final int TAG_PI = 0;
    static final int TAG_PP = 1;
    static final int TAG_DIVERSIFIER = 2;
    static final int TAG_TARGET_MSG_VERSION = 4;
    
    // Tags within the recipient info
    static final int TAG_EXTRA_ELEMENTS = 0;
    static final int TAG_SCHEME_KEY_SET_VERSION = 1;
        
    byte[] unsignedPI;
    byte[] unsignedPP;
    List<KMPRecipientTransformInfo> recipients;
    ArrayList<KMPKeyValue> diversifier = null;
    Integer targetMsgVersion = null;
    
   
    public KMPMultiTransformRequest(byte[] unsignedPI, byte[] unsignedPP, List<KMPRecipientTransformInfo> recipients) {
        this.diversifier = new ArrayList<>();
        
        this.unsignedPI = unsignedPI;
        this.unsignedPP = unsignedPP;

        this.recipients = recipients;
        this.targetMsgVersion = null;
    }
    
    public void addDiversifierKeyValue(String key, String value) {
        this.diversifier.add(new KMPKeyValue(key, value));
    }
    
    public void setTargetMessageVersion(int msg_version) {
        this.targetMsgVersion = msg_version;
    }
     
    public int getTargetMessageVersion() {
        if(this.targetMsgVersion == null)
            return -1;
        return this.targetMsgVersion;
    }
      
     public List<KMPRecipientTransformInfo> getRecipients() {
         return this.recipients;
     }
             
    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        ASN1EncodableVector v_recipient = new ASN1EncodableVector();

        for (int i = 0; i < recipients.size(); i++) {
            KMPRecipientTransformInfo ti = recipients.get(i);
            // FWI: Encoding should be moved to KMPRecipientTransformInfo class
            ASN1EncodableVector v_ti = new ASN1EncodableVector();
            v_ti.add(new DERIA5String(ti.getOin()));
            v_ti.add(new ASN1Integer(ti.getKeySetVersion()));
            v_ti.add(new ASN1Integer(ti.getIdentifierFormat()));
            v_ti.add(new ASN1Integer(ti.areLinksIncluded() ? 1 : 0));

            if (ti.getExtraElements() != null && !ti.getExtraElements().isEmpty()) {
                ASN1EncodableVector v = new ASN1EncodableVector();
                for (int j = 0; j < ti.getExtraElements().size(); j++) {
                    KMPKeyObjectValue ov = ti.getExtraElements().get(j);
                    ASN1EncodableVector v_ov = new ASN1EncodableVector();

                    v_ov.add(new DERIA5String(ov.key));

                    if (ov.value instanceof Integer) {
                        v_ov.add(new ASN1Integer((Integer) ov.value));
                    } else if (ov.value instanceof String) {
                        v_ov.add(new DERUTF8String((String) ov.value));
                    } else if (ov.value instanceof byte[]) {
                        v_ov.add(new DEROctetString((byte[]) ov.value));
                    }
                    v.add(new DLSequence(v_ov));
                }
                DLSequence extraelements_sequence = new DLSequence(v);
                v_ti.add(new DLTaggedObject(false, TAG_EXTRA_ELEMENTS, extraelements_sequence));
            }

            if (ti.getTargetSKSV() != null) {
                v_ti.add(new DLTaggedObject(false, TAG_SCHEME_KEY_SET_VERSION, new ASN1Integer(ti.getTargetSKSV())));
            }
                    
            v_recipient.add(new DLSequence(v_ti));
        }

        DLSequence recipients_sequence = new DLSequence(v_recipient);
        derSequencer.addObject(recipients_sequence);

        if (unsignedPI != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_PI, new DEROctetString(unsignedPI)));
        }
        if (unsignedPP != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_PP, new DEROctetString(unsignedPP)));
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

        if (targetMsgVersion != null) {
            derSequencer.addObject(new DLTaggedObject(false, TAG_TARGET_MSG_VERSION, new ASN1Integer(targetMsgVersion)));
        }
       
        derSequencer.close();

        return buffer.toByteArray();
    }
}
