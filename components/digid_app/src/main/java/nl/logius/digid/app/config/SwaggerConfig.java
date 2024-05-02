
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

package nl.logius.digid.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import nl.logius.digid.sharedlib.config.AbstractSwaggerConfig;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig extends AbstractSwaggerConfig {
    public static final String REQUEST_ACCOUNT_AND_APP = "DigiD app - Aanvragen DigiD account (en Digid app) via de app (zonder gn/ww)";
    public static final String REQUEST_ACCOUNT_AND_APP_DESCRIPTION = "Startpunt -> geen DigiD account OF account in aanvraag. Actie -> Aanvragen DigiD account en app (via de app) zonder gebruikersnaam en wachtwoord. Optioneel om na het versturen van de brief (gebeurt op het endpoint /apps/pincode) de ID-check uit te voeren. Eindresultaat -> DigiD account en app aangevraagd en activatiebrief verstuurd. Indien ID-check uitgevoerd de aanvraag is op niveau Substantieel.";
    public static final String ACTIVATE_LETTER = "DigiD app - Activeren met basis authenticatie en notificatie brief";
    public static final String ACTIVATE_LETTER_DESCRIPTION = "Startpunt -> Een actief DigiD account, geen sms-controle aan EN geen NFC-lezer OF ID-check niet mogelijk/negeren. Actie -> Alternatieve activatie op niveau Midden. Eindresultaat -> Geactiveerde app op niveau Midden. Notificatiebrief ontvangen.";
    public static final String ACTIVATE_SMS = "DigiD app - Activeren met basis authenticatie en sms verificatie";
    public static final String ACTIVATE_SMS_DESCRIPTION = "Startpunt -> Een actief DigiD account, sms-controle aan EN geen NFC-lezer OF ID-check niet mogelijk/negeren. Actie -> Alternatieve activatie op niveau Midden met sms-controle. Eindresultaat -> Geactiveerde app op niveau Midden.";
    public static final String ACTIVATE_RDA = "DigiD app - Activeren met basis authenticatie en ID-check";
    public static final String ACTIVATE_RDA_DESCRIPTION = "Startpunt -> Een actief DigiD account op basis/midden niveau. Actie -> DigiD app activeren op Substantieel door het uitvoeren van de ID-check (indien ID-check negeren/niet mogelijk wordt een alternatief activatieproces gestart). Eindresultaat -> Geactiveerde app op niveau Substantieel.";
    public static final String ACTIVATE_WITH_APP = "DigiD app - Activeren app met andere DigiD app";
    public static final String ACTIVATE_WITH_APP_DESCRIPTION = "";
    public static final String RS_ACTIVATE_WITH_APP = "DigiD app - Activeren app bij aanvraagstation";
    public static final String RS_ACTIVATE_WITH_APP_DESCRIPTION = "";
    public static final String ACTIVATE_WEBSITE = "DigiD app - Activeren account en app na aanvraag via de website";
    public static final String ACTIVATE_WEBSITE_DESCRIPTION = "Startpunt -> aangevraagd account met gn/ww en eventueel sms (via de website). Actie -> Activeren van je account via een nieuwe app. Eindresultaat -> Actief account met een nieuwe app";
    public static final String ACTIVATE_APP_WITH_CODE = "DigiD app - Invoer activeringscode brief (aangevraag via app/aanvraagstation) in app met status: Wacht op activering";
    public static final String ACTIVATE_APP_WITH_CODE_DESCRIPTION = "DigiD app - Invoer activeringscode brief (aangevraag via app/aanvraagstation) in app met status: Wacht op activering";
    public static final String UPGRADE_LOGIN_LEVEL = "DigiD app - Verhogen inlogniveau naar substantieel";
    public static final String UPGRADE_LOGIN_LEVEL_DESCRIPTION = "DigiD app - Verhogen inlogniveau naar substantieel";
    public static final String RE_APPLY_ACTIVATIONCODE = "DigiD app - Heraanvraag brief met activeringscode voor DigiD account en app (via app of gemeentebalie)";
    public static final String RE_APPLY_ACTIVATIONCODE_DESCRIPTION = "DigiD app - Heraanvraag brief met activeringscode voor DigiD account en app (via app of gemeentebalie)";

    public static final String AUTH_WITH_WID = "DigiD app - Authenticeren met identiteitsdocument (DigiD Hoog)";
    public static final String AUTH_WITH_WID_DESCRIPTION = "Startpunt -> Een actief DigiD account met geactiveerd hoog middel. Actie -> Inloggen met identiteitsdocument op niveau hoog Eindresultaat -> Ingelogd bij webdienst op niveau hoog";
    public static final String HEADER = "header";
    public static final String MANAGE_ACCOUNT_SESSION = "DigiD app - Accounthandelingen Mijn DigiD";
    public static final String PINCODE_RESET_OF_APP = "DigiD app - Reset pincode of app";
    public static final String WIDCHECKER_RAISE_TO_SUB = "DigiD Wid Checker - Verhogen naar substantieel";

    public static final String APP_LOGIN = "DigiD app - Starten app (inloggen met pincode)";
    public static final String APP_LOGIN_DESCRIPTION = "Bij het openen van de app wordt je gelijk gevraagd om te authenticeren met pincode, voor andere flows die hierna komen hoeft niet meer te worden geauthenticeerd alleen eventueel bevestigd.";
    public static final String CONFIRM_SESSION = "DigiD app - Bevestigen actie/login met geauthenticeerde sessie";
    public static final String SHARED = "Shared endpoints";
    public static final String SHARED_DESCRIPTION = "Bij het starten van de app haalt deze een aantal gegevens op via deze endpoints. Bijv of switches aan/of uit.";

    @Value("${urls.external.app}")
    private String appBaseUrl;

    @Bean
    public GroupedOpenApi publicOpenApi() {
        var paths = new String[]{"/iapi/**", "/apps/**"};
        return GroupedOpenApi.builder().group("Public-api").pathsToMatch(paths).build();
    }

    @Override
    public OpenAPI IapiOpenAPI(){
        return new OpenAPI().addServersItem(new Server().url(appBaseUrl))
            .components(new Components()
                .addParameters("API-V", apiVersionHeader())
                .addParameters("OS-T", osTypeHeader())
                .addParameters("APP-V", appVersionHeader())
                .addParameters("OS-V", osVersionHeader())
                .addParameters("REL-T", releaseTypeHeader())
                .addParameters("X-Auth-Token", xAuthToken())
        ).tags(getFlowTags());
    }

    public List<Tag> getFlowTags(){
        return List.of(
            createTag(REQUEST_ACCOUNT_AND_APP, REQUEST_ACCOUNT_AND_APP_DESCRIPTION),
            createTag(ACTIVATE_LETTER, ACTIVATE_LETTER_DESCRIPTION),
            createTag(ACTIVATE_SMS, ACTIVATE_SMS_DESCRIPTION),
            createTag(ACTIVATE_RDA, ACTIVATE_RDA_DESCRIPTION),
            createTag(ACTIVATE_WITH_APP, ACTIVATE_WITH_APP_DESCRIPTION),
            createTag(ACTIVATE_APP_WITH_CODE, ACTIVATE_APP_WITH_CODE_DESCRIPTION),
            createTag(AUTH_WITH_WID, AUTH_WITH_WID_DESCRIPTION),
            createTag(RE_APPLY_ACTIVATIONCODE, RE_APPLY_ACTIVATIONCODE_DESCRIPTION),
            createTag(RS_ACTIVATE_WITH_APP, RS_ACTIVATE_WITH_APP_DESCRIPTION),
            createTag(UPGRADE_LOGIN_LEVEL, UPGRADE_LOGIN_LEVEL_DESCRIPTION),
            createTag(APP_LOGIN, APP_LOGIN_DESCRIPTION),
            createTag(ACTIVATE_WEBSITE, ACTIVATE_WEBSITE_DESCRIPTION),
            createTag(SHARED, SHARED_DESCRIPTION)
        );
    }

    private Tag createTag(String name, String description) {
        var tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        return tag;
    }

    protected static Parameter apiVersionHeader() {
        return new Parameter()
            .in(HEADER)
            .name("API-Version")
            .description("API-Version")
            .example("1")
            .required(true)
            .schema(new StringSchema());
    }

    protected static Parameter osTypeHeader() {
        return new Parameter()
            .in(HEADER)
            .name("OS-Type")
            .description("OS-Type")
            .example("Android")
            .required(true)
            .schema(new StringSchema());
    }

    protected static  Parameter appVersionHeader() {
        return new Parameter()
            .in(HEADER)
            .name("App-version")
            .description("App-version")
            .example("1.0.0")
            .required(true)
            .schema(new StringSchema());
    }

    protected static Parameter osVersionHeader() {
        return new Parameter()
            .in(HEADER)
            .name("OS-Version")
            .description("OS-Version")
            .example("10")
            .required(true)
            .schema(new StringSchema());
    }

    protected static Parameter releaseTypeHeader() {
        return new Parameter()
            .in(HEADER)
            .name("Release-Type")
            .description("Release-Type")
            .example("Productie")
            .required(true)
            .schema(new StringSchema());
    }

    protected static Parameter xAuthToken() {
        return new Parameter()
            .in("header")
            .name("X-Auth-Token")
            .description("X-Auth-Token")
            .example("development")
            .required(true)
            .schema(new StringSchema());
    }
}
