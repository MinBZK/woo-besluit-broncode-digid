
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
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import nl.logius.bsnkpp.crypto.BrainpoolP320r1;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.math.ec.ECPoint;

/*
MigrationIntermediaryPseudonym ::= SEQUENCE {
    notationIdentifier     OBJECT IDENTIFIER (id-BSNk-decrypted-migrationpseudonym),
    schemeVersion          INTEGER,
    schemeKeySetVersion    INTEGER,
    source                 IA5String,
    sourceKeySetVersion    INTEGER,
    target                 IA5String,
    targetKeySetVersion    INTEGER,
    migrationID            IA5String,
    type                   INTEGER,
    pseudonymValue         IA5String,
    diversifier       [0]  Diversifier OPTIONAL
}
 */
public class USvEMigrationIntermediaryPseudonym {

    private static final int TAG_DIVERSIFIER = 0;

    private final String notationIdentifier;
    private final int schemeVersion;
    private final int schemeKeysetVersion;
    private final String migrantSourceOin;
    private final int migrantSourceKeysetVersion;
    private final String migrantTargetOin;
    private final int migrantTargetKeysetVersion;
    private final String migrationID;
    private final int type;
    private final String pseudoValue;
    private final USvEDiversifier diversifier;

    public USvEMigrationIntermediaryPseudonym(String notationIdentifier, int schemeVersion, int schemeKeysetVersion, String migrantSourceOin, int migrantSourceKeysetVersion, String migrantTargetOin, int migrantTargetKeysetVersion, String migrationID, USvEDiversifier diversifier, char type, String pseudoValue) {
        this.notationIdentifier = USvEConstants.OID_BSNK_MIGRATION_INTERMEDIATE_P;
        this.schemeVersion = schemeVersion;
        this.schemeKeysetVersion = schemeKeysetVersion;
        this.migrantSourceOin = migrantSourceOin;
        this.migrantSourceKeysetVersion = migrantSourceKeysetVersion;
        this.migrantTargetOin = migrantTargetOin;
        this.migrantTargetKeysetVersion = migrantTargetKeysetVersion;
        this.type = type;
        this.diversifier = diversifier;
        this.pseudoValue = pseudoValue;
        this. migrationID = migrationID;
    }

    public USvEMigrationIntermediaryPseudonym(int schemeKeysetVersion, String migrantSourceOin, int migrantSourceKeysetVersion, String migrantTargetOin, int migrantTargetKeysetVersion, String migrationID, USvEDiversifier diversifier, char type, String pseudoValue) {
        this(USvEConstants.OID_BSNK_MIGRATION_INTERMEDIATE_P, 1, schemeKeysetVersion, migrantSourceOin, migrantSourceKeysetVersion, migrantTargetOin, migrantTargetKeysetVersion, migrationID, diversifier, type, pseudoValue);
    }
    
    public int getSchemeVersion() {
        return schemeVersion;
    }

    public int getSchemeKeysetVersion() {
        return schemeKeysetVersion;
    }

    public String getMigrantSourceOin() {
        return migrantSourceOin;
    }

    public int getMigrantSourceKeysetVersion() {
        return migrantSourceKeysetVersion;
    }

    public String getMigrantTargetOin() {
        return migrantTargetOin;
    }

    public int getMigrantTargetKeysetVersion() {
        return migrantTargetKeysetVersion;
    }

 public String getMigrationId() {
        return migrationID;
    }
    
    public String getPseudonymValue() {
        return pseudoValue;
    }

    public int getType() {
        return type;
    }

    public String getDiversifier() {
        return pseudoValue;
    }

    public ECPoint getPseudonymPoint() {
        byte[] pseudonymBytes = Base64.getDecoder().decode(pseudoValue);
        return BrainpoolP320r1.CURVE.decodePoint(pseudonymBytes);
    }
    
    
    public byte[] encode() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1ObjectIdentifier(notationIdentifier));
        derSequencer.addObject(new ASN1Integer(schemeVersion));
        derSequencer.addObject(new ASN1Integer(schemeKeysetVersion));
        derSequencer.addObject(new DERIA5String(migrantSourceOin));
        derSequencer.addObject(new ASN1Integer(migrantSourceKeysetVersion));
        derSequencer.addObject(new DERIA5String(migrantTargetOin));
        derSequencer.addObject(new ASN1Integer(migrantTargetKeysetVersion));
         derSequencer.addObject(new DERIA5String(migrationID));
        derSequencer.addObject(new ASN1Integer(type));
        derSequencer.addObject(new DERIA5String(pseudoValue));

        if (diversifier != null && diversifier.diversifierValues.size() != 0) {
            Collections.sort(diversifier.diversifierValues);
            ASN1EncodableVector v = new ASN1EncodableVector();
            for (int i = 0; i < diversifier.diversifierValues.size(); i++) {
                USvEDiversifierKeyValuePair kv = diversifier.diversifierValues.get(i);
                ASN1EncodableVector v_kv = new ASN1EncodableVector();
                v_kv.add(new DERIA5String(kv.key));
                v_kv.add(new DERIA5String(kv.value));
                v.add(new DLSequence(v_kv));
            }
            DLSequence diversifier_sequence = new DLSequence(v);
            derSequencer.addObject(new DLTaggedObject(false, TAG_DIVERSIFIER, diversifier_sequence));
        }

        derSequencer.close();

        return buffer.toByteArray();
    }

    public static USvEMigrationIntermediaryPseudonym decode(byte[] encoded) throws Exception {
       throw new IOException("USvEMigrationIntermediaryPseudonym::decode] not implemented");
    }

    public void validate() throws Exception {
        if (!notationIdentifier.equalsIgnoreCase(USvEConstants.OID_BSNK_MIGRATION_INTERMEDIATE_P)) {
            throw new Exception("[USvEPseudonym::validate] Invalid OID (" + notationIdentifier + ")");
        }

        if (schemeVersion != 1) {
            throw new Exception("[USvEPseudonym::validate] Invalid scheme version (" + schemeVersion + ")");
        }

        if (type != 'B' && type != 'E') {
            throw new Exception("[USvEPseudonym::validate] Invalid type (" + type + ")");
        }
    }

}
