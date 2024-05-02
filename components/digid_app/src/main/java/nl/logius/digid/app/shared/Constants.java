
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

package nl.logius.digid.app.shared;

import com.google.common.base.CaseFormat;

public final class Constants {

    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    public static String lowerUnderscore(String value) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, value);
    }

    public static final String ACCOUNT_ID = "accountId";
    public static final String PUBLIC_APP_KEY = "publicAppKey";
    public static final String ACTIVATION_CODE = "activationCode";
    public static final String ACTIVATION_METHOD = "activationMethod";
    public static final String APP_AUTHENTICATOR_ID = "appAuthenticatorId";
    public static final String APP_CODE = "appCode";
    public static final String APP_ID = "appId";
    public static final String APP_SESSION_ID = "appSessionId";
    public static final String ATTEMPTS = "attempts";
    public static final String AUTHENTICATE = "authenticate";
    public static final String AUTHENTICATION_LEVEL = "authenticationLevel";
    public static final String AUTHENTICATION_STATUS = "authenticationStatus";
    public static final String BLOCKED = "BLOCKED";
    public static final String BSN = "bsn";
    public static final String CLIENT_IP_ADDRESS = "clientIpAddress";
    public static final String CONFIRM_ID = "confirmId";
    public static final String CREATE_ACCOUNT = "createAccount";
    public static final String DATE_LETTER_SENT = "dateLetterSent";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String DAYS_VALID = "daysValid";
    public static final String DELAY = "delay";
    public static final String DEVICE_NAME = "deviceName";
    public static final String DISABLED = "DISABLED";
    public static final String DOC_TYPE = "docType";
    public static final String DOCUMENT_NUMBER = "documentNumber";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String DRIVING_LICENCES = "drivingLicences";
    public static final String EMAIL_REF = "emailRef";
    public static final String ENCRYPTED_PSEUDONYM_STATUS_CONTROLLER = "epsc";
    public static final String ERROR = "error";
    public static final String ERROR_CODE = "errorCode";
    public static final String GBA_STATUS = "gbaStatus";
    public static final String GELDIGHEIDSTERMIJN = "geldigheidstermijn";
    public static final String GEMEENTEBALIE = "gemeentebalie";
    public static final String HAS_BSN = "hasBsn";
    public static final String HEADER = "header";
    public static final String HIDDEN = "hidden";
    public static final String HOUSE_NUMBER = "houseNumber";
    public static final String HOUSE_NUMBER_ADDITIONS = "houseNumberAdditions";
    public static final String HUMAN_PROCESS = "humanProcess";
    public static final String IAPI = "iapi";
    public static final String IDENTITY = "identity";
    public static final String INVALID = "invalid";
    public static final String INCLUDE_LINKS = "includeLinks";
    public static final String ISSUER_TYPE = "issuerType";
    public static final String KEY = "key";
    public static final String KILL_APP = "kill_app";
    public static final String KSV = "ksv";
    public static final String LANGUAGE = "language";
    public static final String LOCALE = "locale";
    public static final String MAX_AMOUNT_OF_ACCOUNT_APPLICATION_PER_MONTH = "maxAmountOfAccountApplicationPerMonth";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String NOK = "NOK";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String NOTIFICATION_SUBJECT = "notificationSubject";
    public static final String NO_OF_DAYS_BETWEEN_ACCOUNT_APPLICATION = "noOfDaysBetweenAccountApplication";
    public static final String OIDC_SESSION_ID = "oidcSessionId";
    public static final String OK = "OK";
    public static final String OS_TYPE = "osType";
    public static final String PASSWORD = "password";
    public static final String PAYLOAD = "payload";
    public static final String PIP = "pip";
    public static final String POLYMORPH = "polymorph";
    public static final String POSTAL_CODE = "postalCode";
    public static final String PSEUDONYM = "pseudonym";
    public static final String RECEIVE_NOTIFICATIONS = "receiveNotifications";
    public static final String REGISTRATION_ID = "registrationId";
    public static final String REMAINING_ATTEMPTS = "remainingAttempts";
    public static final String REPLACE_ACCOUNT = "replaceAccount";
    public static final String REPLACE_APPLICATION = "replaceApplication";
    public static final String REQUESTS = "requests";
    public static final String RETURN_URL = "returnUrl";
    public static final String RE_REQUEST_LETTER = "reRequestLetter";
    public static final String SEQUENCE_NO = "sequenceNo";
    public static final String SMSCODE = "smscode";
    public static final String SMS_REF = "smsRef";
    public static final String SPOKEN = "spoken";
    public static final String STANDING = "standing";
    public static final String STATUS = "status";
    public static final String STRING = "string";
    public static final String TOTAL_ALLOWED_ATTEMPTS = "totalAllowedAttempts";
    public static final String TRANSACTION_ID = "transactieid";
    public static final String TRAVEL_DOCUMENTS = "travelDocuments";
    public static final String USERNAME = "username";
    public static final String USER_APP = "userApp";
    public static final String USER_APP_ID = "userAppId";
    public static final String VALUE = "value";
    public static final String WEBSERVICE_ID = "webserviceId";
    public static final String WID_REQUEST_ID = "widRequestId";
    public static final String CHANGE_APP_PIN = "ChangeAppPin";

    public static final String TOO_MANY_APPS = "TOO_MANY_APPS";
}
