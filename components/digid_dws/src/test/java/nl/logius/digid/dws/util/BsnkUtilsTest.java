
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

package nl.logius.digid.dws.util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eid.gdi.nl._1_0.webservices.PolymorphicPseudonymType;
import eid.gdi.nl._1_0.webservices.ProvidePPPPCAOptimizedRequest;
import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.pp.parser.PublicKeyParser;

import javax.xml.datatype.DatatypeConstants;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles({ "default", "unit-test" })
public class BsnkUtilsTest {

    @Mock
    private BsnkUtils bsnkUtil;

    @BeforeEach
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
        String digidMuOin = "digid.mu.oin";
        BigInteger digidMuKsv = BigInteger.valueOf(1);
        String bsnkUPubkeyBase64 = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        BigInteger bsnkUKsv = BigInteger.valueOf(1);
        ECPublicKey bsnkUPubkey = (ECPublicKey) PublicKeyParser
                .decodeKey(Base64.getDecoder().decode(bsnkUPubkeyBase64));
        bsnkUtil = new BsnkUtils(digidMuOin, digidMuKsv, bsnkUPubkey, bsnkUKsv);
    }

    private String signedPipBase64 = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private String pipbase64 = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private String ppbase64 = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    private ASN1Sequence signedPip;
    private ASN1Sequence pp;

    public BsnkUtilsTest() throws IOException {
        signedPip = (ASN1Sequence) ASN1Sequence.fromByteArray(Base64.getDecoder().decode(signedPipBase64));
        pp = (ASN1Sequence) ASN1Sequence.fromByteArray(Base64.getDecoder().decode(ppbase64));
    }

    @Test
    public void signedPipFromPplistTest() throws IOException, BsnkException {
        List<PolymorphicPseudonymType> pplist = new ArrayList<>();
        pplist.add(new PolymorphicPseudonymType() {
            {
                value = signedPip.getEncoded();
            }
        });

        ASN1Sequence result = bsnkUtil.signedPipFromPplist(pplist);
        assertEquals(Base64.getEncoder().encodeToString(result.getEncoded()), signedPipBase64);
    }

    @Test()
    public void signedPipFromPplistNoPipTest() throws IOException {
        List<PolymorphicPseudonymType> pplist = new ArrayList<>();
        pplist.add(new PolymorphicPseudonymType() {
            {
                value = pp.getEncoded();
            }
        });

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bsnkUtil.signedPipFromPplist(pplist));

        assertEquals("No signed pip found in PolymorphicPseudonymType list", ex.getMessage());
    }

    @Test()
    public void signedPipFromPplistInvalidPpTest() throws IOException {
        List<PolymorphicPseudonymType> pplist = new ArrayList<>();
        pplist.add(new PolymorphicPseudonymType() {
            {
                value = "Not an ASN1 sequence".getBytes();
            }
        });
        pplist.add(new PolymorphicPseudonymType() {
            {
                value = signedPip.getEncoded();
            }
        });

        ASN1Sequence result = bsnkUtil.signedPipFromPplist(pplist);
        assertEquals(Base64.getEncoder().encodeToString(result.getEncoded()), signedPipBase64);
    }

    @Test
    public void retrievePipFromSignedPipTest() throws IOException, BsnkException {
        ASN1Sequence result = bsnkUtil.retrievePipFromSignedPip(signedPip);
        assertEquals(Base64.getEncoder().encodeToString(result.getEncoded()), pipbase64);
    }

    @Test
    public void verifySignedPipTest() throws IOException, BsnkException {
        assertTrue(bsnkUtil.verifySignedPip(signedPip));
    }

    @Test
    public void verifySignedPipFailTest() throws IOException, BsnkException, InvalidKeySpecException,
            NoSuchAlgorithmException, NoSuchProviderException {
        String invalidUBase64 = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
        ECPublicKey invalidU = (ECPublicKey) PublicKeyParser.decodeKey(Base64.getDecoder().decode(invalidUBase64));
        ReflectionTestUtils.setField(bsnkUtil, "bsnkUPubkey", invalidU);

        assertFalse(bsnkUtil.verifySignedPip(signedPip));
    }

    @Test
    public void createPpPpcaRequest() throws IOException, BsnkException {
        String bsn = "PPPPPPPPP";
        ProvidePPPPCAOptimizedRequest result = bsnkUtil.createPpPpcaRequest(bsn);

        assertNotNull(result.getDateTime());
        assertEquals(DatatypeConstants.FIELD_UNDEFINED, result.getDateTime().getMillisecond());
        assertTrue(result.getRequestID().startsWith("DGD-"));
        assertNotNull(UUID.fromString(result.getRequestID().replaceAll("DGD-", "")));
        assertEquals(ReflectionTestUtils.getField(bsnkUtil, "digidMuKsv"), result.getRequesterKeySetVersion());
        assertEquals(ReflectionTestUtils.getField(bsnkUtil, "digidMuOin"), result.getRequester());
        assertEquals(bsn, result.getBSN());
    }
}
