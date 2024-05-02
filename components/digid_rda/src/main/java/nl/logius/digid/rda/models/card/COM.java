
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

package nl.logius.digid.rda.models.card;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nl.logius.digid.card.asn1.Asn1Constructed;
import nl.logius.digid.card.asn1.Asn1Exception;
import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public abstract class COM implements Asn1Constructed, Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] tagList;
    private Set<Integer> dataGroups;

    protected static Map<Byte, Integer> createTagToDataGroupMap(Byte[] tags) {
        final ImmutableMap.Builder<Byte, Integer> builder = ImmutableMap.builder();
        for (int i = 0; i < tags.length; i++) {
            final Byte tag = tags[i];
            if (tag != null) {
                builder.put(tag, i);
            }
        }
        return builder.build();
    }

    protected abstract Map<Byte, Integer> getTagToDataGroupMap();

    public abstract Set<Integer> getRdaDataGroups();

    public int getDataGroupOfTag(byte tag) {
        return getTagToDataGroupMap().get(tag);
    }

    @Override
    public void constructed(Asn1ObjectMapper mapper) {
        final Map<Byte, Integer> map = getTagToDataGroupMap();
        final ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        for (final byte tag : tagList) {
            final Integer dg = map.get(tag);
            if (dg == null) {
                throw new Asn1Exception("Unknown tag %x encountered", tag);
            }
            builder.add(dg);
        }
        dataGroups = builder.build();
    }

    @Asn1Property(tagNo = 0x5c, order = 2)
    public byte[] getTagList() {
        return tagList;
    }

    public void setTagList(byte[] tagList) {
        this.tagList = tagList;
    }

    public Set<Integer> getDataGroups() {
        return dataGroups;
    }
}
