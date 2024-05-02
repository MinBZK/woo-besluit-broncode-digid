
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

package nl.logius.digid.dws.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;

import eid.gdi.nl._1_0.webservices.DirectEncryptedPseudonymType;
import eid.gdi.nl._1_0.webservices.ProvideDEPsRequest;
import nl.logius.digid.dws.client.ProvideDepBsnkClient;
import nl.logius.digid.dws.model.BsnkProvideDepResponse;
import org.bouncycastle.asn1.ASN1Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import eid.gdi.nl._1_0.webservices.PolymorphicPseudonymType;
import eid.gdi.nl._1_0.webservices.ProvidePPPPCAOptimizedRequest;
import nl.logius.digid.dws.client.ActivateBsnkClient;
import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.dws.util.BsnkUtils;

@Service
public class BsnkService {
    private ActivateBsnkClient activateBsnkClient;
    private ProvideDepBsnkClient provideDepBsnkClient;
    private final BsnkUtils bsnkUtils;

    @Autowired
    public BsnkService(BsnkUtils bsnkUtils, ActivateBsnkClient activateBsnkClient, ProvideDepBsnkClient provideDepBsnkClient) {
        this.bsnkUtils = bsnkUtils;
        this.activateBsnkClient = activateBsnkClient;
        this.provideDepBsnkClient = provideDepBsnkClient;
    }

    public String bsnkActivate(String bsn) throws BsnkException {
        ProvidePPPPCAOptimizedRequest request = bsnkUtils.createPpPpcaRequest(bsn);
        List<PolymorphicPseudonymType> response;

        response = activateBsnkClient.providePPRequest(request);

        ASN1Sequence signedPip = bsnkUtils.signedPipFromPplist(response);

        if (!bsnkUtils.verifySignedPip(signedPip)) {
            try {
                throw new BsnkException("SignedpipSignatureFault", String.format("Signed pip not verified: '%s'",
                        Base64.getEncoder().encodeToString(signedPip.getEncoded())), null);
            } catch (IOException ex) {
                throw new BsnkException("signedPipSignatureEncodeFault",
                        "Signed pip not verified and not not base64 encodeable", ex);
            }
        }

        ASN1Sequence pip = bsnkUtils.retrievePipFromSignedPip(signedPip);

        try {
            return Base64.getEncoder().encodeToString(pip.getEncoded());
        } catch (IOException ex) {
            throw new BsnkException("PipEncodeFault", "Couldn't base64 encode pip", ex);
        }
    }

    public BsnkProvideDepResponse bsnkProvideDep(String bsn, String oin, BigInteger ksv) throws BsnkException {
        ProvideDEPsRequest request = bsnkUtils.createProvideDEPsRequest(bsn, oin, ksv);
        List<DirectEncryptedPseudonymType> list = provideDepBsnkClient.provideDep(request);

        BsnkProvideDepResponse response = new BsnkProvideDepResponse();
        response.setDep(Base64.getEncoder().encodeToString(list.get(0).getValue()));
        response.setStatus("OK");

        return response;
    }
}
