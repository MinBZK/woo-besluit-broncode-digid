
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

package nl.logius.digid.app.domain.flow;

import nl.logius.digid.app.client.SharedServiceClientException;
import nl.logius.digid.app.domain.authentication.flow.flows.AuthenticateLoginFlow;
import nl.logius.digid.app.domain.authenticator.AppAuthenticator;
import nl.logius.digid.app.domain.authenticator.AppAuthenticatorService;
import nl.logius.digid.app.domain.confirmation.flow.flows.ConfirmSessionFlow;
import nl.logius.digid.app.domain.confirmation.request.CheckPendingRequest;
import nl.logius.digid.app.domain.session.*;
import nl.logius.digid.app.shared.request.*;
import nl.logius.digid.app.shared.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public abstract class FlowService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    public final AppSessionService appSessionService;
    public final AppAuthenticatorService appAuthenticatorService;
    public final FlowFactoryFactory flowFactoryFactory;

    public FlowService(AppSessionService appSessionService, FlowFactoryFactory flowFactoryFactory, AppAuthenticatorService appAuthenticatorService) {
        this.appSessionService = appSessionService;
        this.appAuthenticatorService = appAuthenticatorService;
        this.flowFactoryFactory = flowFactoryFactory;
    }

    /**
     * default method to process actions
     * @param flowType expected nl.logius.digid.app.domain.shared.flow, used to retrieve corresponding nl.logius.digid.app.domain.shared.flow factory
     * @param action requested action to execute
     * @param request object send with request
     * @return
     * @throws FlowNotDefinedException
     * @throws FlowStateNotDefinedException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public AppResponse processAction(String flowType, BaseAction action, AppSessionRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAppSessionId());
        validateSessionAndRequestUserAppId(appSession.getUserAppId(), request.getUserAppId());
        return processAction(flowType, action, request, appSession);
    }

    public AppResponse processAction(String flowType, BaseAction action, AuthSessionRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAuthSessionId());
        return processAction(flowType, action, request, appSession);
    }

    public AppResponse processAction(String flowType, BaseAction action, CheckPendingRequest request) throws FlowNotDefinedException, SharedServiceClientException, IOException, NoSuchAlgorithmException, FlowStateNotDefinedException {
        var appAuthenticator = appAuthenticatorService.findByUserAppId(request.getUserAppId());
        var appSession = appSessionService.findByAccountIdAndFlow(appAuthenticator.getAccountId(), ConfirmSessionFlow.NAME);
        if (appSession.isEmpty()) return new NokResponse();

        return processAction(flowType, action, request, appSession.get());
    }

    public AppResponse processAction(String flowType, BaseAction action, AppRequest request, AppSession appSession) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        Flow flow = flowFactoryFactory.getFactory(flowType).getFlow(appSession.getFlow());
        AbstractFlowStep flowStep = flow.validateStateTransition(stateValueOf(appSession.getState().toUpperCase()), action);
        if (flowStep == null) {
            logger.error("nl.logius.digid.app.domain.shared.flow transition not allowed:{} - {} -> {}", flow.getClass(), appSession.getState(), action);
            return new NokResponse("nl.logius.digid.app.domain.shared.flow transition not allowed");
        }
        flowStep.setAppSession(appSession);

        if (flowStep.expectAppAuthenticator()) {
            flowStep.setAppAuthenticator(getAppAuthenticator(appSession));
        }

        AppResponse appResponse = flow.processState(flowStep, request);
        if (appResponse instanceof NokResponse || !flowStep.isValid()) {
            return appResponse;
        }

        appSession.setFlow(flow.getName());
        appSession.setState(getStateName(flow.getNextState(stateValueOf(appSession.getState().toUpperCase()), action)));

        if (appSession.getState().equals("APP_ACTIVATED") || appSession.getState().equals("APP_PENDING")) {
            appSession.setFlow(AuthenticateLoginFlow.NAME);
            appSession.setState("AUTHENTICATED");
        }

        if (flowStep.getAppAuthenticator() != null && appAuthenticatorService.exists(flowStep.appAuthenticator)) {
            appAuthenticatorService.save(flowStep.getAppAuthenticator());

             if (appSession.getDeviceName() == null) {
                 appSession.setDeviceName(flowStep.getAppAuthenticator().getDeviceName());
             }
             if (appSession.getAppCode() == null) {
                 appSession.setAppCode(flowStep.getAppAuthenticator().getAppCode());
             }
        }

        appSessionService.save(appSession);

        return appResponse;
    }

    /**
     * Process action that supports multiple flows (e.g. registering notifications)
     * @param action requested action to execute
     * @param request object send with request
     * @return
     * @throws FlowNotDefinedException
     */
    public AppResponse processActionExtractFlow(BaseAction action, AppSessionRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        AppSession appSession = appSessionService.getSession(request.getAppSessionId());
        FlowFactory flowFactory = flowFactoryFactory.getFactoryByFlow(appSession.getFlow());
        return processAction(flowFactory.getType(), action, request);
    }

    /**
     * Cancel current nl.logius.digid.app.domain.shared.flow (delete session)
     * @param request
     * @return
     * @throws FlowNotDefinedException
     */
    /** TODO separate /cancel & /cancel_application in a proper way -> currently RequestAccountAndAppFlow overrides the cancelFlow method
        BUT this does not correctly handle /cancel_application for other flows and /cancel for RequestAccountAndAppFlow
    **/
    public AppResponse cancelAction(BaseAction action, CancelFlowRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        try {
            AppSession appSession = appSessionService.getSession(request.getAppSessionId());
            FlowFactory flowFactory = flowFactoryFactory.getFactoryByFlow(appSession.getFlow());

            Flow flow = flowFactory.getFlow(appSession.getFlow());
            AbstractFlowStep flowStep = flow.cancelFlow(action);
            flowStep.setAppSession(appSession);

            AppResponse appResponse = flow.processState(flowStep, request);

            appSession.setState(State.CANCELLED.name());
            appSessionService.save(appSession);

            return appResponse;
        } catch (AppSessionNotFoundException e) {
            return new OkResponse();
        }
    }

    public AppResponse abortAction(BaseAction action, CancelFlowRequest request) throws FlowNotDefinedException, FlowStateNotDefinedException, IOException, NoSuchAlgorithmException, SharedServiceClientException {
        try {
            AppSession appSession = appSessionService.getSession(request.getAppSessionId());
            FlowFactory flowFactory = flowFactoryFactory.getFactoryByFlow(appSession.getFlow());

            Flow flow = flowFactory.getFlow(appSession.getFlow());
            AbstractFlowStep flowStep = flow.cancelFlow(action);
            flowStep.setAppSession(appSession);

            AppResponse appResponse = flow.processState(flowStep, request);

            appSession.setState(State.ABORTED.name());
            appSessionService.save(appSession);

            return appResponse;
        } catch (AppSessionNotFoundException e) {
            return new OkResponse();
        }
    }

    public AppAuthenticator getAppAuthenticator(AppSession appSession) {
        if (appSession.getUserAppId() != null) {
            return appAuthenticatorService.findByUserAppId(appSession.getUserAppId());
        }
        return null;
    }

    public void validateSessionAndRequestUserAppId(String appSessionUserAppId, String requestUserAppId){
        if(appSessionUserAppId != null && requestUserAppId != null && !appSessionUserAppId.equals(requestUserAppId)){
            throw new AppSessionUserAppIdInvalidException(String.format("session has user_app_id: %s but request contains user_app_id: %s", appSessionUserAppId, requestUserAppId));
        }
    }

    protected String getStateName(BaseState state) {
        return state.name();
    }

    protected BaseState stateValueOf(String string) {
        return nl.logius.digid.app.domain.flow.State.valueOf(string);
    }
}
