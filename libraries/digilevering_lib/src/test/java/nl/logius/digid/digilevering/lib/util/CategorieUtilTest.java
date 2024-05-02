
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

package nl.logius.digid.digilevering.lib.util;

import nl.logius.digid.digilevering.api.model.Container;
import nl.logius.digid.digilevering.api.model.Element;
import nl.logius.digid.digilevering.api.util.CategorieUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CategorieUtilTest {

    @Test
    public void testCreateCategorie() {
        Container container = CategorieUtil.createCategorie("A", "B", "C", "D", "E");

        assertThat(container.getNummer(), is("A"));
        assertThat(container.getElement().size(), is(2));
        assertThat(container.getElement().get(0).getNummer(), is("B"));
        assertThat(container.getElement().get(0).getValue(), is("C"));
        assertThat(container.getElement().get(1).getNummer(), is("D"));
        assertThat(container.getElement().get(1).getValue(), is("E"));
    }

    @Test
    public void testFindAnummer() {
        assertThat(CategorieUtil.findANummer(createFullCategories()), is("a-nummer"));
    }

    @Test
    public void testFindBsn() {
        assertThat(CategorieUtil.findBsn(createFullCategories()), is("burgerservicenummer"));
    }

    @Test
    public void testFindDatumOverlijden() {
        assertThat(CategorieUtil.findDatumOverlijden(createFullCategories()), is("datumoverlijden"));
    }

    @Test
    public void testFindDatumOverlijdenOudeWaarde() {
        assertThat(CategorieUtil.findDatumOverlijdenOudeWaarde(createFullCategories()), is("datumoverlijden_oud"));
    }

    @Test
    public void testFindDRedenOpschorting() {
        assertThat(CategorieUtil.findRedenOpschorting(createFullCategories()), is("redenopschorting"));
    }

    @Test
    public void testFindRedenOpschortingOudeWaarde() {
        assertThat(CategorieUtil.findRedenOpschortingOudeWaarde(createFullCategories()), is("redenopschorting_oud"));
    }

    @Test
    public void testEmptyCategorie() {
        Container container = new Container();
        assertThat(CategorieUtil.findBsn(List.of(container)), nullValue());
    }

    @Test
    public void testEmptyElement() {
        Container container = new Container();
        container.setNummer(CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS);
        Element element = new Element();
        container.getElement().add(element);
        assertThat(CategorieUtil.findBsn(List.of(container)), nullValue());
    }

    private List<Container> createFullCategories() {
        List<Container> categories = new ArrayList<>();
        categories.add(
                CategorieUtil.createCategorie(
                        CategorieUtil.CATEGORIE_IDENTIFICATIENUMMERS,
                        CategorieUtil.ELEMENT_A_NUMMER, "a-nummer",
                        CategorieUtil.ELEMENT_BURGERSERVICENUMMER, "burgerservicenummer"
                )
        );
        categories.add(
                CategorieUtil.createCategorie(
                        CategorieUtil.CATEGORIE_OVERLIJDEN,
                        CategorieUtil.ELEMENT_DATUM_OVERLIJDEN, "datumoverlijden"
                )
        );
        categories.add(
                CategorieUtil.createCategorie(
                        CategorieUtil.CATEGORIE_OVERLIJDEN_OUDE_WAARDE,
                        CategorieUtil.ELEMENT_DATUM_OVERLIJDEN, "datumoverlijden_oud"
                )
        );
        categories.add(
                CategorieUtil.createCategorie(
                        CategorieUtil.CATEGORIE_INSCHRIJVING,
                        CategorieUtil.ELEMENT_REDEN_OPSCHORTING, "redenopschorting"
                )
        );
        categories.add(
                CategorieUtil.createCategorie(
                        CategorieUtil.CATEGORIE_INSCHRIJVING_OUDE_WAARDE,
                        CategorieUtil.ELEMENT_REDEN_OPSCHORTING, "redenopschorting_oud"
                )
        );
        return categories;
    }

}
