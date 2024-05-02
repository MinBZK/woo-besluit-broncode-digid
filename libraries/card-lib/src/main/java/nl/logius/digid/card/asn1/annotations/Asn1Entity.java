
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

package nl.logius.digid.card.asn1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nl.logius.digid.card.asn1.Asn1Converter;
import nl.logius.digid.card.asn1.converters.SequenceConverter;

/**
 * Annotation for ASN1 type that can be mapped by Asn1ObjectMapper
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Asn1Entity {
    /**
     * Specify default tag number for this entity, can be overridden by Asn1Property
     *
     * If no default tag number is given (i.e. 0), it is assumed to be a sequence
     * @return tag
     */
    int tagNo() default 0;

    /**
     * Specify if entity should always be fully specified
     * @return true if partial
     */
    boolean partial() default false;

    /**
     * Specify custom converter for this entity
     * @return converter
     */
    Class<? extends Asn1Converter> converter() default SequenceConverter.class;

    /**
     * Specify param for converter if applicable
     * @return custom parameter
     */
    int param() default 0;
}
