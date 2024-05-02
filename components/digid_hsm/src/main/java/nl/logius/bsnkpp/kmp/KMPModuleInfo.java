
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
import java.util.ArrayList;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;

/*-------------------------------------------------
KMPModuleInfo ::= SEQUENCE {
    oin             IA5String,
    installatieID   INTEGER,
    counter         INTEGER,
    extraInfo       IA5String,         
    memInUseSD      INTEGER,
    memInUseSecure  INTEGER,
    componentsList  SEQUENCE OF KMPModuleInfo, // 3x (kma, activate, transform
  
    hsmHardwareVersion   [0] IMPLICIT INTEGER OPTIONAL,
    hsmBootLoaderVersion [1] IMPLICIT INTEGER OPTIONAL,
    hsmSerial            [2] IMPLICIT IA5String OPTIONAL,
    hsmChipUID           [3] IMPLICIT IA5String OPTIONAL,
    supervisor           [4] IMPLICIT IA5String  OPTIONAL,
    hsmModuleFlag        [5] IMPLICIT INTEGER OPTIONAL
}
-------------------------------------------------*/
public class KMPModuleInfo {

    String m_version;
    String m_creatorOIN;
    int m_hwId;
    int m_counter;
    String m_info;

    Integer m_memoryInUseSD;
    Integer m_memoryInUseSecure;
    ArrayList<KMPComponentInfo> m_components = new ArrayList<>();

    Integer m_hsmHwVersion;
    Integer m_hsmBlVersion;
    String m_hsmSerial;
    byte[] m_hsmChipUID;
    String m_supervisor;
    int m_hsmModuleFlags = 0;

    public String getVersion() {
        return m_version;
    }

    public String getCreatorOIN() {
        return m_creatorOIN;
    }

    public int getHardwareId() {
        return m_hwId;
    }

    public int getCounter() {
        return m_counter;
    }

    public String getInfo() {
        return m_info;
    }

    public Integer getMemoryInUseSD() {
        return m_memoryInUseSD;
    }

    public Integer getMemoryInUseSecure() {
        return m_memoryInUseSecure;
    }

    public int getHSMHardwareVersion() {
        return m_hsmHwVersion;
    }

    public String getSupervisor() {
        return m_supervisor;
    }

    public String getHSMHardwareVersionString() {
        String v = "";
        v += Integer.toString(m_hsmHwVersion & 0x000000FF);
        v += ".";
        v += String.format("%02d", (m_hsmHwVersion & 0x0000FFFF) >> 8);
        v += ".";
        v += Integer.toString((m_hsmHwVersion & 0x00FFFFFF) >> 16);
        v += ".";
        v += Integer.toString((m_hsmHwVersion & 0xFFFFFFFF) >> 24);
        return v;
    }

    public int getHSMBootloaderVersion() {
        return m_hsmBlVersion;
    }

    public String getHSMBootloaderVersionString() {
        String v = "";
        v += Integer.toString(m_hsmBlVersion & 0x000000FF);
        v += ".";
        v += String.format("%02d", (m_hsmBlVersion & 0x0000FFFF) >> 8);
        v += ".";
        v += Integer.toString((m_hsmBlVersion & 0x00FFFFFF) >> 16);
        v += ".";
        v += Integer.toString((m_hsmBlVersion & 0xFFFFFFFF) >> 24);
        return v;
    }

    public String getHSMSerial() {
        return m_hsmSerial;
    }

    public byte[] getHSMChipUID() {
        return m_hsmChipUID;
    }

    public ArrayList<KMPComponentInfo> getComponents() {
        return m_components;
    }

    public int getHSMModuleFlags() {
        return m_hsmModuleFlags;
    }

    public KMPModuleInfo(String version, String oin, int hwId, int counter, String info, Integer memSD, Integer memSec) {
        this.m_version = version;
        this.m_creatorOIN = oin;
        this.m_hwId = hwId;
        this.m_counter = counter;
        this.m_info = info;
        this.m_memoryInUseSD = memSD;
        this.m_memoryInUseSecure = memSec;
        this.m_hsmModuleFlags = 0;
    }

    public static KMPModuleInfo decode(byte[] encoded) throws IOException {

        ASN1InputStream asn1_stream = new ASN1InputStream(encoded);

        DLSequence seq = (DLSequence) asn1_stream.readObject();

        String _version = ((DERIA5String) seq.getObjectAt(0)).getString();
        String _oin = ((DERIA5String) seq.getObjectAt(1)).getString();
        Integer _hw_id = ((ASN1Integer) seq.getObjectAt(2)).getValue().intValue();
        Integer _counter = ((ASN1Integer) seq.getObjectAt(3)).getValue().intValue();
        String _info = ((DERIA5String) seq.getObjectAt(4)).getString();
        Integer _memSD = ((ASN1Integer) seq.getObjectAt(5)).getValue().intValue();
        Integer _memSecure = ((ASN1Integer) seq.getObjectAt(6)).getValue().intValue();

        KMPModuleInfo _modInfo = new KMPModuleInfo(_version, _oin, _hw_id, _counter, _info, _memSD, _memSecure);

        DLSequence componentListParser = (DLSequence) seq.getObjectAt(7);
        for (int i = 0; i < 3; i++) {
            DLSequence componentParser = (DLSequence) componentListParser.getObjectAt(i);
            String _label = ((DERIA5String) componentParser.getObjectAt(0)).getString();
            Boolean _active = ((ASN1Boolean) componentParser.getObjectAt(1)).isTrue();
            _modInfo.m_components.add(new KMPComponentInfo(_label, _active));
        }

        DLSequence extra_info_items = (DLSequence) seq.getObjectAt(8);

        for (int i = 0; i < extra_info_items.size(); i++) {
            ASN1TaggedObject to = (ASN1TaggedObject) extra_info_items.getObjectAt(i);

            switch (to.getTagNo()) {
                case 0: // HSM hw version
                    _modInfo.m_hsmHwVersion = ((ASN1Integer) to.getObject()).getValue().intValue();
                    break;
                case 1: // HSM bootloader version
                    _modInfo.m_hsmBlVersion = ((ASN1Integer) to.getObject()).getValue().intValue();
                    break;
                case 2: // HSM Serial
                    _modInfo.m_hsmSerial = ((DERIA5String) to.getObject()).getString();
                    break;
                case 3: // HSM Chip UID
                    String main_version = _version.substring(0, 3);
                    if (main_version.equalsIgnoreCase("1.0")) {
                        _modInfo.m_hsmChipUID = ((DERIA5String) to.getObject()).getString().getBytes();
                    } else {
                        _modInfo.m_hsmChipUID = ((DEROctetString) to.getObject()).getOctets();
                    }
                    break;
                case 4: // HSM supervisor
                    _modInfo.m_supervisor = ((DERIA5String) to.getObject()).getString();
                    break;
                case 5: // HSM bootloader version
                    _modInfo.m_hsmModuleFlags = ((ASN1Integer) to.getObject()).getValue().intValue();
                    break;
            }
        }

        return _modInfo;
    }

    public void print() {
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("-                                                   -");
        System.out.println("-----------------------------------------------------------------------");

        System.out.println("-----------------------------------------------------------------------");
    }
}
