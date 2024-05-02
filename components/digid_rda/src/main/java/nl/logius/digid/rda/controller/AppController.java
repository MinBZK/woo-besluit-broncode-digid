
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

package nl.logius.digid.rda.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.logius.digid.rda.apdu.ApduFactory;
import nl.logius.digid.rda.exceptions.BadRequestException;
import nl.logius.digid.rda.exceptions.RdaException;
import nl.logius.digid.rda.exceptions.StatusException;
import nl.logius.digid.rda.models.Protocol;
import nl.logius.digid.rda.models.RdaSession;
import nl.logius.digid.rda.models.Status;
import nl.logius.digid.rda.models.card.App;
import nl.logius.digid.rda.models.card.Step;
import nl.logius.digid.rda.models.rest.app.AuthenticateRequest;
import nl.logius.digid.rda.models.rest.app.AuthenticateResponse;
import nl.logius.digid.rda.models.rest.app.KeyAgreeRequest;
import nl.logius.digid.rda.models.rest.app.KeyAgreeResponse;
import nl.logius.digid.rda.models.rest.app.MapRequest;
import nl.logius.digid.rda.models.rest.app.MapResponse;
import nl.logius.digid.rda.models.rest.app.MutualAuthRequest;
import nl.logius.digid.rda.models.rest.app.MutualAuthResponse;
import nl.logius.digid.rda.models.rest.app.PrepareRequest;
import nl.logius.digid.rda.models.rest.app.PrepareResponse;
import nl.logius.digid.rda.models.rest.app.SecureMessagingRequest;
import nl.logius.digid.rda.models.rest.app.SecureMessagingResponse;
import nl.logius.digid.rda.models.rest.app.SelectRequest;
import nl.logius.digid.rda.models.rest.app.SelectResponse;
import nl.logius.digid.rda.models.rest.app.StartRequest;
import nl.logius.digid.rda.models.rest.app.StartResponse;
import nl.logius.digid.rda.service.CardVerifier;
import nl.logius.digid.rda.service.ConfirmService;
import nl.logius.digid.rda.service.RandomFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1")
@Tag(name = "/v1", description = "App endpoints")
public class AppController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CardVerifier cardVerifier;
    @Autowired
    private ConfirmService confirmService;
    @Autowired
    private RandomFactory randomFactory;

    @Operation(summary = "Start new session and return select commands and an additional challenge command")
    @PostMapping(value = "start", consumes = "application/json", produces = "application/json")
    public StartResponse start(@Valid @RequestBody StartRequest request,
                               @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new StartResponse();

        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else if (request.supportsPace()) {
            session.reset();
            final var infos = request.paceInfo();
            session.getApp().setPaceParameterId(infos.getPaceParameterId());
            session.getApp().setProtocol(Protocol.PACE);
            session.setStatus(Status.PREPARE);
            session.getApp().setDocumentType(request.getType());
            response.setSelect(App.getSelect(request.getType(), Protocol.PACE));
            response.setPace(ApduFactory.getPaceInitialize());
            response.setNonce(ApduFactory.getRandomNonce());
            sessionRepo.save(session);
        } else if (request.getType() != null) {
            session.reset();
            session.setStatus(Status.CHALLENGE);
            session.getApp().setProtocol(Protocol.BAC);
            session.getApp().setDocumentType(request.getType());
            response.setSelect(App.getSelect(request.getType(), Protocol.BAC));
            response.setChallenge(ApduFactory.getChallenge());

            sessionRepo.save(session);
        } else {
            session.reset();
            session.setStatus(Status.SELECT);
            session.getApp().setProtocol(Protocol.BAC);
            response.setSelects(App.getSelects(session.getApp()));
            response.setChallenge(ApduFactory.getChallenge());

            sessionRepo.save(session);
        }

        response.setStatus(session.getStatus());
        return response;
    }

    @Operation(summary = "Identified selected card and return authenticate command")
    @PostMapping(value = "select", consumes = "application/json", produces = "application/json")
    public SelectResponse select(@Valid @RequestBody SelectRequest request,
                                 @RequestHeader(value = "X-Forwarded-For") String clientIp) {

        final var session = findSession(request, clientIp);
        final var response = new SelectResponse();
        checkStatus(session, Status.SELECT);

        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else if (request.getType() == null) {
            session.setStatus(Status.FAILED);
            sessionRepo.save(session);
        } else {
            session.getApp().setDocumentType(request.getType());
            withApp(session, app -> {
                app.setChallenge(request.getChallenge());
                response.setAuthenticate(app.getAuthenticate());
                session.setStatus(Status.AUTHENTICATE);
            });
        }

        response.setStatus(session.getStatus());
        return response;
    }

    @PostMapping(value = "challenge", consumes = "application/json", produces = "application/json")
    public SelectResponse challenge(@Valid @RequestBody SelectRequest request,
                                    @RequestHeader(value = "X-Forwarded-For") String clientIp) {

        final var session = findSession(request, clientIp);
        final var response = new SelectResponse();
        checkStatus(session, Status.CHALLENGE);

        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else {
            withApp(session, app -> {
                app.setChallenge(request.getChallenge());
                response.setAuthenticate(app.getAuthenticate());
                session.setStatus(Status.AUTHENTICATE);
            });

            sessionRepo.save(session);
        }

        response.setStatus(session.getStatus());
        return response;
    }


    @PostMapping(value = "prepare", consumes = "application/json", produces = "application/json")
    public PrepareResponse prepare(@Valid @RequestBody PrepareRequest request,
                                   @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new PrepareResponse();
        checkStatus(session, Status.PREPARE);

        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else {
            withApp(session, app -> {
                response.setMappedNonce(app.decryptNonce(request.getEncryptedNonce()));
                session.setStatus(Status.MAP);
            });
        }

        response.setStatus(session.getStatus());
        return response;
    }

    @PostMapping(value = "map", consumes = "application/json", produces = "application/json")
    public MapResponse map(@Valid @RequestBody MapRequest request,
                           @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new MapResponse();
        checkStatus(session, Status.MAP);

        withApp(session, app -> {
            response.setKeyAgree(app.mapNonce(request.getMappedNonce().getData()));
            session.setStatus(Status.KEY_AGREE);
        });

        response.setStatus(session.getStatus());
        return response;
    }

    @PostMapping(value = "key_agreement", consumes = "application/json", produces = "application/json")
    public KeyAgreeResponse keyAgree(@Valid @RequestBody KeyAgreeRequest request,
                                     @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new KeyAgreeResponse();

        checkStatus(session, Status.KEY_AGREE);

        withApp(session, app -> {
            response.setToken(app.keyAgree(request.getKeyAgree().getData()));
            session.setStatus(Status.MUTUAL_AUTH);
        });

        response.setStatus(session.getStatus());
        return response;
    }

    @PostMapping(value = "mutual_auth", consumes = "application/json", produces = "application/json")
    public MutualAuthResponse mutualAuth(@Valid @RequestBody MutualAuthRequest request,
                                         @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new MutualAuthResponse();
        checkStatus(session, Status.MUTUAL_AUTH);

        if (request.getAuthenticate() != null) {
            withApp(session, app -> {
                app.verifyToken(request.getAuthenticate());
                response.setCommands(app.next());
                session.setStatus(Status.SECURE_MESSAGING);
            });
        } else if (request.getEncryptedNonce() != null) {
            withApp(session, app -> {
                response.setMappedNonce(app.decryptNonce(request.getEncryptedNonce()));
                session.setStatus(Status.MAP);
            });
        }

        response.setStatus(session.getStatus());

        return response;
    }

    @Operation(summary = "Authenticate card by providing response or give next challenge response")
    @PostMapping(value = "authenticate", consumes = "application/json", produces = "application/json")
    public AuthenticateResponse authenticate(@Valid @RequestBody AuthenticateRequest request,
                                             @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        if (request.getChallenge() == null && request.getAuthenticate() == null) {
            throw new BadRequestException("Both challenge and authenticate are null");
        }

        final var session = findSession(request, clientIp);
        final var response = new AuthenticateResponse();

        checkStatus(session, Status.AUTHENTICATE);

        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else if (request.getAuthenticate() != null) {
            withApp(session, app -> {
                app.verifyAuthenticate(request.getAuthenticate());
                response.setCommands(app.next());
                session.setStatus(Status.SECURE_MESSAGING);
            });
        } else if (request.getAuthenticate() == null) {
            withApp(session, app -> {
                app.setChallenge(request.getChallenge());
                response.setAuthenticate(app.getAuthenticate());
            });
        }

        response.setStatus(session.getStatus());
        return response;
    }

    @Operation(summary = "Secure messaging session, just process requests and return responses")
    @PostMapping(value = "secure_messaging", consumes = "application/json", produces = "application/json")
    public SecureMessagingResponse secureMessaging(@Valid @RequestBody SecureMessagingRequest request,
                                                   @RequestHeader(value = "X-Forwarded-For") String clientIp) {
        final var session = findSession(request, clientIp);
        final var response = new SecureMessagingResponse();

        checkStatus(session, Status.SECURE_MESSAGING);
        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else {
            withApp(session, app -> {
                response.setCommands(app.process(request.getResponses()));
                if (session.getApp().getStep() == Step.END) {
                    session.setStatus(Status.VERIFIED);
                    logger.info("RDA success for {}", session.getId());
                    confirmService.sendConfirm(
                        session.getReturnUrl(),
                        session.getConfirmId(),
                        session.getConfirmSecret(),
                        true,
                        session.getApp(),
                        null
                    );
                }
            });
        }

        response.setStatus(session.getStatus());
        return response;
    }

    private void checkStatus(RdaSession session, Status allowed) {
        if (session.getStatus() != allowed && !session.isFinished()) {
            logger.warn("Invalid status for {}, got {}, expected {}", session.getId(), session.getStatus(), allowed);
            throw new StatusException("Session is in state " + allowed);
        }
    }

    private void withApp(RdaSession session, AppConsumer consumer) {
        final var app = App.fromSession(session.getApp(), cardVerifier, randomFactory);

        try {
            consumer.accept(app);
        } catch (RdaException e) {
            if (logger.isInfoEnabled()) {
                logger.error("RDA failed for " + session.getId(), e);
            } else {
                logger.error("RDA failed for {}: {} - {}", session.getId(), e.error, e.getMessage());
            }
            session.setStatus(Status.FAILED);
            confirmService.sendConfirm(
                session.getReturnUrl(),
                session.getConfirmId(),
                session.getConfirmSecret(),
                false,
                session.getApp(),
                e.error
            );
        }
        if (session.isFinished()) {
            sessionRepo.delete(session);
        } else {
            sessionRepo.save(session);
        }
    }

    @FunctionalInterface
    private interface AppConsumer {
        void accept(App app) throws RdaException;
    }
}
