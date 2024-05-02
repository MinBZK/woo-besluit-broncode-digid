
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

package nl.logius.digid.eid.config;

/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
public class Constants {

    private Constants() {
    }

    public static final String URL_RDW_GETCERTIFICATE = "/v1/rdw/getcertificate";
    public static final String URL_RDW_POLYMORPHICINFO = "/v1/rdw/polymorph/info";
    public static final String URL_RDW_SIGNATURE = "/v1/rdw/signature";
    public static final String URL_RDW_SECAPDU = "/v1/rdw/secapdu";
    public static final String URL_RDW_POLYMORPHICDATA = "/v1/rdw/polymorph/data";
    public static final String URL_NIK_START = "/v1/nik/start";
    public static final String URL_NIK_PREPARE_EAC = "/v1/nik/prepareeac";
    public static final String URL_NIK_PREPARE_PCA = "/v1/nik/preparepca";
    public static final String URL_NIK_POLYMORPHICDATA = "/v1/nik/polymorph/data";

    public static final String URL_OLD_RDW_GETCERTIFICATE = "/v1/getcertificate";
    public static final String URL_OLD_RDW_POLYMORPHICINFO = "/v1/polymorph/info";
    public static final String URL_OLD_RDW_SIGNATURE = "/v1/signature";
    public static final String URL_OLD_RDW_SECAPDU = "/v1/secapdu";
    public static final String URL_OLD_RDW_POLYMORPHICDATA = "/v1/polymorph/data";
}
