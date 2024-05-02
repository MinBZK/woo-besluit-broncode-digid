
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

package nl.logius.digid.eid.security;

import java.util.List;

import javax.smartcardio.CommandAPDU;

import org.bouncycastle.util.Arrays;

import com.google.common.collect.ImmutableList;

import nl.logius.digid.card.apdu.SecureMessaging;
import nl.logius.digid.card.asn1.Asn1Utils;
import nl.logius.digid.eid.apdu.CLA;
import nl.logius.digid.eid.apdu.INS;
import nl.logius.digid.eid.apdu.P1;
import nl.logius.digid.eid.apdu.P2;
import nl.logius.digid.eid.models.db.Certificate;

public final class ApduFactory {
    private ApduFactory() {}

    /**
     * creates the apdu for general authenticate
     *
     * @return the command apdu with the commands
     */
    public static CommandAPDU getRdwGACommand() {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        );
    }

    /**
     * creates the apdu for general authenticate
     *
     * @return the command apdu with the commands
     */
    public static CommandAPDU getNikGACommand() {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        );
    }

    /**
     * creates the apdu for getCASetAT
     *
     * @param caOID
     *            the oid from ca
     */
    public static CommandAPDU getCASetAT(byte[] caOID) {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        );
    }

    /**
     * this creates the command apdu for getSelectPcaEIDAS
     *
     * @param applicationId
     *            the pca application id
     * @return the command apdu with the commands
     */
    public static CommandAPDU getSelectPcaEIDAS(byte[] applicationId) {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        );
    }

    public static CommandAPDU getSetDstCar(byte[] car) {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
                SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS)
        );
    }

    public static List<CommandAPDU> getPsoVerifyCertificate(Certificate cert) {
        final byte ins = SSSSSSSSSSSSSSSSSSSSSSSS;
        final byte p1 = SSSSSSSSSSSSSSSSSSSSSSS;
        final byte p2 = SSSSSSSSSSSSSSSSSSSSSSS;

        final byte[] data = Asn1Utils.getValue(cert.getRaw());
        final int n = (data.length - 1) / SecureMessaging.MAX_COMMAND_SIZE + 1;

        final ImmutableList.Builder<CommandAPDU> chain = ImmutableList.builder();
        int from = 0;
        int to = SecureMessaging.MAX_COMMAND_SIZE;
        for (int i = 0; i < n - 1; i++) {
            final byte[] part = Arrays.copyOfRange(data, from, to);
            chain.add(new CommandAPDU(CLA.COMMAND_CHAINING.value, ins, p1, p2, part));
            from = to;
            to += SecureMessaging.MAX_COMMAND_SIZE;
        }
        final byte[] part = Arrays.copyOfRange(data, from, data.length);
        chain.add(new CommandAPDU(CLA.PLAIN.value, ins, p1, p2, part));

        return chain.build();
    }

    public static CommandAPDU getTASetAT(byte[] chr) {
        return new CommandAPDU(
            SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS,
                SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        );
    }

    public static CommandAPDU getChallengeCommand() {
        return new CommandAPDU(SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS);
    }

    public static CommandAPDU getExternalAuthenticate(byte[] signature) {
        return new CommandAPDU(SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS);
    }
}
