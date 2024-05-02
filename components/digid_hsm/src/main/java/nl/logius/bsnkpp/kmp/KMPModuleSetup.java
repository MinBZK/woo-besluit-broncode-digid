
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
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequenceParser;

public class KMPModuleSetup {

    int m_functionFlag;
    String m_creatorOIN;
    int m_hardwareId;
    String m_supervisorId;

    public Integer getFlags() {
        return m_functionFlag;
    }

    public String getCreatorOIN() {
        return m_creatorOIN;
    }

    public KMPModuleSetup(Integer flags, String creatorOIN, int hwId, String supervisorId) {
        this.m_functionFlag = flags;
        this.m_creatorOIN = creatorOIN;
        this.m_hardwareId = hwId;
        this.m_supervisorId = supervisorId;
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DERSequenceGenerator derSequencer = new DERSequenceGenerator(buffer);

        derSequencer.addObject(new ASN1Integer(m_functionFlag));
        derSequencer.addObject(new DERIA5String(m_creatorOIN));
        derSequencer.addObject(new ASN1Integer(m_hardwareId));
        derSequencer.addObject(new DERIA5String(m_supervisorId));

        derSequencer.close();

        return buffer.toByteArray();
    }

    public static KMPModuleSetup decode(byte[] encoded) throws IOException {
        ASN1StreamParser parser = new ASN1StreamParser(encoded);
        DLSequenceParser sequenceParser = (DLSequenceParser) parser.readObject();

        int _flags = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _creatorOIN = ((DERIA5String) sequenceParser.readObject()).getString();
        int _hwId = ((ASN1Integer) sequenceParser.readObject()).getValue().intValue();
        String _supervisorId = ((DERIA5String) sequenceParser.readObject()).getString();

        return new KMPModuleSetup(_flags, _creatorOIN, _hwId, _supervisorId);
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                     KMPModuleSetup                              -");
        System.out.println("-----------------------------------------------------------------------");

        System.out.println("-----------------------------------------------------------------------");
    }
}
