import {
  warningSessionTimeResponse,
  timeoutSessionTimeResponse,
  staticSessionTimeResponse,
} from './fixtures/getSession';

let getSessionResponse;
jest.mock('../../api/services/dd.session.service.ts', () => ({
  getSession: jest.fn().mockImplementation(() => getSessionResponse),
}));

import { newSpecPage } from '@stencil/core/testing';
import { h } from '@stencil/core';
import sessionService from '../../api/services/dd.session.service';
import { DdSessionHandler } from './dd-session-handler';
import { DdModal } from '../dd-modal/dd-modal';
import { DdBackdrop } from '../dd-backdrop/dd-backdrop';
import { DdButton } from '../base/dd-button/dd-button';

describe('dd-session-handler', () => {
  it('should render the session modals', async () => {
    const page = await newSpecPage({
      components: [DdSessionHandler, DdModal, DdBackdrop, DdButton],
      template: () => {
        return <dd-session-handler />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('Should call the getSession and get the timeout time returned', async () => {
    getSessionResponse = staticSessionTimeResponse;
    const receivedResponse = new Date('2022-02-25T12:31:07.563Z');
    return sessionService.getSession().then(data => expect(data.timestamp).toEqual(receivedResponse));
  });

  it('should render the warning modal when there is only 1 minute left on the session', async () => {
    const page = await newSpecPage({
      components: [DdSessionHandler, DdModal, DdBackdrop, DdButton],
      template: () => {
        return <dd-session-handler />;
      },
    });
    const component: DdSessionHandler = page.rootInstance;
    const showModalSpy = jest.fn();

    (component as any).warningModal = {
      showModal: showModalSpy,
    };

    getSessionResponse = warningSessionTimeResponse;

    await component.getSessionAndRenderModals();

    expect(showModalSpy).toHaveBeenCalled();
  });

  it('should render the timeout modal when there is no time left on the session', async () => {
    const page = await newSpecPage({
      components: [DdSessionHandler, DdModal, DdBackdrop, DdButton],
      template: () => {
        return <dd-session-handler />;
      },
    });
    const component: DdSessionHandler = page.rootInstance;
    const showModalSpy = jest.fn();
    const hideModalSpy = jest.fn();

    (component as any).timeoutModal = {
      showModal: showModalSpy,
    };

    (component as any).warningModal = {
      hideModal: hideModalSpy,
    };

    getSessionResponse = timeoutSessionTimeResponse;

    await component.getSessionAndRenderModals();

    expect(showModalSpy).toHaveBeenCalled();
    expect(hideModalSpy).toHaveBeenCalled();
  });
});
