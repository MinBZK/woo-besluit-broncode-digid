
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

package nl.logius.digid.saml.helpers;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import nl.logius.digid.saml.util.StringMetadataResolver;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class MetadataParser {

    private MetadataParser() {
        throw new IllegalStateException("Utility class");
    }

    private static Logger logger = LoggerFactory.getLogger(MetadataParser.class);

    public static EntityDescriptor readMetadata(Resource metadataFile, String entityId) {
        try (Reader reader = new InputStreamReader(metadataFile.getInputStream(), UTF_8)) {
            return parseMetadata(FileCopyUtils.copyToString(reader), entityId);
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
        return null;
    }

    private static EntityDescriptor parseMetadata(String rawMetadata, String entityId) {
        StringMetadataResolver metadataResolver = new StringMetadataResolver();
        try {
            metadataResolver.setParserPool(XMLObjectProviderRegistrySupport.getParserPool());
            metadataResolver.setId("StringMetadataResolver");
            metadataResolver.initialize();
            metadataResolver.addMetadataString(rawMetadata);
            CriteriaSet criteria = new CriteriaSet(new EntityIdCriterion(entityId));
            return metadataResolver.resolveSingle(criteria);
        } catch (UnmarshallingException | ResolverException | ComponentInitializationException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
