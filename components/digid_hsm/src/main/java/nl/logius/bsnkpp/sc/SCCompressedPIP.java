
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.logius.bsnkpp.sc;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

/*PPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP*/
public class SCCompressedPIP {
    public byte [] point_0 = null;
    public byte [] point_1 = null;
    public byte [] point_2 = null;

    byte[] PIPB4; // received from RDW/RvIG static for the same schemeVersion and schemeKeyVersion (IPP / Y)
    byte[] PIPB5; // received from RDW/RvIG static for the same schemeVersion and schemeKeyVersion (PPP / Z)
    
    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("brainpoolp320r1");
    ECCurve B320 = spec.getCurve();
    ECPoint Generator = spec.getG();
    BigInteger qB = B320.getOrder();
    BigInteger one = BigInteger.valueOf(1L);
    BigInteger qB_minus_1 = qB.subtract(one);
    
    public void setIPP(byte[] ipp)
    {
        PIPB4 = ipp;
    }
    
    public void setPPP(byte[] ppp)
    {
        PIPB5 = ppp;
    }
    
     public byte[][] getPointsPI() { //conversion to 3 point PI format as expected by BSNk
        byte[][] result = new byte[3][81]; // 3 uncompressed format EC points
        
        ECPoint[] IEP = new ECPoint[3];   // intermediary 
        
        IEP[0] = B320.decodePoint(point_0); // convert to point regardless of compressed or not
        IEP[1] = B320.decodePoint(point_1); // convert to point regardless of compressed or not 
        IEP[2] = B320.decodePoint(PIPB4);  // convert to point regardless of compressed or not

        result[0] = IEP[0].getEncoded(false);                               // convert to uncompressed
        result[1] = IEP[1].getEncoded(false); // convert to uncompressed
        result[2] = IEP[2].getEncoded(false);   // convert to compressed
        return result;
    }
     
     
     public byte[][] getPointsPP() { //conversion to 3 point PP format as expected by BSNk
        byte[][] result = new byte[3][81]; // 3 uncompressed format EC points
        ECPoint[] IEP = new ECPoint[3]; // intermediary 

        IEP[0] = B320.decodePoint(point_0); // convert to point regardless of compressed or not
        IEP[1] = B320.decodePoint(point_2); // convert to point regardless of compressed or not 
        IEP[2] = B320.decodePoint(PIPB5);  // convert to point regardless of compressed or not

        result[0] = IEP[0].getEncoded(false);                               // convert to uncompressed
        result[1] = IEP[1].getEncoded(false); // convert to uncompressed
        result[2] = IEP[2].getEncoded(false);   // convert to compressed
        return result;
    }
     
     public byte[][] getPointsPIP() { //conversion to 5 point PIP format as used by BSNk
        byte[][] result = new byte[5][81]; // 3 uncompressed format EC points
        ECPoint[] IEP = new ECPoint[5];   // intermediary 

        IEP[0] = B320.decodePoint(point_0); // convert to point regardless of compressed or not
        IEP[1] = B320.decodePoint(point_1); // convert to point regardless of compressed or not 
        IEP[2] = B320.decodePoint(point_2);  // convert to point regardless of compressed or not
        IEP[3] = B320.decodePoint(PIPB4);  // convert to point regardless of compressed or not
        IEP[4] = B320.decodePoint(PIPB5);  // convert to point regardless of compressed or not

        result[0] = IEP[0].getEncoded(false);   // convert to uncompressed
        result[1] = IEP[1].getEncoded(false); // convert to uncompressed
        result[2] = IEP[2].getEncoded(false);   // convert to compressed
        result[3] = IEP[3].getEncoded(false);   // convert to compressed
        result[4] = IEP[4].getEncoded(false);   // convert to compressed

        return result;
    }
     
     
    public static SCCompressedPIP decode(byte[] encoded) throws IOException, Exception {
        SCCompressedPIP pip = new SCCompressedPIP();

        ASN1InputStream parser = new ASN1InputStream(encoded);

        DERApplicationSpecific das = (DERApplicationSpecific)parser.readObject();
        
        int tag = das.getApplicationTag();
        
        ASN1InputStream ais = new ASN1InputStream(das.getContents());
        
        ASN1ObjectIdentifier _oid = (ASN1ObjectIdentifier)ais.readObject();
        DLTaggedObject dto = (DLTaggedObject) ais.readObject();
        
        DLSequence dto_seq = (DLSequence) dto.getObjectParser(0, true);
        
        DLTaggedObject dto_pt_0 = (DLTaggedObject)dto_seq.getObjectAt(0);
        DLTaggedObject dto_pt_1 = (DLTaggedObject)dto_seq.getObjectAt(1);
        DLTaggedObject dto_pt_2 = (DLTaggedObject)dto_seq.getObjectAt(2);
 
        pip.point_0 = ((DEROctetString)(dto_pt_0.getObject()).toASN1Primitive()).getOctets();
        pip.point_1 = ((DEROctetString)(dto_pt_1.getObject()).toASN1Primitive()).getOctets();
        pip.point_2 = ((DEROctetString)(dto_pt_2.getObject()).toASN1Primitive()).getOctets();
  
        return pip;
    }
}
