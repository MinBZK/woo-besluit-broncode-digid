
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

package nl.logius.digid.eid.validations;

import nl.logius.digid.card.asn1.Asn1ObjectMapper;
import nl.logius.digid.eid.BaseTest;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.asn1.PcaSecurityInfos;
import nl.logius.digid.eid.models.asn1.PolymorphicInfo;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class CardValidationsTest extends BaseTest {
    private final PcaSecurityInfos efCardSecurity = new PcaSecurityInfos();

    private Asn1ObjectMapper mapper = new Asn1ObjectMapper();

    @BeforeEach
    public void init() {
        efCardSecurity.setTaVersion(1);
        efCardSecurity.setCaKeyId(1);
        efCardSecurity.setPaceVersion(1);
    }

    @Test
    public void validPolymorhpicInfo() {
        final PolymorphicInfo info = mapper.read(
            Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"),
            PolymorphicInfo.class);
        CardValidations.validatePolymorhpicInfo(info);
    }

    @Test
    public void invalidPolymorphicInfoVersion() {
        final PolymorphicInfo info = mapper.read(
            Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"),
            PolymorphicInfo.class);

        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validatePolymorhpicInfo(info));
        assertEquals("Polymorphic info is not correct", thrown.getMessage());
    }

    @Test
    public void invalidPolymorphicRandomizedPip() {
        final PolymorphicInfo info = mapper.read(
            Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"),
            PolymorphicInfo.class);

        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validatePolymorhpicInfo(info));
        assertEquals("Polymorphic info is not correct", thrown.getMessage());

    }

    @Test
    public void invalidPolymorphicCompressedEncoding() {
        final PolymorphicInfo info = mapper.read(
            Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"),
            PolymorphicInfo.class);

        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validatePolymorhpicInfo(info));
        assertEquals("Polymorphic info is not correct", thrown.getMessage());
    }

    @Test
    public void validateCardSecurityVsCardAccessSuccessful() {
        CardValidations.validateCardSecurityVsCardAccess(efCardSecurity, 1, 1, 1);
    }

    @Test
    public void validateCardSecurityVsCardAccessCaFail() {
        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validateCardSecurityVsCardAccess(efCardSecurity, 2, 1, 1));
        assertEquals("The card info and the card security do not match.", thrown.getMessage());
    }

    @Test
    public void validateCardSecurityVsCardAccessTaFail() {
        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validateCardSecurityVsCardAccess(efCardSecurity, 1, 1, 10));
        assertEquals("The card info and the card security do not match.", thrown.getMessage());
    }

    @Test
    public void validateCardSecurityVsCardAccessPaceFail() {
        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validateCardSecurityVsCardAccess(efCardSecurity, 1, 2, 1));
        assertEquals("The card info and the card security do not match.", thrown.getMessage());
    }

    @Test
    public void validateRdwAidUnsuccessful() {
        ClientException thrown = assertThrows(ClientException.class, () -> CardValidations.validateRdwAid(Hex.decode("SSSSSS")));
        assertEquals("Unknown aId", thrown.getMessage());
    }

    @Test
    public void validateRdwAidSuccessful() {
        CardValidations.validateRdwAid(Hex.decode("SSSSSSSSSSSSSSSSSSSSSS"));
    }
}
