
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import nl.logius.digid.card.asn1.models.SecurityInfos;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.asn1.PolymorphicInfo;
import nl.logius.digid.eid.util.KeyUtils;

/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
public final class CardValidations {
    private static final Logger logger = LoggerFactory.getLogger(CardValidations.class);

    private static final List<byte[]> RDW_AID = Arrays.asList(
        Hex.decode("SSSSSSSSSSSSSSSSSSSSSS"),
        Hex.decode("SSSSSSSSSSSSSSSS")
    );

    private CardValidations() {}

    /**
     * Checks the polymorphic info on the cardaccess.
     * If validation is not correct a ClientException is thrown.
     *
     * @param info polymorphic info
     * @throws IOException
     */
    public static void validatePolymorhpicInfo(PolymorphicInfo info) {
        if (info.getPcaVersion() != 1) {
            logger.error("Unsupported PCA version {}", info.getPcaVersion());
            throw new ClientException("Polymorphic info is not correct");
        }
        int polymorphicFlags = info.getFlags().intValue();
        boolean randomizedPip = (polymorphicFlags & 32) != 0;
        boolean compressedEncoding = (polymorphicFlags & 4) != 0;
        if (!randomizedPip || !compressedEncoding) {
            logger.error("Polymorphic flags incorrect randomizedPip: {} compressedEncoding: {}",
                randomizedPip, compressedEncoding);
            throw new ClientException("Polymorphic info is not correct");
        }
    }

    /**
     * Check if the cardSecurity is correct with the cardaccess we received earlier.
     * Throw a ServerException when validation fails.
     *
     * @param cardSecurity   the CardSecurity
     * @param caKeyReference the cakeyreference from cardaccess
     * @param paceVersion    the paceversion from cardaccess
     * @param taVersion      the taversion from cardaccess
     */
    public static void validateCardSecurityVsCardAccess(SecurityInfos cardSecurity, int caKeyReference, int paceVersion,
                                                        int taVersion) {
        Assert.notNull(cardSecurity, "cardSecurity may not be null");

        if (caKeyReference != cardSecurity.getCaKeyId() || paceVersion != cardSecurity.getPaceVersion()
                || taVersion != cardSecurity.getTaVersion()) {
            logger.error("the card info and the card security do not match.");
            throw new ClientException("The card info and the card security do not match.");
        }
    }

    /**
     * Generates a byte[] terminaltoken from the ephemeral public key,
     * and look if it is the same as the client given tpicc.
     * Throws a ServerException when the validation fails.
     *
     * @param session         the eid session
     * @param tPicc           the tpicc we received from the client
     */
    public static void validateTerminalTokenVsTpicc(EidSession session, byte[] tPicc) {
        byte[] terminalToken = KeyUtils.getTerminalTokenFromCaPublicKey(
            session.getEphemeralKey().getQ(), session.getkMac().data
        );
        if (!Arrays.equals(terminalToken, tPicc)) {
            logger.error("the tpicc tokens does not match.");
            logger.debug("Terminal token {} not enquals tpicc {}", Base64.toBase64String(terminalToken),Base64.toBase64String(tPicc));
            throw new ClientException("the tpicc tokens do not match");
        }
    }

    public static void validateRdwAid(byte[] aId) {
        for (final byte[] compare : RDW_AID) {
            if (Arrays.equals(compare, aId)) {
                return;
            }
        }
        logger.error("Driving licence has unknown aId: {}", Hex.toHexString(aId).toUpperCase());
        throw new ClientException("Unknown aId");
    }
}
