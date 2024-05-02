
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
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.card.crypto.VerificationException;
import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.service.CardVerifier;
import nl.logius.digid.rda.service.RandomFactory;
import nl.logius.digid.rda.utils.MrzUtils;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.util.Map;

public class DrivingLicenceApp extends App {
    protected static final byte[] AID = Hex.decode("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");

    private static final Map<Step, byte[]> FILE_IDS = new ImmutableMap.Builder<Step, byte[]>()
        .put(Step.COM, Hex.decode("SSSSS"))
        .put(Step.SOD, Hex.decode("SSSSS"))
        .put(Step.AA_PUBLIC_KEY, Hex.decode("SSSSS"))
        .put(Step.DG14, Hex.decode("SSSSS"))
        .put(Step.MRZ_CHECK, Hex.decode("SSSSS"))
        .build();

    public DrivingLicenceApp(App.Session session, CardVerifier verifier, RandomFactory random) {
        super(session, verifier, random);
    }

    @Override
    protected void processFile(byte[] file) throws RdaException {
        if (session.getStep() != Step.MRZ_CHECK) {
            super.processFile(file);
            return;
        }
        final var nonMatch = verifier.verifyDataGroup(
            session.getCom(), session.getLdsSecurityObject(), file, NonMatch.class
        );
        session.setDocumentNumber(nonMatch.getDocumentNumber());
        verifier.verifyMrz(nonMatch, getSeedMrz());
    }

    protected String getSeedMrz() throws RdaException {
        final String mrz = session.selectedDrivingLicence();
        try {
            MrzUtils.checkDrivingLicenceMrz(mrz);
        } catch (VerificationException e) {
            throw new RdaException(RdaError.BAP, "Invalid MRZ");
        }
        return mrz.substring(1, 29);
    }

    @Override
    protected Class<? extends COM> getComClass() {
        return DrivingLicenceCOM.class;
    }

    @Override
    protected Map<Step, byte[]> getFileIds() {
        return FILE_IDS;
    }

    @Override
    protected MessageDigest defaultAaDigest(AaPublicKey publicKey) {
        return DigestUtils.digest("SHA-256");
    }
}
