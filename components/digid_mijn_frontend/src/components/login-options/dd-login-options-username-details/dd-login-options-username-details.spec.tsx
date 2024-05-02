import getAccountData from '../dd-login-options/fixtures/getAccountData';

jest.mock('../../../api/services/dd.login-methods.service', () => ({
  getAccountData: jest.fn().mockImplementation(() => getAccountData),
}));

import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsUsernameDetails } from './dd-login-options-username-details';
import { h } from '@stencil/core';

const mockUser = {
  account_id: 13,
  bsn: 'PPPPPPPPP',
  deceased: false,
  email: {
    account_id: 13,
    adres: 'PPPPPPPPPPPPPPP',
    confirmed_at: '2021-12-16T15:48:17.000+01:00',
    controle_code: 'SSSSSSSSS',
    created_at: '2021-12-14T14:29:01.000+01:00',
    id: 14,
    status: 'verified',
    updated_at: '2021-12-16T15:48:17.000+01:00',
  },
  gesproken_sms: false,
  phone_number: 'PPPPPPPPPPPP',
  pref_lang: 'nl',
  sms: 'pending',
  two_faactivated_date: '2021-12-29T14:22:38.000+01:00',
  username: 'PPPPPPPPPPP',
  zekerheidsniveau: 10,
};

const mockUser2 = {
  account_id: 13,
  bsn: 'PPPPPPPPP',
  deceased: false,
  email: {
    account_id: 13,
    adres: 'PPPPPPPPPPPPPPP',
    confirmed_at: '2021-12-16T15:48:17.000+01:00',
    controle_code: 'SSSSSSSSS',
    created_at: '2021-12-14T14:29:01.000+01:00',
    id: 14,
    status: 'verified',
    updated_at: '2021-12-16T15:48:17.000+01:00',
  },
  gesproken_sms: false,
  phone_number: 'PPPPPPPPPPPP',
  pref_lang: 'nl',
  sms: 'inactive',
  two_faactivated_date: '2021-12-29T14:22:38.000+01:00',
  username: 'PPPPPPPPPPP',
  zekerheidsniveau: 10,
};

describe('dd-login-options-username-details', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsernameDetails],
      template: () => {
        return <dd-login-options-username-details userData={mockUser} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render with the activate sms code link', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsernameDetails],
      template: () => {
        return <dd-login-options-username-details userData={mockUser2} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render with the request sms code link ', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsernameDetails],
      template: () => {
        return <dd-login-options-username-details userData={mockUser2} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });
});
