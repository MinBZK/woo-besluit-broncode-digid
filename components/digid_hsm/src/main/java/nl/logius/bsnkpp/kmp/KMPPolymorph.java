
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

import java.io.IOException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DLSequence;

public class KMPPolymorph {

    static private final int TAG_PI = 0;
    static private final int TAG_PP = 1;
    static private final int TAG_PIP = 2;
    static private final int TAG_DEP = 3;
    static private final int TAG_VPIP = 4;

    private byte[] m_pi_encoded = null;
    private byte[] m_pp_encoded = null;
    private byte[] m_pip_encoded = null;
    private byte[] m_dep_encoded = null;
    private byte[] m_vpip_encoded = null;

    public byte[] getEncodedPolymorphicIdentity() {
        return m_pi_encoded;
    }

    public byte[] getEncodedPolymorphicPseudonym() {

        return m_pp_encoded;
    }

    public byte[] getEncodedPIP() {
        return m_pip_encoded;
    }

    public byte[] getEncodedDEP() {
        return m_dep_encoded;
    }

    public byte[] getEncodedVerifiablePIP() {
        return m_vpip_encoded;
    }

    public static KMPPolymorph decode(byte[] encoded) throws IOException {
        KMPPolymorph poly = new KMPPolymorph();

        ASN1InputStream parser = new ASN1InputStream(encoded);

        DLSequence seq = (DLSequence) parser.readObject();

        byte[] asn1 = null;

        for (int i = 0; i < seq.size(); i++) {
            ASN1TaggedObject to = (ASN1TaggedObject) seq.getObjectAt(i);

            switch (to.getTagNo()) {
                case TAG_PI:
                    asn1 = ((DLSequence) to.getObject()).getEncoded();
                    poly.m_pi_encoded = asn1;
                    break;
                case TAG_PP:
                    asn1 = ((DLSequence) to.getObject()).getEncoded();
                    poly.m_pp_encoded = asn1;
                    break;
                case TAG_PIP:
                    asn1 = ((DLSequence) to.getObject()).getEncoded();
                    poly.m_pip_encoded = asn1;
                    break;
                case TAG_DEP:
                    asn1 = ((DLSequence) to.getObject()).getEncoded();
                    poly.m_dep_encoded = asn1;
                    break;
                case TAG_VPIP:
                    asn1 = ((DLSequence) to.getObject()).getEncoded();
                    poly.m_vpip_encoded = asn1;
                    break;
            }
        }

        return poly;
    }
}
