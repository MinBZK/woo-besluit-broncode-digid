
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

package nl.logius.digid.saml;

public final class AttributeTypes {
    public static final String INTENDED_AUDIENCE = "urn:nl-eid-gdi:1.0:IntendedAudience";
    public static final String SERVICE_UUID = "urn:nl-eid-gdi:1.0:ServiceUUID";
    public static final String IDP_ASSERTION = "urn:nl-eid-gdi:1.0:IdpAssertion";
    public static final String LEGACY_BSN = "urn:nl-eid-gdi:1.0:id:legacy-BSN";
    public static final String BSN = "urn:nl-eid-gdi:1.0:id:BSN";
    public static final String PSEUDONYM = "urn:nl-eid-gdi:1.0:id:Pseudonym";
    public static final String ACTING_SUBJECT_ID = "urn:nl-eid-gdi:1.0:ActingSubjectID";
    public static final String LEGAL_SUBJECT_ID = "urn:nl-eid-gdi:1.0:LegalSubjectID";
    public static final String MANDATE_SERVICES = "urn:nl-eid-gdi:1.0:MandateServices";

    private AttributeTypes() {
        throw new IllegalStateException("Utility class");
    }
}
