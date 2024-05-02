import getApps from '../dd-login-options/fixtures/getApp';

jest.mock('../../../api/services/dd.login-methods.service', () => ({
  getApps: jest.fn().mockImplementation(() => getApps),
}));

import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsAppDetails } from './dd-login-options-app-details';
import { h } from '@stencil/core';

const appMock = {
  account_id: 13,
  activated_at: '2021-11-29T15:43:21.000+01:00',
  created_at: '2021-11-29T15:43:21.000+01:00',
  device_name: 'Huawei P30 Pro Lite Gold',
  id: 16,
  instance_id: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS',
  last_sign_in_at: '2021-12-29T14:22:07.000+01:00',
  status: 'active',
  substantieel_activated_at: null,
  substantieel_document_type: null,
};

const appMock2 = {
  account_id: 13,
  activated_at: '2021-11-29T15:43:21.000+01:00',
  created_at: '2021-11-29T15:43:21.000+01:00',
  device_name: 'PPPPPPPPPPPPPPP',
  id: 16,
  instance_id: 'SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS',
  last_sign_in_at: '2021-12-29T14:22:07.000+01:00',
  status: 'active',
  substantieel_activated_at: '2021-11-29T15:43:21.000+01:00',
  substantieel_document_type: 'licence',
};

//Skipped suite because of  Call retries were exceeded at ChildProcessWorker.initialize exception
describe('dd-login-options-app-details', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsAppDetails],
      template: () => {
        return <dd-login-options-app-details data={appMock} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsAppDetails],
      template: () => {
        return <dd-login-options-app-details data={appMock2} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });
});
