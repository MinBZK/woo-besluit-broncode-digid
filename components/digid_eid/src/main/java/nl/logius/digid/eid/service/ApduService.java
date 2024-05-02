
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

package nl.logius.digid.eid.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import com.google.common.collect.ImmutableList;

import nl.logius.digid.card.ByteArrayUtils;
import nl.logius.digid.card.apdu.AESSecureMessaging;
import nl.logius.digid.card.apdu.SecureMessaging;
import nl.logius.digid.eid.apdu.StatusCode;
import nl.logius.digid.eid.exceptions.ClientException;
import nl.logius.digid.eid.models.EidSession;
import nl.logius.digid.eid.models.db.Certificate;
import nl.logius.digid.eid.security.ApduFactory;

public class ApduService {

    private final SecureMessaging sm;

    public ApduService(EidSession session) {
        final byte[] ssc = new byte[16];
        sm = new AESSecureMessaging(session.getkEnc().data, session.getkMac().data, ssc);
    }

    public ApduService(EidSession session, long counter) {
        final byte[] ssc = ByteArrayUtils.toByteArray(counter * 2, 16);
        sm = new AESSecureMessaging(session.getkEnc().data, session.getkMac().data, ssc);
    }

    public List<CommandAPDU> createSecureApdusForRDW(byte[] tag80, byte[] pcaApplicationId) {
        return ImmutableList.of(
            // [SELECT PCA-eIDAS]
            encrypt(ApduFactory.getSelectPcaEIDAS(pcaApplicationId)),
            // [MSE: Set AT (for PMA)]
            encrypt(ApduFactory.getCASetAT(tag80)),
            // [GA (for PMA)]
            encrypt(ApduFactory.getRdwGACommand())
        );
    }

    public List<CommandAPDU> createSecureNikPcaApdus(byte[] tag80) {
        return ImmutableList.of(
            // [MSE: Set AT (for PMA)]
            encrypt(ApduFactory.getCASetAT(tag80)),
            // [GA (for PMA)]
            encrypt(ApduFactory.getNikGACommand())
        );
    }

    public List<CommandAPDU> createPrepareEacNIKApdus(List<Certificate> certificates, String atSubject) {
        final ImmutableList.Builder<CommandAPDU> builder = ImmutableList.builder();

        for (Certificate certificate : certificates) {
            builder.add(encrypt(ApduFactory.getSetDstCar(certificate.getIssuer().getBytes(StandardCharsets.US_ASCII))));
            builder.addAll(encrypt(ApduFactory.getPsoVerifyCertificate(certificate)));
        }

        // 1.19 APDU > SET AT
        builder.add(encrypt(ApduFactory.getTASetAT(atSubject.getBytes(StandardCharsets.US_ASCII))));

        // 1.20 APDU > Get Challenge
        builder.add(encrypt(ApduFactory.getChallengeCommand()));
        return builder.build();
    }

    public CommandAPDU getExternalAuthenticate(byte[] signature) {
        return encrypt(ApduFactory.getExternalAuthenticate(signature));
    }


    public ResponseAPDU verify(ResponseAPDU response) {
        final ResponseAPDU decrypted = sm.decrypt(response);
        if (decrypted.getSW() != StatusCode.SUCCESS.val) {
            throw new ClientException(String.format("Unexpected response %04X", decrypted.getSW()));
        }
        sm.next();
        return decrypted;
    }

    public ResponseAPDU verify(List<ResponseAPDU> responses) {
        ResponseAPDU last = null;
        for (final ResponseAPDU response : responses) {
            last = verify(response);
        }
        return last;
    }

    private CommandAPDU encrypt(CommandAPDU plain) {
        final CommandAPDU encrypted = sm.encrypt(plain);
        sm.next();
        return encrypted;
    }

    private List<CommandAPDU> encrypt(List<CommandAPDU> plains) {
        final ImmutableList.Builder<CommandAPDU> builder = ImmutableList.builderWithExpectedSize(plains.size());
        for (final CommandAPDU plain : plains) {
            builder.add(sm.encrypt(plain));
            sm.next();
        }
        return builder.build();
    }

}
