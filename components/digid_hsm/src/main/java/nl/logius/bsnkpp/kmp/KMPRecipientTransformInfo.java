
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

import java.util.ArrayList;
import java.util.List;


/*------------------------------------------------------------------------
ExtraElements ::= SEQUENCE OF ExtraElementsKeyValuePair

VariableValueType ::= CHOICE {
    text  UTF8String,
    number INTEGER,
    binary OCTET STRING
}


ExtraElementsKeyValuePair ::= SEQUENCE {
    key IA5String,
    value VariableValueType
}


KMPRecipientTransformInfo ::= SEQUENCE {
    oin             IA5STRING,
    keyVersion      INTEGER,    // key version is same as key set version. 
    identifierType  INTEGER,    
    referenceNeeded BOOLEAN,
    extraElements       [0] IMPLICIT  OCTET STRING OPTIONAL,
    schemeKeysetVersion [1] IMPLICIT  INTEGER OPTIONAL  // If not set the SKSV is used from the provided PI/PP structure
}
------------------------------------------------------------------------*/
public class KMPRecipientTransformInfo {

    static public int FRMT_PSEUDO = 'P';
    static public int FRMT_IDENTIFIER = 'I';

    public String getOin() {
        return oin;
    }

    public void setOin(String oin) {
        this.oin = oin;
    }

    public Integer getKeySetVersion() {
        return ksv;
    }

    public void setKeySetVersion(Integer ksv) {
        this.ksv = ksv;
    }

    public int getIdentifierFormat() {
        return identifierFormat;
    }

    public void setIdentifierFormat(int identifierFormat) {
        this.identifierFormat = identifierFormat;
    }

    public boolean areLinksIncluded() {
        return linksNeeded;
    }

    public void includeLinks(boolean linksNeeded) {
        this.linksNeeded = linksNeeded;
    }

    public void addExtraElement(String key, String value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    public void addExtraElement(String key,     byte[] value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    public void addExtraElement(String key, int value) {
        this.extraElements.add(new KMPKeyObjectValue(key, value));
    }

    // public void setExtraElements(List<KMPKeyObjectValue> extraElements) {
    //     this.extraElements = extraElements;
    // }
    public List<KMPKeyObjectValue> getExtraElements() {
        return extraElements;
    }

    public void setEncryptedData(byte[] data) {
        encryptedData = data;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public Integer getTargetSKSV() {
        return schemeKeySetVersion;
    }

    public void setTargetSKSV(Integer sksv) {
        this.schemeKeySetVersion = sksv;
    }
    
    public KMPRecipientTransformInfo() {
        encryptedData = null;
        oin = null;
        ksv = null;
        identifierFormat = 0;
        linksNeeded = false;
        extraElements = new ArrayList<>();
        schemeKeySetVersion = null;   
    }

    // provide deep copy
    public KMPRecipientTransformInfo(KMPRecipientTransformInfo copy) {
        if (copy.encryptedData != null) {
            encryptedData = new byte[copy.encryptedData.length];
            System.arraycopy(copy.encryptedData, 0, this.encryptedData, 0, copy.encryptedData.length);
        }
        this.oin = copy.oin;
        this.ksv = copy.ksv;
        this.identifierFormat = copy.identifierFormat;
        this.linksNeeded = copy.linksNeeded;
        if (copy.extraElements != null) {
            this.extraElements = new ArrayList<>();
            for (int i = 0; i < copy.extraElements.size(); i++) {
                this.extraElements.add(new KMPKeyObjectValue(copy.extraElements.get(i).getKey(), copy.extraElements.get(i).getValue()));
            }
        }
        schemeKeySetVersion = copy.schemeKeySetVersion;
            
    }

    private byte[] encryptedData = null;
    private String oin;
    private Integer ksv;                            // Key version or key set version
    private int identifierFormat = FRMT_PSEUDO;     // What kind of transform is requested for this recipient? Pseudo or BSN
    private boolean linksNeeded = false;            // If true this recipient will receive the hashes of all linked multi transforms
    private List<KMPKeyObjectValue> extraElements = null;
    private Integer schemeKeySetVersion = null;     // Optional. If provided this is used for the transform
}
