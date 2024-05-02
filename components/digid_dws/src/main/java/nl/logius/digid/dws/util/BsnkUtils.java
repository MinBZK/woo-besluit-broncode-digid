
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
import java.security.interfaces.ECPublicKey;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eid.gdi.nl._1_0.webservices.PolymorphicPseudonymType;
import eid.gdi.nl._1_0.webservices.ProvideDEPsRequest;
import eid.gdi.nl._1_0.webservices.ProvidePPPPCAOptimizedRequest;
import eid.gdi.nl._1_0.webservices.RelyingPartyType;
import nl.logius.digid.dws.exception.BsnkException;
import nl.logius.digid.pp.crypto.CryptoException;
import nl.logius.digid.pp.crypto.SignatureEcdsa;

public class BsnkUtils {

    private static final Logger logger = LoggerFactory.getLogger(BsnkUtils.class);

    private String digidMuOin;

    private BigInteger digidMuKsv;

    private ECPublicKey bsnkUPubkey;

    private BigInteger bsnkUKsv;

    private static final String PIP_OID = "2.16.528.1.1003.10.1.1.5";
    private static final String SIGNED_PIP_OID = "2.16.528.1.1003.10.1.1.6";

    public BsnkUtils(String digidMuOString, BigInteger digidMuKsv, ECPublicKey bsnkUPubkey, BigInteger bsnkUKsv) {
        this.digidMuOin = digidMuOString;
        this.digidMuKsv = digidMuKsv;
        this.bsnkUPubkey = bsnkUPubkey;
        this.bsnkUKsv = bsnkUKsv;

    }

    public ASN1Sequence retrievePipFromSignedPip(ASN1Sequence signedPip) throws BsnkException {
        ASN1Sequence signedPipContent = (ASN1Sequence) signedPip.getObjectAt(1);
        ASN1Sequence pip = (ASN1Sequence) signedPipContent.getObjectAt(0);

        ASN1ObjectIdentifier objectIdentifier = (ASN1ObjectIdentifier) pip.getObjectAt(0);
        if (!objectIdentifier.getId().equals(PIP_OID)) {
            throw new BsnkException("SignedPipNoPipFault",
                    String.format("Signed pip doesnt contain a pip. Expected identifier: '%s'. Found identifier: '%s'",
                            PIP_OID, objectIdentifier.toString()),
                    null);
        }

        return pip;
    }

    public ASN1Sequence signedPipFromPplist(List<PolymorphicPseudonymType> response) {
        for (PolymorphicPseudonymType polymorphicPseudonymType : response) {
            ASN1Sequence sequence;
            try {
                sequence = (ASN1Sequence) ASN1Sequence.fromByteArray(polymorphicPseudonymType.getValue());
            } catch (Exception e) {
                logger.error(String.format("PolymorphicPseudonymType not a valid ASN1 Sequence. Exception: '%s'",
                        e.getMessage()));
                continue;
            }
            if (sequence.getObjectAt(0) instanceof ASN1ObjectIdentifier) {
                ASN1ObjectIdentifier objectIdentifier = (ASN1ObjectIdentifier) sequence.getObjectAt(0);
                if (objectIdentifier.getId().equals(SIGNED_PIP_OID)) {
                    return sequence;
                }
            }
        }
        throw new IllegalArgumentException("No signed pip found in PolymorphicPseudonymType list");
    }

    public boolean verifySignedPip(ASN1Sequence signedPip) throws BsnkException {
        ASN1Sequence signedPipContent = (ASN1Sequence) signedPip.getObjectAt(1);
        ASN1Sequence pipSignatureSequence = (ASN1Sequence) signedPip.getObjectAt(2);
        ASN1Sequence pipSignature = (ASN1Sequence) pipSignatureSequence.getObjectAt(1);

        byte[] pipBytes;
        try {
            pipBytes = signedPipContent.getEncoded();
        } catch (IOException ex) {
            throw new BsnkException("SignedPipIOFault", "Failed to get byte[] from pip or pip signature.", ex);
        }

        BigInteger pipKsv = ((ASN1Integer) signedPipContent.getObjectAt(2)).getValue();

        if (!pipKsv.equals(bsnkUKsv)) {
            throw new BsnkException("SignedpipKsvMismatch",
                    String.format("Signedpip ksv mismatch. U: '%s'. Pip: '%s'", bsnkUKsv, pipKsv), null);
        }

        String oid = ((ASN1ObjectIdentifier) pipSignatureSequence.getObjectAt(0)).getId();
        BigInteger r = ((ASN1Integer) pipSignature.getObjectAt(0)).getValue();
        BigInteger s = ((ASN1Integer) pipSignature.getObjectAt(1)).getValue();
        SignatureEcdsa signature = (SignatureEcdsa) SignatureEcdsa.from(oid, r, s);

        try {
            signature.verify(bsnkUPubkey, pipBytes);
            return true;
        } catch (CryptoException ex) {
            logger.error(String.format("Exception during pip verification: '%s", ex.getMessage()));
            return false;
        }
    }

    public ProvidePPPPCAOptimizedRequest createPpPpcaRequest(String bsn) throws BsnkException {
        ProvidePPPPCAOptimizedRequest request = new ProvidePPPPCAOptimizedRequest();

        request.setDateTime(getDateTime());
        request.setRequestID("DGD-" + UUID.randomUUID().toString());
        request.setRequester(digidMuOin);
        request.setRequesterKeySetVersion(digidMuKsv);
        request.setBSN(bsn);

        return request;
    }

    public ProvideDEPsRequest createProvideDEPsRequest(String bsn, String oin, BigInteger ksv) throws BsnkException {
        ProvideDEPsRequest request = new ProvideDEPsRequest();

        request.setDateTime(getDateTime());
        request.setRequestID("DGD-" + UUID.randomUUID().toString());
        request.setRequester(digidMuOin);
        request.setBSN(bsn);
        RelyingPartyType relyingParty = new RelyingPartyType();
        relyingParty.setEntityID(oin);
        relyingParty.setKeySetVersion(ksv);
        request.getRelyingParties().add(relyingParty);

        return request;
    }

    private XMLGregorianCalendar getDateTime() throws BsnkException {
        XMLGregorianCalendar xmlCalender;
        try {
            xmlCalender = DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        } catch (DatatypeConfigurationException ex) {
            throw new BsnkException("xmlDatetimestampFault", "Couldn't create xml datetimestamp", ex);
        }
        xmlCalender.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        return xmlCalender;
    }
}
