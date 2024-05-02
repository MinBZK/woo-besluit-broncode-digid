
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
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.util.Map;

public class TravelDocumentApp extends App {
    protected static final byte[] AID = Hex.decode("SSSSSSSSSSSSSSSSSSSS");
    private static final Map<Step, byte[]> FILE_IDS = new ImmutableMap.Builder<Step, byte[]>()
        .put(Step.COM, Hex.decode("SSSSS"))
        .put(Step.SOD, Hex.decode("SSSSS"))
        .put(Step.AA_PUBLIC_KEY, Hex.decode("SSSSS"))
        .put(Step.DG14, Hex.decode("SSSSS"))
        .put(Step.MRZ_CHECK, Hex.decode("SSSSS"))
        .build();

    public TravelDocumentApp(App.Session session, CardVerifier verifier, RandomFactory random) {
        super(session, verifier, random);
    }

    @Override
    protected void processFile(byte[] file) throws RdaException {
        if (session.getStep() != Step.MRZ_CHECK) {
            super.processFile(file);
            return;
        }
        final DataGroup1 dg1 = verifier.verifyDataGroup(
            session.getCom(), session.getLdsSecurityObject(), file, DataGroup1.class
        );
        verifier.verifyMrz(dg1, session.selectedTravelDocument());
        session.setBsn(dg1.getBsn());
        session.setMrzIdentifier(dg1.getMrzIdentifier());
        session.setDocumentType(dg1.getTypeOfTravelDocument());
        session.setDocumentNumber(dg1.getDocumentNumber());
    }

    @Override
    protected String getSeedMrz() throws RdaException {
        try {
            return MrzUtils.createTravelDocumentSeed(session.selectedTravelDocument());
        } catch (VerificationException e) {
            throw new RdaException(RdaError.BAC, "Invalid MRZ");
        }
    }

    @Override
    protected Class<? extends COM> getComClass() {
        return TravelDocumentCOM.class;
    }

    @Override
    protected Map<Step, byte[]> getFileIds() {
        return FILE_IDS;
    }

    @Override
    protected MessageDigest defaultAaDigest(AaPublicKey publicKey) {
        if (publicKey.getKeyParams() instanceof ECPublicKeyParameters) {
            return DigestUtils.digest("SHA-256");
        } else {
            return DigestUtils.digest("SHA1");
        }
    }
}
