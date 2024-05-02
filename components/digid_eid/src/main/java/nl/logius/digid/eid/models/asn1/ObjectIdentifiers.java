
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

package nl.logius.digid.eid.models.asn1;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;

public final class ObjectIdentifiers {

    private ObjectIdentifiers() {
        throw new IllegalStateException("Utility class");
    }

    public static final ASN1ObjectIdentifier id_AT = EACObjectIdentifiers.bsi_de.branch("3.1.2.2");
    public static final ASN1ObjectIdentifier id_LOGIUS_USVE = new ASN1ObjectIdentifier("2.16.528.1.1003.10");

    public static final ASN1ObjectIdentifier id_BSNK = id_LOGIUS_USVE.branch("1");
    public static final ASN1ObjectIdentifier id_BSNK_PIP = id_BSNK.branch("1.5");
    public static final ASN1ObjectIdentifier id_BSNK_PP = id_BSNK.branch("1.2");

    public static final ASN1ObjectIdentifier id_PCA = id_LOGIUS_USVE.branch("9");
    public static final ASN1ObjectIdentifier id_PCA_AT = id_PCA.branch("2");

    public static final ASN1ObjectIdentifier id_PCA_PIP = id_PCA.branch("3.3");
    public static final ASN1ObjectIdentifier id_PCA_PP = id_PCA.branch("4.3");
}
