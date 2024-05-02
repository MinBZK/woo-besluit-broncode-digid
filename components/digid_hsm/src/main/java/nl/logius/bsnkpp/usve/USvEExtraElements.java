
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

package nl.logius.bsnkpp.usve;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;

/*------------------------------------------------------------------------
ExtraElements ::= SEQUENCE OF ExtraElementsKeyValuePair

ExtraElementsKeyValuePair ::= SEQUENCE {
    key IA5String,
    value VariableValueType
}

VariableValueType ::= CHOICE {
    text  UTF8String,
    number INTEGER,
    binary OCTET STRING
}
------------------------------------------------------------------------*/
public class USvEExtraElements {
    static final int TAG_EXTRA_ELEMENTS = 2;
    ArrayList<USvEExtraElementsKeyValuePair> elements = new ArrayList<USvEExtraElementsKeyValuePair>();

    public ArrayList<USvEExtraElementsKeyValuePair> getElements() {
        return elements;
    }

    public void addElement(String key, Object value) {
        elements.add(new USvEExtraElementsKeyValuePair(key, value));
    }

    public USvEExtraElementsKeyValuePair findForKey(String key) {
        if(elements==null || elements.isEmpty())
            return null;
        for(int i=0; i<elements.size(); i++) {
            if(elements.get(i).key.equals(key))
                return elements.get(i);
        }
        return null;
    }
    
    public List<USvEExtraElementsKeyValuePair> findAll(String key) {
        if(elements==null || elements.isEmpty())
            return null;
        
        ArrayList<USvEExtraElementsKeyValuePair> same_elts = new ArrayList<>();
        
        for(int i=0; i<elements.size(); i++) {
            if(elements.get(i).key.equals(key))
                same_elts.add(elements.get(i));
        }
        return same_elts;
    }
    
    public byte[] encode() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i = 0; i < elements.size(); i++) {
            USvEExtraElementsKeyValuePair kvp = elements.get(i);

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

        derSequencer.close();

        return buffer.toByteArray();
    }

    private static void addExtraElement(USvEExtraElements ee, ASN1Encodable key, ASN1Encodable value) throws Exception {
        if (value instanceof ASN1Integer) {
            ee.addElement(key.toString(), ((ASN1Integer) value).getValue().intValue());
        } else if (value instanceof DERUTF8String) {
            ee.addElement(key.toString(), ((DERUTF8String) value).getString());
        } else if (value instanceof DEROctetString) {
            ee.addElement(key.toString(), ((DEROctetString) value).getOctets());
        } else {
            throw new Exception("USvESignedEIPayload::decode] Unsupported element in extraElements");
        }
    }

    public static USvEExtraElements decode(byte[] encoded) throws Exception {
        ASN1InputStream bIn = new ASN1InputStream(encoded);

        DLTaggedObject dto = (DLTaggedObject) (bIn.readObject());

        USvEExtraElements ee = new USvEExtraElements();

        ASN1Encodable[] extra_elements = ((DLSequence) dto.getObject()).toArray();

        for (ASN1Encodable extra_element : extra_elements) {
            // 1 element is encoded/interpreted as DERSequence
            // Multiple elements as DLSequence
            if (extra_element instanceof DLSequence) {
                ASN1Encodable[] kv_pair = ((DLSequence) extra_element).toArray();
                addExtraElement(ee, kv_pair[0], kv_pair[1]);
            } else if (extra_elements.length==2) {
                addExtraElement(ee, extra_elements[0], extra_elements[1]);
                break;
            }
            else throw new Exception("[USvEExtraElements::decode] unexpected structure");
        }
        return ee;
    }
}
