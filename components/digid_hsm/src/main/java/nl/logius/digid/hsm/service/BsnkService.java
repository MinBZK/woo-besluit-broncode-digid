
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

package nl.logius.digid.hsm.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.BaseEncoding;

import nl.logius.digid.hsm.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import nl.logius.bsnkpp.kmp.KMPRecipientTransformInfo;
import nl.logius.digid.hsm.crypto.bsnk.Activation;
import nl.logius.digid.hsm.crypto.bsnk.CardActivation;
import nl.logius.digid.hsm.crypto.bsnk.Transformation;
import nl.logius.digid.hsm.crypto.bsnk.VerificationPoints;
import nl.logius.digid.hsm.cryptoserver.ConnectionPool;
import nl.logius.digid.hsm.exception.TransformException;

@Service
public class BsnkService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionPool pool;

    public Map<String, TransformOutput> transform(TransformInput input) {
        final Map<String, TransformOutput> result = new HashMap<>(input.getRequests().size());

        logger.info("Transforming {}", encodeForLog(input.getPolymorph()));
        try (final Transformation transformation = new Transformation(pool, input.getPolymorph())) {
            for (Map.Entry<String, TransformOptions> entry : input.getRequests().entrySet()) {
                final TransformOutput output = transform(transformation, entry.getKey(), entry.getValue(),
                        input.getTargetMsgVersion());
                logger.info("For <{}, {}> to <{},{}>", entry.getKey(), entry.getValue().getKsv(),
                        encodeForLog(output.getIdentity()), encodeForLog(output.getPseudonym()));
                result.put(entry.getKey(), output);
            }
        } catch (IOException e) {
            logger.error("Unexpected IO error", e);
            throw new RuntimeException("Unexpected IO error", e);
        }

        return result;
    }

    public Map<String, TransformOutput> multipleTransform(MultipleTransformRequest input) throws TransformException {
        try (Transformation transformation = new Transformation(pool, input.getPolymorph())) {
            ArrayList<KMPRecipientTransformInfo> recipients = recipientListFromTransformInput(input);
            return transformation.multipleTransform(recipients, input.getTargetMsgVersion());
        } catch (IOException e) {
            throw new TransformException("HSM resource error during multipleTransform", e);
        }
    }

    private ArrayList<KMPRecipientTransformInfo> recipientListFromTransformInput(MultipleTransformRequest transformInput)
            throws TransformException {
        ArrayList<KMPRecipientTransformInfo> linkedServiceProviders = new ArrayList();

        for (Map.Entry<String, TransformOptions> entry : transformInput.getRequests().entrySet()) {
            KMPRecipientTransformInfo recipient = new KMPRecipientTransformInfo();
            recipient.setOin(entry.getKey());
            recipient.setKeySetVersion(entry.getValue().getKsv());
            int identifierFormat;

            if (entry.getValue().isIdentity() && entry.getValue().isPseudonym()) {
                throw new TransformException(
                        String.format("Both VI and VP polymorphic identifier format found for %s", recipient.getOin()));
            } else if (entry.getValue().isIdentity()) {
                identifierFormat = KMPRecipientTransformInfo.FRMT_IDENTIFIER;
            } else if (entry.getValue().isPseudonym()) {
                identifierFormat = KMPRecipientTransformInfo.FRMT_PSEUDO;
            } else {
                throw new TransformException(
                        String.format("No polymorphic identifier format found for %s", recipient.getOin()));
            }

            recipient.setIdentifierFormat(identifierFormat);

            recipient.includeLinks(entry.getValue().isIncludeLinks());

            linkedServiceProviders.add(recipient);
        }
        return linkedServiceProviders;
    }

    private TransformOutput transform(Transformation transformation, String serviceProvider, TransformOptions options,
            int targetMsgVersion) {
        final TransformOutput output = new TransformOutput();
        final int keySetVersion = options.getKsv();
        if (options.isIdentity()) {
            output.setIdentity(transformation.transformIdentity(serviceProvider, keySetVersion, targetMsgVersion));
        }
        if (options.isPseudonym()) {
            output.setPseudonym(transformation.transformPseudonym(serviceProvider, keySetVersion, targetMsgVersion));
        }
        return output;
    }

    public byte[] activate(ActivateRequest request) {
        logger.info("Activating {}{} - {}{}", request.getIdentifierType(), request.getIdentifier(),
                request.isSigned() ? "SIGNED " : "", request.getType());
        try (final Activation activation = new Activation(pool)) {
            return activation.activate(request);
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the
            // signature
            logger.error("Unexpected IO error", e);
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    public CardActivateResponse activateCard(CardActivateRequest request) {
        logger.info("Activating card {}{} - {}", request.getIdentifierType(), request.getIdentifier(),
                request.getSequenceNo());
        try (final CardActivation activation = new CardActivation(pool)) {
            return activation.activate(request);
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the
            // signature
            logger.error("Unexpected IO error", e);
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    @Cacheable(cacheNames = "verification-points", key = "#request.cacheKey()")
    public Map<String, byte[]> verificationPoints(VerificationPointsRequest request) {
        logger.info("VerificationPoints <{},{}>", request.getSchemeVersion(), request.getSchemeKeyVersion());
        try (final VerificationPoints action = new VerificationPoints(pool)) {
            return action.getPoints(request.getSchemeVersion(), request.getSchemeKeyVersion());
        } catch (IOException e) {
            // No action implementation is throwing an IOException, but it is part of the
            // signature
            logger.error("Unexpected IO error", e);
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    private String encodeForLog(final byte[] data) {
        if (!logger.isInfoEnabled() || data == null)
            return null;
        return BaseEncoding.base64().encode(data);
    }
}
