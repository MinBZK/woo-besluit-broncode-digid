
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

package nl.logius.digid.dc.util;

import org.bouncycastle.asn1.*;

import java.util.Arrays;
import java.util.Iterator;

public class X509CertificateDataParser {
    public static final String X509_NAME_SERIALNUMBER_OID = "2.5.4.5";
    public static final String X509_NAME_STRING_OID = "2.5.5.5";
    public static final String X509_SUBJECT_ALT_NAME_OID = "2.5.29.17";
    public static final int X509_SUBJECT_ALT_TAG_NUMBER = 3;
    public static final String URAPREFIX = "00000009";
    public static final String URASUFFIX = "0000";

    private X509CertificateDataParser() {
        throw new IllegalStateException("Utility class");
    }

    public static String getOin(byte[] value) {
        ASN1Sequence input = ASN1Sequence.getInstance(value);
        Iterator iterator = ((DLSequence) input.getObjectAt(0)).iterator();
        String result = null;

        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof DLTaggedObject) {
                String subjectAltName = findSubjectAltName((DLTaggedObject) o);
                if (subjectAltName != null)
                    return subjectAltName;
            }
            else if (o instanceof DLSequence) {
                String serialNumber = findSerialNumber((DLSequence) o);
                if (serialNumber != null)
                    result = serialNumber;
            }
        }

        return result;
    }

    private static String findSerialNumber(DLSequence sequence) {
         Iterator nestedIterator = sequence.iterator();

        while (nestedIterator.hasNext()) {
            Object o = nestedIterator.next();
            if ((o instanceof DLSet)) {
                DLSet dlSet = (DLSet) o;
                DLSequence subSequence = (DLSequence) dlSet.getObjectAt(0);
                ASN1ObjectIdentifier identifier = (ASN1ObjectIdentifier) subSequence.getObjectAt(0);
                if ((identifier.toString().equals(X509_NAME_SERIALNUMBER_OID))) {
                    return subSequence.getObjectAt(1).toString();
                }
            }
        }
        return null;
    }


    private static String findSubjectAltName(DLTaggedObject taggedObject){
        if (taggedObject.getTagNo() != X509_SUBJECT_ALT_TAG_NUMBER) return null;

        Iterator nestedIterator = ((DLSequence) taggedObject.getObject()).iterator();

        while (nestedIterator.hasNext()) {
            Object o = nestedIterator.next();
            if ((o instanceof DLSequence)) {
                DLSequence sequence = (DLSequence) o;
                ASN1ObjectIdentifier identifier = (ASN1ObjectIdentifier) sequence.getObjectAt(0);
                if (identifier.toString().equals(X509_SUBJECT_ALT_NAME_OID)) {
                    DEROctetString octetData = (DEROctetString) sequence.getObjectAt(1);
                    sequence = (DLSequence) DLSequence.getInstance(octetData.getOctets());

                    if (sequence.size() > 1) {
                        taggedObject = (DLTaggedObject) sequence.getObjectAt(1);
                        if ((taggedObject.getObject() instanceof DLSequence)) {
                            sequence = (DLSequence) taggedObject.getObject();
                            taggedObject = (DLTaggedObject) sequence.getObjectAt(1);
                            String data = taggedObject.getObject().toString();

                            //<OID CA>-<versie-nr>-<UZI-nr>-<pastype>-<Abonnee-nr>-<rol>-<AGB-code>
                            String uzi = Arrays.asList(data.split("-")).get(4);

                            //<prefix><nummer><suffix>
                            return URAPREFIX + uzi + URASUFFIX;
                        }
                    }
                }
            }
        }
        return null;
    }
}
