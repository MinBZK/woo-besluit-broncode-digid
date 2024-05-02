
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

package nl.logius.digid.saml.util;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.metadata.resolver.impl.AbstractMetadataResolver;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

// TODO: place in shared lib? or place all xml logic in dc, and remove from saml
public class StringMetadataResolver extends AbstractMetadataResolver {

    private EntitiesDescriptor entitiesDescriptor;
    private EntityDescriptor entityDescriptor;

    public StringMetadataResolver() {
        createNewBackingStore();
    }

    public EntitiesDescriptor getEntitiesDescriptor() {
        return entitiesDescriptor;
    }

    public boolean addMetadataString(String rawMetadata) throws UnmarshallingException {
        InputStream inputStream = new ByteArrayInputStream(rawMetadata.getBytes(UTF_8));
        XMLObject metadata = super.unmarshallMetadata(inputStream);
        if (!isValid(metadata)) {
            return false;
        }

        if (metadata instanceof EntitiesDescriptor) {
            this.entitiesDescriptor = (EntitiesDescriptor) metadata;
        }

        if (metadata instanceof EntityDescriptor) {
            this.entityDescriptor = (EntityDescriptor) metadata;
        }

        return true;
    }

    @Nonnull
    @Override
    public Iterable<EntityDescriptor> resolve(CriteriaSet criteria) {
        List<EntityDescriptor> matchedEntityDescriptors = new ArrayList<>();
        if (entitiesDescriptor != null) {
            for (EntityDescriptor entityDescriptorLocal : entitiesDescriptor.getEntityDescriptors()) {
                if (criteria.contains(new EntityIdCriterion(entityDescriptorLocal.getEntityID()))) {
                    matchedEntityDescriptors.add(entityDescriptorLocal);
                }
            }
        }

        if (entityDescriptor != null && criteria.contains(new EntityIdCriterion(entityDescriptor.getEntityID()))) {
            matchedEntityDescriptors.add(entityDescriptor);
        }

        return matchedEntityDescriptors;
    }
}
