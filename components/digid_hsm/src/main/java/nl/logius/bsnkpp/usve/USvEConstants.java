
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

public class USvEConstants {

    public final static int SCHEME_VERSION = 1;

    public final static String OID_NL = "2.16.528.1.1003";

    // id-BSNk-scheme-nl OBJECT IDENTIFIER ::= { joint-iso-itu-t(2) country(16) nl(528) nederlandse-organisatie(1) nederlandse-overheid(1003) ..... TODO }
    private static final String OID_BSNK = OID_NL + "." + 10;
    // id-BSNk-identifiers OBJECT IDENTIFIER ::= { id-BSNk-scheme-nl 1 }
    private static final String OID_BSNK_IDS = OID_BSNK + '.' + "1";
    // id-BSNk-polymorphics OBJECT IDENTIFIER ::= { id-BSNk-identifiers 1 }
    private static final String OID_BSNK_POLYMORPHICS = OID_BSNK_IDS + '.' + "1";
    // id-BSNk-polymorphic-identity OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 1 }
    public static final String OID_BSNK_PI = OID_BSNK_POLYMORPHICS + '.' + "1";
    // id-BSNk-polymorphic-pseudonym OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 2 }
    public static final String OID_BSNK_PP = OID_BSNK_POLYMORPHICS + '.' + "2";
    // id-BSNk-polymorphic-identity-signed OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 3 }
    public static final String OID_BSNK_PI_SIGNED = OID_BSNK_POLYMORPHICS + '.' + "3";
    // id-BSNk-polymorphic-pseudonym-signed OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 4 }
    public static final String OID_BSNK_PP_SIGNED = OID_BSNK_POLYMORPHICS + '.' + "4";
    // id-BSNk-polymorphic-pip OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 5 }
    public static final String OID_BSNK_PIP = OID_BSNK_POLYMORPHICS + '.' + "5";
     // id-BSNk-polymorphic-pip-verifiable OBJECT IDENTIFIER ::= { id-BSNk-polymorphics 11 }
    public static final String OID_BSNK_VPIP = OID_BSNK_POLYMORPHICS + '.' + "11";

    // id-BSNk-encrypted OBJECT IDENTIFIER ::= { id-BSNk-ids 2 }
    private static final String OID_BSNK_ENCRYPTED = OID_BSNK_IDS + '.' + "2";
    // id-BSNk-encrypted-identity OBJECT IDENTIFIER ::= { id-BSNk-encrypted 1 }
    public static final String OID_BSNK_EI = OID_BSNK_ENCRYPTED + '.' + "1";
    public static final String OID_BSNK_EI_V2 = OID_BSNK_EI + '.' + "2";
    // id-BSNk-encrypted-pseudonym OBJECT IDENTIFIER ::= { id-BSNk-encrypted 2 }
    public static final String OID_BSNK_EP = OID_BSNK_ENCRYPTED + '.' + "2";
    // id-BSNk-encrypted-identity-signed OBJECT IDENTIFIER ::= { id-BSNk-encrypted 3 }
    public static final String OID_BSNK_EI_SIGNED_DEPRECATED = OID_BSNK_ENCRYPTED + '.' + "3";
    // id-BSNk-encrypted-pseudonym-signed OBJECT IDENTIFIER ::= { id-BSNk-encrypted 4 }
    public static final String OID_BSNK_EP_SIGNED_DEPRECATED = OID_BSNK_ENCRYPTED + '.' + "4";
    // id-BSNk-encrypted-direct-pseudonym OBJECT IDENTIFIER ::= { id-BSNk-encrypted 5 }
    public static final String OID_BSNK_DEP = OID_BSNK_ENCRYPTED + '.' + "5";
    public static final String OID_BSNK_DEP_V2 = OID_BSNK_DEP + '.' + "2";

    // id-BSNk-encrypted-direct-pseudonym OBJECT IDENTIFIER ::= { id-BSNk-encrypted 6 }
    public static final String OID_BSNK_DEP_SIGNED = OID_BSNK_ENCRYPTED + '.' + "6";
    public static final String OID_BSNK_DEP_SIGNED_V2 = OID_BSNK_DEP_SIGNED + '.' + "2";
    
    // id-BSNk-encrypted-identity-signed OBJECT IDENTIFIER ::= { id-BSNk-encrypted 7 }
    public static final String OID_BSNK_EI_SIGNED = OID_BSNK_ENCRYPTED + '.' + "7";
    public static final String OID_BSNK_EI_SIGNED_V2 = OID_BSNK_EI_SIGNED + '.' + "2";
        // id-BSNk-encrypted-pseudonym-signed OBJECT IDENTIFIER ::= { id-BSNk-encrypted 8 }
    public static final String OID_BSNK_EP_SIGNED = OID_BSNK_ENCRYPTED + '.' + "8";
    public static final String OID_BSNK_EP_SIGNED_V2 = OID_BSNK_EP_SIGNED + '.' + "2";
    
    public static final String OID_BSNK_DEI = OID_BSNK_ENCRYPTED + '.' + "9";
    public static final String OID_BSNK_DEI_SIGNED = OID_BSNK_ENCRYPTED + '.' + "10";
    
    public static final String OID_BSNK_DEI_V2 = OID_BSNK_DEI + '.' + "2";
    public static final String OID_BSNK_DEI_SIGNED_V2 = OID_BSNK_DEI_SIGNED + '.' + "2";
    
    // id-BSNk-decrypted OBJECT IDENTIFIER ::= { id-BSNk-identifiers 3 }
    private static final String OID_BSNK_DECRYPTED = OID_BSNK_IDS + '.' + "3";
    // id-BSNk-decrypted-identifier OBJECT IDENTIFIER ::= { id-BSNk-decrypted 1 }
    public static final String OID_BSNK_I = OID_BSNK_DECRYPTED + '.' + "1";
    // id-BSNk-decrypted-pseudonym OBJECT IDENTIFIER ::= { id-BSNk-decrypted 2 }
    public static final String OID_BSNK_P = OID_BSNK_DECRYPTED + '.' + "2";
    public static final String OID_BSNK_MIGRATION_INTERMEDIATE_P = OID_BSNK_DECRYPTED + '.' + "3";

    public static final String OID_ECDSA_WITH_SHA384 = "1.2.840.10045.4.3.3";
    public static final String OID_BSI_ECSDSA_PLAIN_SHA384_LEGACY = "0.4.0.127.0.7.1.1.4.3.3";
    public static final String OID_BSI_ECSDSA_PLAIN_SHA384 = "0.4.0.127.0.7.1.1.4.4.3";
    
    public static final String OID_BRAINPOOLP320R1  = "1.3.36.3.3.2.8.1.1.9"; 
}

