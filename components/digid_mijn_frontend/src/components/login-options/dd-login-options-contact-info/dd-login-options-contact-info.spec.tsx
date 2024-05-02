import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsContactInfo } from './dd-login-options-contact-info';
import { h } from '@stencil/core';
//import i18next from '../../../utils/i18next';

describe('dd-login-options-add-info', () => {
  const mockInfo = {
    account_id: 15,
    bsn: 'PPPPPPPPP',
    email: {
      account_id: 15,
      adres: 'PPPPPPPPPPPPP',
      confirmed_at: '2021-12-06T11:03:09.000+01:00',
      controle_code: 'SSSSSSSSS',
      created_at: '2021-12-06T11:02:35.000+01:00',
      id: 12,
      status: 'verified',
      updated_at: '2021-12-06T11:03:09.000+01:00',
    },
    gesproken_sms: false,
    phone_number: 'PPPPPPPPPPPP',
    pref_lang: 'nl',
    sms: false,
    two_faactivated_date: null,
    username: 'PPPPPPPPPPP',
    zekerheidsniveau: 10,
  };

  it('should render with the provided input', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsContactInfo],
      template: () => {
        return <dd-login-options-contact-info accountData={mockInfo as any} />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should compute the rowData on Update ', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsContactInfo],
      template: () => {
        return <dd-login-options-contact-info accountData={mockInfo as any} />;
      },
      supportsShadowDom: false,
    });
    await page.waitForChanges();
    const rowData = page.rootInstance.getRowData();
    expect(rowData).toEqual(['', 'PPPPPPPPPPPP', 'Nederlands']);
  });
});
