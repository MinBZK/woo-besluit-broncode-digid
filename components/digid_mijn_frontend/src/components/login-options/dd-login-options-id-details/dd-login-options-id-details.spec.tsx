import getDocumentData from '../dd-login-options/fixtures/getDocumentData';

jest.mock('../../../api/services/dd.login-methods.service', () => ({
  getDocumentData: jest.fn().mockImplementation(() => getDocumentData),
}));

import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsIdDetails } from './dd-login-options-id-details';
import { h } from '@stencil/core';

const documentMock = {
  activated_at: '2021-12-09T10:51:00+01:00',
  active: false,
  doc_num: 'PPPPPPPPPP',
  existing_unblock_request: false,
  paid: true,
  sequence_no: 'SSSSSSSSSSSS',
  status: 'niet_actief',
	documentType : 'id'
};

describe('dd-login-options-id-details', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsIdDetails],
      template: () => {
        return <dd-login-options-id-details docData={documentMock} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });
});
