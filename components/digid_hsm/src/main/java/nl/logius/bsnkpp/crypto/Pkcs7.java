
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

package nl.logius.bsnkpp.crypto;

import java.security.PrivateKey;
import java.util.Collection;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;


public class Pkcs7 {

    byte[] m_p7_der = null;
    byte[] m_content = null;

    private Pkcs7() {
    }

    public byte[] getContent() {
        return m_content;
    }

    static public Pkcs7 decode(byte[] der_encoded_p7, PrivateKey priv_key) throws Exception {
        CMSEnvelopedData ced = new CMSEnvelopedData(der_encoded_p7);
        Collection recip = ced.getRecipientInfos().getRecipients();

        KeyTransRecipientInformation rinfo = (KeyTransRecipientInformation) recip.iterator().next();
        byte[] content = rinfo.getContent(new JceKeyTransEnvelopedRecipient(priv_key).setProvider("BC"));

        Pkcs7 p7 = new Pkcs7();
        p7.m_p7_der = der_encoded_p7;
        p7.m_content = content;

        return p7;
    }
}
