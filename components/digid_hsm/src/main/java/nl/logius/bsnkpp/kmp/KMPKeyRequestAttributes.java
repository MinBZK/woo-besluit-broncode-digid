
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
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;

/*-------------------------------------------
    KMPKeyRequestAttributes ::= SEQUENCE {
        schemeVersion       INTEGER,
        schemeKeySetVersion INTEGER,
        meansProvider       [0] IMPLICIT KMPParticipant OPTIONAL,    
        identityProvider    [1] IMPLICIT KMPParticipant OPTIONAL,       
        statusProvider      [2] IMPLICIT KMPParticipant OPTIONAL,
        serviceProvider     [3] IMPLICIT KMPParticipant OPTIONAL,
        signingKeyVersion   [4] IMPLICIT INTEGER OPTIONAL,
        serviceProvider     [5] IMPLICIT KMPParticipant OPTIONAL,
        diversifier         [6] IMPLICIT  SEQUENCE OF KMPKeyValue OPTIONAL,
        keyLabel            [7] IMPLICIT IA5STRING OPTIONAL,
        targetSchemeKeySetVersion [8] IMPLICIT INTEGER OPTIONAL
    }
-------------------------------------------*/
public class KMPKeyRequestAttributes {

    private static final int TAG_MEANS_PROVIDER = 0;
    private static final int TAG_IDENTITY_PROVIDER = 1;
    private static final int TAG_STATUS_PROVIDER = 2;
    private static final int TAG_SERVICE_PROVIDER = 3;
    private static final int TAG_SIGNING_KEY_VERSION = 4;
    private static final int TAG_SERVICE_PROVIDER_2 = 5;
    private static final int TAG_DIVERSIFIER = 6;
    private static final int TAG_KEY_LABEL = 7;
    private static final int TAG_TARGET_SKSV = 8;

    // Required
    private int schemeVersion;
    private int schemeKeySetVersion;

    // Optional
    private KMPParticipant meansProvider;             // Middelenuitgever
    private KMPParticipant identityProvider;          // Authenticatiedienst
    private KMPParticipant statusProvider;            // Statusdienst
    private KMPParticipant serviceProvider;           // Dienstverlener
    private KMPParticipant serviceProvider2;          // Extra dienstverlener for actions requiring 2 service provider such as key rollover
    private Integer signingKeyVersion;                // u/U key version, signing of PI/PP/PIP/DEP  
    ArrayList<KMPKeyValue> diversifier = null;
    private String keyLabel;
    private Integer targetSchemeKeySetVersion = null;

    public KMPKeyRequestAttributes(int scheme_version, int scheme_key_set_version) {
        schemeVersion = scheme_version;
        schemeKeySetVersion = scheme_key_set_version;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        // Required elements
        derSequencer.addObject(new ASN1Integer(schemeVersion));
        derSequencer.addObject(new ASN1Integer(schemeKeySetVersion));

        // Optional elements (construct as IMPLICIT
        if (getMeansProvider() != null) {

            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getMeansProvider().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_MEANS_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (getIdentityProvider() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getIdentityProvider().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_IDENTITY_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (getStatusProvider() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getStatusProvider().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_STATUS_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (getServiceProvider() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getServiceProvider().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_SERVICE_PROVIDER, asn1Stream.readObject()));
            }
        }

        if (getSigningKeyVersion() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(new ASN1Integer(getSigningKeyVersion()).getEncoded()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_SIGNING_KEY_VERSION, asn1Stream.readObject()));
            }
        }

        if (getServiceProvider2() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(getServiceProvider2().encode()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_SERVICE_PROVIDER_2, asn1Stream.readObject()));
            }
        }

        if (diversifier != null) {
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

        if (getKeyLabel() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(new DERIA5String(getKeyLabel()).getEncoded()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_KEY_LABEL, asn1Stream.readObject()));
            }
        }

        if (getTargetSchemeKeySetVersion() != null) {
            try (ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(new ASN1Integer(getTargetSchemeKeySetVersion()).getEncoded()))) {
                derSequencer.addObject(new DLTaggedObject(false, TAG_TARGET_SKSV, asn1Stream.readObject()));
            }
        }
        derSequencer.close();

        return buffer.toByteArray();
    }

    public Integer getSchemeVersion() {
        return this.schemeVersion;
    }

    public Integer getSchemeKeySetVersion() {
        return this.schemeKeySetVersion;
    }

    public KMPParticipant getMeansProvider() {
        return meansProvider;
    }

    public KMPParticipant getIdentityProvider() {
        return identityProvider;
    }

    public KMPParticipant getStatusProvider() {
        return statusProvider;
    }

    public KMPParticipant getServiceProvider() {
        return serviceProvider;
    }

    public KMPParticipant getServiceProvider2() {
        return serviceProvider2;
    }

    public Integer getSigningKeyVersion() {
        return signingKeyVersion;
    }

    public String getKeyLabel() {
        return keyLabel;
    }

    public Integer getTargetSchemeKeySetVersion() {
        return targetSchemeKeySetVersion;
    }
    
            
    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    public void setSchemeKeySetVersion(int schemeKeySetVersion) {
        this.schemeKeySetVersion = schemeKeySetVersion;
    }

    public void setMeansProvider(KMPParticipant means_provider) {
        this.meansProvider = means_provider;
    }

    public void setIdentityProvider(KMPParticipant identity_provider) {
        this.identityProvider = identity_provider;
    }

    public void setStatusProvider(KMPParticipant status_provider) {
        this.statusProvider = status_provider;
    }

    public void setServiceProvider(KMPParticipant service_provider) {
        this.serviceProvider = service_provider;
    }

    public void setServiceProvider2(KMPParticipant service_provider) {
        this.serviceProvider2 = service_provider;
    }

    public void setSigningKeyVersion(Integer version) {
        signingKeyVersion = version;
    }

    public void setDiversifier(ArrayList<KMPKeyValue> diversifier) {
        this.diversifier = diversifier;
    }

    public void setKeyLabel(String label) {
        this.keyLabel = label;
    }
    
     public void setTargetSchemeKeySetVersion(int schemeKeySetVersion) {
        this.targetSchemeKeySetVersion = schemeKeySetVersion;
    }
}
