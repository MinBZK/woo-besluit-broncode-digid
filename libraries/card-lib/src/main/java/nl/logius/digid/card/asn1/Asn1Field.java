
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

package nl.logius.digid.card.asn1;

/*
 * From Java 9 only java.beans is only included in desktop module,
 * can be replaced with: https://github.com/PPPPP/java-beans-lite
 */
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import nl.logius.digid.card.asn1.annotations.Asn1Entity;
import nl.logius.digid.card.asn1.annotations.Asn1ObjectIdentifier;
import nl.logius.digid.card.asn1.annotations.Asn1Property;

public class Asn1Field implements Comparable<Asn1Field> {
    public final Asn1Property property;
    public final PropertyDescriptor pd;
    public final Asn1Entity entity;
    public final Asn1ObjectIdentifier identifier;
    public final int tagNo;

    static List<Asn1Field> of(Class<?> type) {
        final BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type, Object.class);
        } catch (IntrospectionException e) {
            throw new Asn1Exception("Could not get info of bean %s", type);
        }

        final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
        final ArrayList<Asn1Field> fields = new ArrayList<>(descriptors.length);
        for (final PropertyDescriptor pd : descriptors) {
            final Method method = pd.getReadMethod();
            if (method == null) continue;

            final Asn1Property property = pd.getReadMethod().getAnnotation(Asn1Property.class);
            if (property != null) {
                fields.add(new Asn1Field(pd, property));
            }
        }
        return ImmutableList.sortedCopyOf(fields);
    }

    private Asn1Field(PropertyDescriptor pd, Asn1Property property) {
        this.pd = pd;
        this.property = property;
        entity = pd.getPropertyType().getAnnotation(Asn1Entity.class);
        identifier = pd.getReadMethod().getAnnotation(Asn1ObjectIdentifier.class);

        if (property.tagNo() != 0) {
            tagNo = property.tagNo();
            return;
        }
        if (entity == null || entity.tagNo() == 0) {
            throw new Asn1Exception("Tag of property can only be 0 if entity is specified on class with tag");
        }
        tagNo = entity.tagNo();
    }

    public Class<? extends Asn1Converter> converter() {
        if (property.converter() == Asn1Converter.None.class && entity != null) {
            return entity.converter();
        } else {
            return property.converter();
        }
    }

    public Class<?> type() {
        return pd.getPropertyType();
    }

    @Override
    public int compareTo(Asn1Field other) {
        int sgn = this.property.order() - other.property.order();
        if (sgn != 0) return sgn;

        sgn = this.tagNo - other.tagNo;
        if (sgn != 0) return sgn;

        if (this.identifier != null && other.identifier !=  null) {
            sgn = this.identifier.value().compareTo(other.identifier.value());
            if (sgn != 0) return sgn;
        }

        return this.pd.getName().compareTo(other.pd.getName());
    }
}
