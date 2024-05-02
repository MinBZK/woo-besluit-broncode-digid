
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

package nl.logius.digid.hsm.crypto.bsnk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLSequenceParser;
import org.bouncycastle.asn1.DLTaggedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CryptoServerAPI.CryptoServerException;
import nl.logius.bsnkpp.kmp.KMPMultiTransformRequest;
import nl.logius.bsnkpp.kmp.KMPRecipientTransformInfo;
import nl.logius.digid.hsm.crypto.BsnkAction;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.CryptoError;
import nl.logius.digid.hsm.exception.TransformException;
import nl.logius.digid.hsm.model.TransformOutput;
import nl.logius.digid.pp.PolyPseudoException;
import nl.logius.digid.pp.parser.Asn1Parser;

public class Transformation extends BsnkAction {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private byte[] polymorph;
    private String oid;

    private ASN1Integer schemeKeyVersion;
    private DERIA5String identityProvider;
    private ASN1Integer identityProviderKeySetVersion;

    private byte[] polymorphIdentity;
    private byte[] polymorphPseudonym;

    public Transformation(ConnectionPool pool, byte[] polymorph) {
        super(pool);
        this.polymorph = polymorph;
        checkPolymorph(false);
        this.polymorphIdentity = null;
        this.polymorphPseudonym = null;
    }

    public byte[] transformIdentity(String serviceProvider, int serviceProviderKeySetVersion, int targetMsgVersion) {
        final byte[] input = getPolymorphIdentity();
        try {
            return getConnection()
                    .transformPi(encode(input, serviceProvider, serviceProviderKeySetVersion, targetMsgVersion));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error transfoming PIP to PI", e);
            throw new CryptoError("Error transform to PI", e);
        }
    }

    public byte[] transformPseudonym(String serviceProvider, int serviceProviderKeySetVersion, int targetMsgVersion) {
        final byte[] input = getPolymorphPseudonym();
        try {
            return getConnection()
                    .transformPp(encode(input, serviceProvider, serviceProviderKeySetVersion, targetMsgVersion));
        } catch (IOException | CryptoServerException e) {
            setLastException(e);
            logger.error("Error transfoming PIP to PP", e);
            throw new CryptoError("Error transform to PP", e);
        }
    }

    public Map<String, TransformOutput> multipleTransform(ArrayList<KMPRecipientTransformInfo> recipients, int targetMsgVersion)
            throws TransformException {
        KMPMultiTransformRequest kmtr = new KMPMultiTransformRequest(getPolymorphIdentity(), getPolymorphPseudonym(),
                recipients);

        kmtr.setTargetMessageVersion(targetMsgVersion);

        byte[] asn1_out;
        try {
            asn1_out = getConnection().transformSignedPipEx(kmtr.encode());
        } catch (IOException e) {
            throw new TransformException(e);
        } catch (CryptoServerException e) {
            throw new TransformException(e);
        }
        return this.parseMultipleTransformASN1(asn1_out, recipients);
    }

    private Map<String, TransformOutput> parseMultipleTransformASN1(byte[] asn1_out,
            List<KMPRecipientTransformInfo> recipients) throws TransformException {

        DLSequence transformSequence;

        try (ASN1InputStream asn1_stream = new ASN1InputStream(asn1_out)) {
            transformSequence = (DLSequence) asn1_stream.readObject();
        } catch (IOException e) {
            throw new TransformException(e);
        }

        if (transformSequence.size() != recipients.size()) {
            throw new TransformException(
                    String.format("MultipleTranform output size (%d) doesn't match expected recipients size (%d)",
                            transformSequence.size(), recipients.size()));
        }

        final Map<String, TransformOutput> result = new HashMap<>(recipients.size());

        // The results sequences are in the same order as the recipients from the
        // TransformInput.
        // In order not to have to parse the ASN1 structure to find the OIN inside the
        // Polymorphic Sequence,
        // We get the OIN from the recipient at the same index
        for (int i = 0; i < transformSequence.size(); i++) {
            ASN1Encodable encodable = transformSequence.getObjectAt(i);
            byte[] octets;

            try {
                octets = encodable.toASN1Primitive().getEncoded();
            } catch (IOException e) {
                throw new TransformException(String.format("Could not parse sequence at index %d to ASN1", i), e);
            }

            TransformOutput output = new TransformOutput();

            if (recipients.get(i).getIdentifierFormat() == KMPRecipientTransformInfo.FRMT_IDENTIFIER) {
                output.setIdentity(octets);
            } else if (recipients.get(i).getIdentifierFormat() == KMPRecipientTransformInfo.FRMT_PSEUDO) {
                output.setPseudonym(octets);
            } else {
                throw new TransformException(String.format("No polymorphic identifier format found for recipient: '%s'",
                        recipients.get(i).getOin()));
            }

            result.put(recipients.get(i).getOin(), output);
        }

        return result;
    }

    private byte[] encode(byte[] input, String recipient, int recipientKeySetVersion, int targetMsgVersion) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(schemeKeyVersion);
            sequence.addObject(new DEROctetString(input));
            sequence.addObject(identityProvider);
            sequence.addObject(identityProviderKeySetVersion);
            sequence.addObject(new DERIA5String(recipient));
            sequence.addObject(new ASN1Integer(recipientKeySetVersion));
            sequence.addObject(new DLTaggedObject(false, 4, new ASN1Integer(targetMsgVersion)));
            sequence.close();
        } catch (IOException e) {
            throw new CryptoError("Could not encode request for HSM", e);
        }

        return buffer.toByteArray();
    }

    private void checkPolymorph(boolean recall) {
        try {
            Asn1Parser parser = new Asn1Parser(polymorph);
            oid = checkHeader(parser, null).getId();

            switch (oid) {
                case OID_PI:
                case OID_PP:
                case OID_PIP:
                    initTransform(parser);
                    break;

                case OID_SIGNED_PI:
                case OID_SIGNED_PP:
                case OID_SIGNED_PIP:
                    if (recall) {
                        throw new CryptoError("Nested signed error");
                    }
                    logger.info(String.format("Unsign polymorph %s", oid));
                    polymorph = unsign(polymorph);
                    checkPolymorph(true);
                    break;

                default:
                    throw new CryptoError(String.format("Unknown OID %s", oid));
            }
        } catch (IOException | PolyPseudoException e) {
            throw new CryptoError("Could not read polymorph", e);
        }
    }

    private void initTransform(Asn1Parser parser) throws IOException {
        parser.readObject(ASN1Integer.class);
        schemeKeyVersion = parser.readObject(ASN1Integer.class);
        parser.readObject(DERIA5String.class);
        identityProvider = parser.readObject(DERIA5String.class);
        identityProviderKeySetVersion = parser.readObject(ASN1Integer.class);
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Initialized HSM-transform: %s, SKV %s, IP <%s, %s>",
                    oid, schemeKeyVersion.getValue(), identityProvider.toString(),
                    identityProviderKeySetVersion.getValue()));
        }
    }

    private byte[] getPolymorphIdentity() {
        if (polymorphIdentity == null) {
            switch (oid) {
                case OID_PI:
                    polymorphIdentity = polymorph;
                    break;
                case OID_PIP:
                    polymorphIdentity = convertPipToPx(polymorph, false);
                    break;
                default:
                    throw new CryptoError("Cannot transform PP to EncryptedIdentity");
            }
        }
        return polymorphIdentity;
    }

    private byte[] getPolymorphPseudonym() {
        if (polymorphPseudonym == null) {
            switch (oid) {
                case OID_PP:
                    polymorphPseudonym = polymorph;
                    break;
                case OID_PIP:
                    polymorphPseudonym = convertPipToPx(polymorph, true);
                    break;
                default:
                    throw new CryptoError("Cannot transform PI to EncryptedPseudonym");
            }
        }
        return polymorphPseudonym;
    }

    private byte[] convertPipToPx(byte[] payload, boolean pseudonym) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            final Asn1Parser parser = new Asn1Parser(payload);
            // Read sequence and OID
            parser.readObject(DLSequenceParser.class);
            parser.readObject(ASN1ObjectIdentifier.class);

            final DERSequenceGenerator sequence = new DERSequenceGenerator(buffer);
            sequence.addObject(new ASN1ObjectIdentifier(pseudonym ? OID_PP : OID_PI));

            for (int i = 0; i < 5; i++) {
                sequence.addObject(parser.readObject());
            }
            final ASN1Encodable type = parser.readObject();
            if (pseudonym) {
                sequence.addObject(type);
            }

            final DLSequence pipPoints = (DLSequence) parser.readObject(DLSequenceParser.class).getLoadedObject();
            final ASN1Encodable[] points = new ASN1Encodable[] {
                    pipPoints.getObjectAt(0),
                    pipPoints.getObjectAt(pseudonym ? 2 : 1),
                    pipPoints.getObjectAt(pseudonym ? 4 : 3)
            };
            sequence.addObject(new DERSequence(points));

            sequence.close();
        } catch (IOException e) {
            throw new CryptoError(
                    String.format("Could not convert PIP to %s", pseudonym ? "PP" : "PI", e));
        }

        return buffer.toByteArray();
    }
}
