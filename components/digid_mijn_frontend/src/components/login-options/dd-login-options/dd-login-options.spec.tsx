import getApps from './fixtures/getApp';
import getDocumentData from './fixtures/getDocumentData';
import getAccountData from './fixtures/getAccountData';

jest.mock('../../../api/services/dd.login-methods.service', () => ({
  getAccountData: jest.fn().mockImplementation(() => getAccountData),
  getApps: jest.fn().mockImplementation(() => getApps),
  getDocumentData: jest.fn().mockImplementation(() => getDocumentData),
}));

jest.mock('../../../api/services/dd.session.service.ts', () => ({
  updateSession: jest.fn().mockImplementation(() => {}),
}));

import { newSpecPage, SpecPage } from '@stencil/core/testing';
import { DdLoginOptions } from './dd-login-options';
import { h } from '@stencil/core';
import ddService from '../../../api/services/dd.login-methods.service';
import { mockAccountDataResponse, mockDocumentdataResponse, mockedAppResponse } from '../../../global/dev-data';

describe('dd-login-options', () => {
  it('should render with the provided input', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptions],
      html: <dd-login-options />,
    });

    expect(page.root).toMatchSnapshot();
  });

  it('should call data on load', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptions],
      template: () => {
        return <dd-login-options />;
      },
      supportsShadowDom: false,
    });
    const fetchAppsDataSpy = jest.spyOn(page.rootInstance, 'fetchAppsData');
    const fetchAccountDataSpy = jest.spyOn(page.rootInstance, 'fetchAccountData');
    const fetchDocumentDataSpy = jest.spyOn(page.rootInstance, 'fetchDocumentData');
    page.rootInstance.componentWillLoad();
    expect(fetchAppsDataSpy).toHaveBeenCalled();
    expect(fetchAccountDataSpy).toHaveBeenCalled();
    expect(fetchDocumentDataSpy).toHaveBeenCalled();
  });

  it('should call app data and assign it to the appsArray', async () => {
    return ddService.getApps().then(data => expect(data).toEqual(mockedAppResponse));
  });

  it('should call account data and assign it to the accountData array', async () => {
    return ddService.getAccountData().then(data => expect(data).toEqual(mockAccountDataResponse));
  });

  it('should call document data and assign it to the documentData array', async () => {
    return ddService.getDocumentData().then(data => expect(data).toEqual(mockDocumentdataResponse));
  });

  it('Should call the navigation method after a button click', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptions],
      template: () => {
        return <dd-login-options />;
      },
    });
    const btn: HTMLElement = page.body.querySelector('#navToHistBtn');
    const navigateUsageHistSpy = jest.spyOn(page.rootInstance.navigationClicked, 'emit');

    btn.click();
    await page.waitForChanges();

    expect(navigateUsageHistSpy).toHaveBeenCalled();
  });

  xit('should call the delete function after the delete button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptions],
      html: <dd-login-options />,
    });
    const btn: HTMLElement = page.body.querySelector('dd-login-options__deleteBtn');
    btn.click();
    await page.waitForChanges();
    expect(page.rootInstance.deleteDigid).toHaveBeenCalled();
  });

  it('should return the newest document from the sortDocumentsAndReturnNewest function', async () => {
    const mockDocData = {
      driving_licences: [
        {
          activated_at: '2022-10-24T16:01:00+02:00',
        },
        {
          activated_at: '2021-10-24T16:01:00+02:00',
        },
        {
          status: 'newest',
          activated_at: '2023-10-24T16:01:00+02:00',
        },
      ],
      identity_cards: [
        {
          doc_num: 'PPPPPPPP',
        },
      ],
    };

    const page = await newSpecPage({
      components: [DdLoginOptions],
      template: () => {
        return <dd-login-options />;
      },
    });

    page.rootInstance.assignDocumentData(mockDocData);
    await page.waitForChanges();

    expect(page.rootInstance.drivingLicence.status).toBe('newest');
    expect(page.rootInstance.identityDocument.doc_num).toBe('PPPPPPPP');
  });
});

describe('The document area', () => {
  const mockDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
    show_driving_licences: true,
    show_identity_cards: true,
    rdw_error: null,
    rvig_error: null,
  };

  const mockNoDocData = { identity_cards: [], driving_licences: [] };

  const mockBothErrorDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
    show_driving_licences: true,
    show_identity_cards: true,
    rdw_error: 'try_again',
    rvig_error: 'service',
  };

  const mockIDErrorDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
    show_driving_licences: true,
    show_identity_cards: true,
    rdw_error: null,
    rvig_error: 'service',
  };

  const mockLicenceErrorDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
    show_driving_licences: true,
    show_identity_cards: true,
    rdw_error: 'try_again',
    rvig_error: null,
  };

  const mockBothSwitchesOffDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
    show_driving_licences: false,
    show_identity_cards: false,
    rdw_error: null,
    rvig_error: null,
  };

  const mockDocDataNoLicence = {
    status: 'OK',
    pin_reset_driving_licences: true,
    rdw_error: null,
    rvig_error: null,
    show_driving_licences: false,
    show_identity_cards: true,
    driving_licences: [
      {
        doc_num: 'PPPPPPPP',
      },
    ],
    identity_cards: [
      {
        doc_num: 'PPPPPPPP',
      },
    ],
  };

  const mockDocDataNoID = {
    status: 'OK',
    pin_reset_driving_licences: true,
    rdw_error: null,
    rvig_error: null,
    show_driving_licences: true,
    show_identity_cards: false,
    driving_licences: [
      {
        doc_num: 'PPPPPPPP',
      },
    ],
    identity_cards: [
      {
        doc_num: 'PPPPPPPP',
      },
    ],
  };

  let page: SpecPage;
  let documentArea: HTMLElement;

  beforeEach(async () => {
    page = await newSpecPage({
      components: [DdLoginOptions],
      template: () => {
        return <dd-login-options />;
      },
    });
    documentArea = page.root.querySelector('.dd-login-options__id-card-area');
  });

  it('should render document cards', async () => {
    page.rootInstance.assignDocumentData(mockDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
      <h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'>
				<li>
        	<dd-login-options-id class='dd-login-options__id-card-area__two-docs'></dd-login-options-id>
       </li>
       <li>
         <dd-login-options-id></dd-login-options-id>
       </li>
			</ul>
		`);
  });

  it('should show an empty state when no documents are available', async () => {
    page.rootInstance.assignDocumentData(mockNoDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
		<ul class="dd-login-options__card-wrapper"></ul>
	  <dd-empty-state type="id"></dd-empty-state>
		`);
  });

  it('should not render a licence when the switch is off', async () => {
    page.rootInstance.assignDocumentData(mockDocDataNoLicence);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'>
			<li>
        <dd-login-options-id class="dd-login-options__id-card-area__two-docs"></dd-login-options-id>
      </li>
     </ul>
    `);
  });

  it('should not render an identity document when the switch is off', async () => {
    page.rootInstance.assignDocumentData(mockDocDataNoID);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'>

       <li>
         <dd-login-options-id></dd-login-options-id>
       </li>
     </ul>

		`);
  });

  it('should render an error card for the licence', async () => {
    page.rootInstance.assignDocumentData(mockLicenceErrorDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'>
       <li>
         <dd-login-options-id class="dd-login-options__id-card-area__two-docs"></dd-login-options-id>
       </li>
       <li>
         <dd-empty-state error="" type="id">
           De status van uw rijbewijs kon niet worden opgehaald. Probeer het later nogmaals.
         </dd-empty-state>
       </li>
     </ul>
		`);
  });

  it('should render an error card for the identity document', async () => {
    page.rootInstance.assignDocumentData(mockIDErrorDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'>
 			<li>
         <dd-empty-state error="" type="id">
           De status van uw identiteitskaart kon niet worden opgehaald.
         </dd-empty-state>
       </li>
       <li>
         <dd-login-options-id></dd-login-options-id>
       </li>
</ul>
		`);
  });

  it('should render two error cards', async () => {
    page.rootInstance.assignDocumentData(mockBothErrorDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			 <ul class="dd-login-options__card-wrapper">
       <li>
         <dd-empty-state error="" type="id">
           De status van uw identiteitskaart kon niet worden opgehaald.
         </dd-empty-state>
       </li>
       <li>
         <dd-empty-state error="" type="id">
           De status van uw rijbewijs kon niet worden opgehaald. Probeer het later nogmaals.
         </dd-empty-state>
       </li>
     </ul>

		`);
  });

  it('should show nothing when both switches are turned off', async () => {
    page.rootInstance.assignDocumentData(mockBothSwitchesOffDocData);
    await page.waitForChanges();
    expect(documentArea.innerHTML).toEqualHtml(`
		<h3 class='dd-login-options__card-grid__header digid'>Met identiteitsbewijs</h3>
 			<ul class='dd-login-options__card-wrapper'></ul>
 			<dd-empty-state type="id"></dd-empty-state>
		`);
  });
});
