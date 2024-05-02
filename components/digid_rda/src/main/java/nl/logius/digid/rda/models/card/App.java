
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
import nl.logius.digid.card.apdu.AESSecureMessaging;
import nl.logius.digid.card.apdu.SecureMessaging;
import nl.logius.digid.card.apdu.SecureMessagingException;
import nl.logius.digid.card.apdu.TDEASecureMessaging;
import nl.logius.digid.card.asn1.Asn1InputStream;
import nl.logius.digid.card.asn1.models.DataGroup14;
import nl.logius.digid.card.asn1.models.LdsSecurityObject;
import nl.logius.digid.card.crypto.CryptoUtils;
import nl.logius.digid.card.crypto.DigestUtils;
import nl.logius.digid.rda.apdu.ApduFactory;
import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.models.DocumentType;
import nl.logius.digid.rda.models.KDF;
import nl.logius.digid.rda.models.MrzInfo;
import nl.logius.digid.rda.models.PaceCrypto;
import nl.logius.digid.rda.models.Protocol;
import nl.logius.digid.rda.models.RdaError;
import nl.logius.digid.rda.service.CardVerifier;
import nl.logius.digid.rda.service.RandomFactory;
import org.bouncycastle.asn1.ASN1ApplicationSpecific;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class App {
    public static final int BAC_MAX_RESPONSE_SIZE = 231;
    public static final int PACE_MAX_RESPONSE_SIZE = 223;
    private static final Map<Integer, String> protocolMap = new HashMap<>();

    static {
        protocolMap.put(8, "secp192r1");
        protocolMap.put(9, "BrainpoolP192r1");
        protocolMap.put(10, "secp224r1");
        protocolMap.put(11, "BrainpoolP224r1");
        protocolMap.put(12, "secp256r1");
        protocolMap.put(13, "BrainpoolP256r1");
        protocolMap.put(14, "BrainpoolP320r1");
        protocolMap.put(15, "secp384r1");
        protocolMap.put(16, "BrainpoolP384r1");
        protocolMap.put(17, "BrainpoolP512r1");
        protocolMap.put(18, "secp521r1");
    }

    public final Session session;
    protected final CardVerifier verifier;
    protected final RandomFactory randomFactory;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected SecureMessaging sm;

    protected App(Session session, CardVerifier verifier, RandomFactory random) {
        this.session = session;
        this.verifier = verifier;
        this.randomFactory = random;
    }

    public static App fromSession(App.Session session, CardVerifier verifier, RandomFactory random) {
        return switch (session.documentType) {
            case DRIVING_LICENCE -> new DrivingLicenceApp(session, verifier, random);
            case TRAVEL_DOCUMENT -> new TravelDocumentApp(session, verifier, random);
            default -> throw new BadRequestException("Document type is not selected");
        };
    }

    protected static CommandAPDU selectApp(byte[] aid) {
        return new CommandAPDU(0, (byte) 0xa4, 4, (byte)0x0c, aid, 0x100);
    }

    public static Map<DocumentType, CommandAPDU> getSelects(Session session) {
        final ImmutableMap.Builder<DocumentType, CommandAPDU> selects = ImmutableMap.builder();
        if (!session.drivingLicences.isEmpty()) {
            selects.put(DocumentType.DRIVING_LICENCE, selectApp(DrivingLicenceApp.AID));
        }
        if (!session.travelDocuments.isEmpty()) {
            selects.put(DocumentType.TRAVEL_DOCUMENT, selectApp(TravelDocumentApp.AID));
        }
        return selects.build();
    }

    public static CommandAPDU getSelect(DocumentType type, Protocol protocol) {
        if (type == DocumentType.DRIVING_LICENCE && protocol != Protocol.PACE) {
            return selectApp(DrivingLicenceApp.AID);
        } else if (type == DocumentType.TRAVEL_DOCUMENT) {
            return selectApp(TravelDocumentApp.AID);
        }

        return null;
    }

    private static CommandAPDU mutualAuthenticate(byte[] encrypted, byte[] mac) {
        final byte[] input = Arrays.concatenate(encrypted, mac);
        return new CommandAPDU(0, 0x82, 0, 0, input, input.length);
    }

    private static CommandAPDU internalAuthenticate(byte[] random) {
        return new CommandAPDU(0, 0x88, 0, 0, random, 0x100);
    }

    private static CommandAPDU selectFile(byte[] fileId) {
        return new CommandAPDU(0, 0xa4, 2, 0x0c, fileId, 0);
    }

    private static CommandAPDU readBinary(int offset) {
        return new CommandAPDU(0, 0xb0, offset >>> 8, offset & 0xff, 0x100);
    }

    public void setChallenge(ResponseAPDU response) throws RdaException {
        checkResponse(response, RdaError.CHALLENGE);
        session.challenge = response.getData();
    }

    public CommandAPDU decryptNonce(ResponseAPDU response) throws RdaException {
        if (!session.nextDocument()) {
            throw new RdaException(RdaError.AUTHENTICATE, "No more documents");
        }

        var mrzSeed = getSeedMrz().getBytes(StandardCharsets.US_ASCII);

        if (session.documentType == DocumentType.TRAVEL_DOCUMENT)
            mrzSeed = DigestUtils.digest("SHA1").digest(mrzSeed);

        final var kpi = KDF.derivePI(mrzSeed);

        final var authTemplate = ASN1ApplicationSpecific.getInstance(response.getData());
        final var authObject = ASN1TaggedObject.getInstance(authTemplate.getContents());
        final var encryptedNonce = ASN1OctetString.getInstance(authObject.getObject()).getOctets();

        final var nonce = PaceCrypto.decryptNonce(kpi, encryptedNonce);

        session.setNonce(nonce);

        final var params = ECNamedCurveTable.getParameterSpec(session.getPaceParameterId());
        final var privateKeyTerminal = (ECPrivateKeyParameters) randomFactory.generatePrivateKey(params);
        final var publicKeyTerminal = params.getG().multiply(privateKeyTerminal.getD()).normalize();

        session.setEncodedPrivateKey(privateKeyTerminal.getD());

        final var xCoord = publicKeyTerminal.getAffineXCoord().getEncoded();
        final var yCoord = publicKeyTerminal.getAffineYCoord().getEncoded();

        return ApduFactory.getMappedNonce(Bytes.concat(xCoord, yCoord));
    }

    public CommandAPDU getAuthenticate() throws RdaException {
        if (!session.nextDocument()) {
            throw new RdaException(RdaError.AUTHENTICATE, "No more documents");
        }

        session.seed = DigestUtils.digest("SHA1").digest(getSeedMrz().getBytes(StandardCharsets.US_ASCII));

        final byte[] kIfd = CryptoUtils.adjustParity(randomFactory.next(16));
        final byte[] rndIfd = randomFactory.next(8);
        final byte[] rndIcc = session.challenge;
        session.ssc = new byte[8];
        for (var i = 0; i < 4; i++) {
            session.ssc[i] = rndIcc[4 + i];
            session.ssc[4 + i] = rndIfd[4 + i];
        }

        final SecureMessaging secureMessaging = new TDEASecureMessaging(session.seed, 0, 16, null);
        final byte[] eIfd = secureMessaging.encrypt(false, true, rndIfd, rndIcc, kIfd);
        final byte[] mIfd = secureMessaging.mac(m -> m.update(eIfd));

        if (logger.isDebugEnabled()) {
            logger.debug("kSeed: {}", Hex.toHexString(session.seed));
            logger.debug("kIfd: {}", Hex.toHexString(kIfd));
            logger.debug("rndIfd: {}", Hex.toHexString(rndIfd));
            logger.debug("rndIcc: {}", Hex.toHexString(rndIcc));
            logger.debug("ssc: {}", Hex.toHexString(session.ssc));
            logger.debug("eIfd: {}", Hex.toHexString(eIfd));
            logger.debug("mIfd: {}", Hex.toHexString(mIfd));
        }
        session.intermediate = kIfd;

        return mutualAuthenticate(eIfd, mIfd);
    }

    public void verifyAuthenticate(ResponseAPDU response) throws RdaException {
        checkResponse(response, RdaError.AUTHENTICATE);
        final byte[] result = verifier.verifyAuthenticate(session.seed, response.getData());
        final byte[] kIfd = session.intermediate;
        final byte[] kIcc = Arrays.copyOfRange(result, result.length - 16, result.length);
        session.seed = CryptoUtils.xor(kIfd, kIcc);

        if (logger.isDebugEnabled()) {
            logger.debug("kIfd: {}", Hex.toHexString(kIfd));
            logger.debug("kIcc: {}", Hex.toHexString(kIcc));
            logger.debug("kSeed: {}", Hex.toHexString(session.seed));
        }
        sm = new TDEASecureMessaging(session.seed, session.ssc);
        session.nextStep();
    }

    public void verifyToken(ResponseAPDU response) throws RdaException {
        final var authTemplate = ASN1ApplicationSpecific.getInstance(response.getData());
        final var authObject = ASN1TaggedObject.getInstance(authTemplate.getContents());
        final var tic = ASN1OctetString.getInstance(authObject.getObject()).getOctets();

        // verify tic

        session.ssc = new byte[16];
        sm = new AESSecureMessaging(session.getkEnc(), session.getkMac(), session.ssc);

        if (logger.isDebugEnabled()) {
            logger.debug("kEnc: {}", Hex.toHexString(session.getkEnc()));
            logger.debug("kMac: {}", Hex.toHexString(session.getkMac()));
            logger.debug("ssc: {}", Hex.toHexString(session.ssc));
            logger.debug("tic: {}", Hex.toHexString(tic));
        }

        session.nextStep();
    }

    public List<CommandAPDU> process(List<ResponseAPDU> responses) throws RdaException {
        logger.info("Step {}", session.step);

        if (session.paceParameterId != null) {
            sm = new AESSecureMessaging(session.getkEnc(), session.getkMac(), session.ssc);
        } else {
            sm = new TDEASecureMessaging(session.seed, session.ssc);
        }

        if (session.step == Step.AA) {
            if (responses.size() != 1) {
                throw new RdaException(RdaError.ACTIVE_AUTHENTICATION, "None or multiple responses");
            }
            final byte[] result = decrypt(responses.get(0), RdaError.ACTIVE_AUTHENTICATION);
            final AaPublicKey publicKey = verifier.verifyDataGroup(
                session.com, session.ldsSecurityObject, session.aaPublicKey, AaPublicKey.class);
            final MessageDigest digest = session.aaAlgorithm == null ? defaultAaDigest(publicKey) :
                DigestUtils.digest(new ASN1ObjectIdentifier(session.aaAlgorithm));
            verifier.verifyActiveAuthentication(publicKey, digest, session.intermediate, result);
            session.ssc = sm.getSsc();
        } else {
            final byte[] file;
            List<CommandAPDU> requests;
            if (session.intermediate == null) {
                requests = readFileRemaining(responses);
                if (!requests.isEmpty()) {
                    return requests;
                }
                file = compileFile(Collections.emptyList());
            } else {
                file = compileFile(responses);
            }
            processFile(file);
        }
        session.nextStep();
        return next();
    }

    public List<CommandAPDU> next() {
        if (session.step == Step.END) {
            // sonarqube: return empty list, test fails
            return null;
        }

        if (session.step == Step.DG14 && !session.com.getDataGroups().contains(14)) {
            logger.info("Skipping data group 14");
            session.nextStep();
        }

        if (session.step == Step.AA) {
            session.intermediate = randomFactory.next(8);
            if (logger.isDebugEnabled()) {
                logger.debug("AA challenge: {}", Hex.toHexString(session.intermediate));
            }
            return List.of(encrypt(internalAuthenticate(session.intermediate)));
        } else {
            return readFileFirst();
        }
    }

    protected void processFile(byte[] file) throws RdaException {
        switch (session.step) {
            case COM:
                session.com = verifier.verifyCom(file, getComClass());
                break;
            case SOD:
                session.ldsSecurityObject = verifier.verifySOd(session.com, file);
                break;
            case AA_PUBLIC_KEY:
                session.aaPublicKey = file;
                break;
            case DG14:
                final String algorithm = verifier.verifyDataGroup(
                    session.com, session.ldsSecurityObject, file, DataGroup14.class
                ).getSecurityInfos().getAaAlgorithm();
                if (algorithm != null) {
                    session.aaAlgorithm = algorithm;
                }
                break;
            default:
                throw new IllegalStateException("Invalid file for step " + session.step);
        }
    }

    protected abstract String getSeedMrz() throws RdaException;

    protected abstract Class<? extends COM> getComClass();

    protected abstract Map<Step, byte[]> getFileIds();

    protected abstract MessageDigest defaultAaDigest(AaPublicKey publicKey);

    private void checkResponse(ResponseAPDU response, RdaError error, boolean failFast) throws RdaException {
        if (failFast && response.getSW() != 0x9000) {
            throw new RdaException(error, "Unexpected status %04x", response.getSW());
        }
    }

    private void checkResponse(ResponseAPDU response, RdaError error) throws RdaException {
        checkResponse(response, error, true);
    }


    private byte[] decrypt(ResponseAPDU response, RdaError error) throws RdaException {
        return decrypt(response, error, true);
    }

    private byte[] decrypt(ResponseAPDU response, RdaError error, boolean failFast) throws RdaException {
        final ResponseAPDU decrypted;
        try {
            decrypted = sm.decrypt(response);
        } catch (SecureMessagingException e) {
            throw new RdaException(RdaError.SECURE_MESSAGING, "Secure messaging failed", e);
        }
        sm.next();
        checkResponse(response, error, failFast);
        return decrypted.getData();
    }



    private CommandAPDU encrypt(CommandAPDU request) {
        final CommandAPDU encrypted = sm.encrypt(request);
        sm.next();
        return encrypted;
    }

    private List<CommandAPDU> readFileFirst() {
        final byte[] fileId = getFileIds().get(session.step);

        if (session.documentType == DocumentType.DRIVING_LICENCE && session.getProtocol() == Protocol.PACE) {
            return List.of(
                encrypt(selectApp(DrivingLicenceApp.AID)),
                encrypt(selectFile(fileId)),
                encrypt(readBinary(0))
            );
        } else if (session.documentType == DocumentType.TRAVEL_DOCUMENT && session.getProtocol() == Protocol.PACE) {
            return List.of(
                encrypt(selectApp(TravelDocumentApp.AID)),
                encrypt(selectFile(fileId)),
                encrypt(readBinary(0))
            );
        }
        else {
             return List.of(
                encrypt(selectFile(fileId)),
                encrypt(readBinary(0))
            );
        }
    }

    private List<CommandAPDU> readFileRemaining(List<ResponseAPDU> responses) throws RdaException {
        final var maxResponseSize = session.getProtocol() == Protocol.PACE ? PACE_MAX_RESPONSE_SIZE : BAC_MAX_RESPONSE_SIZE;

        for (final ResponseAPDU response : responses) {
            session.intermediate = decrypt(response, RdaError.READ_FILE, false);
        }
        session.ssc = sm.getSsc();

        // Determine length from start of ASN1 structure
        final int length;
        int offset;
        try (final var is = new Asn1InputStream(session.intermediate)) {
            is.readTag();
            final int objLength = is.readLength();
            length = objLength + is.offset();
            offset = is.total();
        }

        final int reads = (length - 1) / maxResponseSize;
        final ImmutableList.Builder<CommandAPDU> builder = ImmutableList.builderWithExpectedSize(reads);
        for (var i = 0; i < reads; i++) {
            builder.add(encrypt(readBinary(offset)));
            offset += maxResponseSize;
        }
        return builder.build();
    }

    private byte[] compileFile(List<ResponseAPDU> responses) throws RdaException {
        try (final var bos = new ByteArrayOutputStream()) {
            bos.write(session.intermediate);
            for (final ResponseAPDU response : responses) {
                bos.write(decrypt(response, RdaError.READ_FILE));
            }
            session.ssc = sm.getSsc();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IO exception");
        }
    }

    public CommandAPDU mapNonce(byte[] data) {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec(session.getPaceParameterId());
        final var authTemplate = ASN1ApplicationSpecific.getInstance(data);
        final var authObject = ASN1TaggedObject.getInstance(authTemplate.getContents());

        final var chipPublicData = ASN1OctetString.getInstance(authObject.getObject()).getOctets();
        final var chipPublicPoint = params.getCurve().decodePoint(chipPublicData);

        final var sharedSecret = new BigInteger(1, session.getNonce());
        final var privateKeyTerminal = session.getEncodedPrivateKey();
        final var h = chipPublicPoint.multiply(privateKeyTerminal).normalize();
        final var gMapped = params.getG().multiply(sharedSecret).add(h).normalize();

        final var normalizedPoint = gMapped.multiply(privateKeyTerminal).normalize();
        final var xCoord = normalizedPoint.getAffineXCoord().getEncoded();
        final var yCoord = normalizedPoint.getAffineYCoord().getEncoded();

        return ApduFactory.performKeyAgreement(Bytes.concat(xCoord, yCoord));
    }

    public CommandAPDU keyAgree(byte[] data) {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec(session.getPaceParameterId());
        final var authTemplate = ASN1ApplicationSpecific.getInstance(data);
        final var authObject = ASN1TaggedObject.getInstance(authTemplate.getContents());

        final var chipPublicData = ASN1OctetString.getInstance(authObject.getObject()).getOctets();
        final var chipPublicPoint = params.getCurve().decodePoint(chipPublicData);

        final var privateKeyTerminal = session.getEncodedPrivateKey();
        final var k = chipPublicPoint.multiply(privateKeyTerminal).normalize().getXCoord().getEncoded();
        final var kEnc = KDF.deriveEnc(k);
        final var kMac = KDF.deriveMAC(k);

        session.setkEnc(kEnc);
        session.setkMac(kMac);

        final var tifd = PaceCrypto.getTerminalTokenFromPublicKey(chipPublicPoint, kMac);

        return ApduFactory.mutualAuth(tifd);
    }

    public static class Session implements Serializable {
        private static final long serialVersionUID = 1L;
        private String aaAlgorithm;
        private byte[] aaPublicKey;
        private String bsn;
        private byte[] challenge;
        private COM com;
        private String documentNumber;
        private DocumentType documentType;
        private List<String> drivingLicences;
        private BigInteger encodedPrivateKey;
        private byte[] intermediate;
        private byte[] kEnc;
        private byte[] kMac;
        private LdsSecurityObject ldsSecurityObject;
        private String mrzIdentifier;
        private byte[] nonce;
        private String paceParameterId;
        private Protocol protocol;
        private byte[] seed;
        private int selectedDocument;
        private byte[] ssc;
        private Step step;
        private List<MrzInfo> travelDocuments;

        public Session() {
            selectedDocument = -1;
            step = null;
        }

        public Session(Session session) {
            this();
            drivingLicences = session.drivingLicences;
            travelDocuments = session.travelDocuments;
        }

        public String getDocumentType() {
            return this.documentType.toString();
        }

        public void setDocumentType(DocumentType documentType) {
            this.documentType = documentType;
        }

        public void setDrivingLicences(List<String> drivingLicences) {
            this.drivingLicences = ImmutableList.copyOf(drivingLicences);
        }

        public void setTravelDocuments(List<MrzInfo> travelDocuments) {
            this.travelDocuments = ImmutableList.copyOf(travelDocuments);
        }

        public boolean nextDocument() {
            selectedDocument++;
            return switch (documentType) {
                case DRIVING_LICENCE -> selectedDocument < drivingLicences.size();
                case TRAVEL_DOCUMENT -> selectedDocument < travelDocuments.size();
                case ID_CARD -> selectedDocument < travelDocuments.size();
                case PASSPORT -> selectedDocument < travelDocuments.size();
                default -> false;
            };
        }

        public String selectedDrivingLicence() {
            return drivingLicences.get(selectedDocument);
        }

        public MrzInfo selectedTravelDocument() {
            return travelDocuments.get(selectedDocument);
        }

        public Step getStep() {
            return step;
        }

        private void nextStep() {
            step = step == null ? Step.COM : step.next();
            intermediate = null;
        }

        public COM getCom() {
            return com;
        }

        public LdsSecurityObject getLdsSecurityObject() {
            return ldsSecurityObject;
        }

        public String getBsn() {
            return bsn;
        }

        public void setBsn(String bsn) {
            this.bsn = bsn;
        }

        public byte[] getNonce() {
            return nonce;
        }

        public void setNonce(byte[] nonce) {
            this.nonce = nonce;
        }

        public String getPaceParameterId() {
            return paceParameterId;
        }

        public void setPaceParameterId(int paceParameterId) {
            this.paceParameterId = protocolMap.get(paceParameterId);
        }

        public BigInteger getEncodedPrivateKey() {
            return encodedPrivateKey;
        }

        public void setEncodedPrivateKey(BigInteger encodedPrivateKey) {
            this.encodedPrivateKey = encodedPrivateKey;
        }

        public byte[] getkEnc() {
            return kEnc;
        }

        public void setkEnc(byte[] kEnc) {
            this.kEnc = kEnc;
        }

        public byte[] getkMac() {
            return kMac;
        }

        public void setkMac(byte[] kMac) {
            this.kMac = kMac;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public void setProtocol(Protocol protocol) {
            this.protocol = protocol;
        }

        public String getMrzIdentifier() {
            return mrzIdentifier;
        }

        public void setMrzIdentifier(String mrzIdentifier) {
            this.mrzIdentifier = mrzIdentifier;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }

        public void setDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
        }
    }
}
