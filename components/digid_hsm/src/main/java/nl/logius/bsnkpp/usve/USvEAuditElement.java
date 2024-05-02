
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

import java.nio.ByteBuffer;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;

/*------------------------------------------------------------------------
The ENCRYPTED audit element is a 16 byte block (OCTET STRING) as part of 
the PI/PP/PIP/DEP and EP/PP element.

The DECRYPTED audit element is returned from the HSM and is returned as a
simple ASN1 structure:

USvEAuditElement ::= SEQUENCE {
    auditElement OCTET STRING,
}
------------------------------------------------------------------------*/
public final class USvEAuditElement {

    private int hardwareId = 0;
    private int seconds = 0;
    private long counter = 0;

    /*---------------------------------------------------------
    Construct a new AuditElement providing the DECRYPTED raw
    16 byte audit element as input.
    ---------------------------------------------------------*/
    public USvEAuditElement(byte[] data) throws Exception {
        if (data.length != 16) {
            throw new Exception("[USvEAuditElement] Invalid length of audit element");
        }
        unpack(data);
    }

    /*---------------------------------------------------------
     The audit element is an 16 byte element build up as:
     [counter 0..7][seconds_1970 8..11][hardware_id [12..15]  
     This function unpacks the decrypted data. Note that the 
     ASN1 contains the encrypted data   
    ---------------------------------------------------------*/
    private void unpack(byte[] data) {

        byte[] hw_id = new byte[4];
        byte[] sec = new byte[4];
        byte[] cnt = new byte[8];

        System.arraycopy(data, 12, hw_id, 0, 4);
        System.arraycopy(data, 8, sec, 0, 4);
        System.arraycopy(data, 0, cnt, 0, 8);

        ByteBuffer wrapped = ByteBuffer.wrap(hw_id); // big-endian by default
        hardwareId = wrapped.getInt();

        wrapped = ByteBuffer.wrap(sec); // big-endian by default
        seconds = wrapped.getInt();

        wrapped = ByteBuffer.wrap(cnt); // big-endian by default
        counter = wrapped.getLong();
    }

    /*---------------------------------------------------------
    Parse the ASN1 structure of the DECODED audit element as 
    returned from the HSM PP crypto module.
     ---------------------------------------------------------*/
    public static USvEAuditElement decode(byte[] encoded) throws Exception {
        ASN1InputStream bIn = new ASN1InputStream(encoded);
        org.bouncycastle.asn1.DLSequence audit_sequence = (DLSequence) bIn.readObject();

        ASN1Encodable[] elements = audit_sequence.toArray();
         return new USvEAuditElement(((DEROctetString) elements[0]).getOctets());
    }


    /*---------------------------------------------------------
    Convert the seconds sinds 1-1-1970 to a readable/usable
    DateTime object.
    ---------------------------------------------------------*/
    public Date getDateTime() {
        Long sec = new Long(seconds);
        return new Date(sec * 1000);
    }

    public int getHardwareID() {
        return hardwareId;
    }

    public long getCounter() {
        return counter;
    }
    
    public int getSeconds() {
        return seconds;
    }
    
    public void validate() {
        
    }
}
