import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsAccordionWrapper } from './dd-login-options-accordion-wrapper';
import { h } from '@stencil/core';

describe('dd-login-options-accordion-wrapper', () => {
	const mockApps = {app_authenticators: [
			{
				app_code: 'SSSSSSSS',
			},
			{
				app_code: 'SSSSSSSS',
			},]}
	const mockAccount = {username: 'test'}
  const mockNoAppsArray = [];
  const mockNoAccountData = { username: null };
  const mockNoDocData = { identity_cards: [], driving_licences: [] };
  const mockErrorDocData = {
    identity_cards: [{ doc_num: 'PPPPPP' }],
    driving_licences: [{ doc_num: 'PPPPPP' }],
		show_driving_licences: true,
		show_identity_cards: true,
    rdw_error: 'try_again',
    rvig_error: 'service',
  };
  const mockDocDataSwitchesOn = {
		status: 'OK',
		pin_reset_driving_licences: true,
		rdw_error: null,
		rvig_error: null,
		show_driving_licences: true,
		show_identity_cards: true,
		driving_licences: [
			{
				doc_num: 'PPPPPPPP',
			}
		],
		identity_cards: [
			{
				doc_num: 'PPPPPPPP',
			}
		],
		}

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
			}
		],
		identity_cards: [
			{
				doc_num: 'PPPPPPPP',
			}
		],
	}

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
			}
		],
		identity_cards: [
			{
				doc_num: 'PPPPPPPP',
			}
		],
	}

  it('should render with empty states if no data is entered', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsAccordionWrapper],
      template: () => {
        return (
          <dd-login-options-accordion-wrapper
            appsArray={mockNoAppsArray}
            accountData={mockNoAccountData}
            docData={mockNoDocData}
						hasNoDocuments={true}
          />
        );
      },
    });
    //amount of apps based on input data
    expect(page.root).toEqualHtml(`
      <dd-login-options-accordion-wrapper  class="dd-login-options-accordion-wrapper">
      <div>
        <dd-empty-state type="app"></dd-empty-state>
      </div>
      <div>
        <dd-empty-state type="id"></dd-empty-state>
      </div>
      </dd-login-options-accordion-wrapper>
    `);
  });

  it('should render error cards if there is an rvig and rdw error', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsAccordionWrapper],
      template: () => {
        return (
          <dd-login-options-accordion-wrapper
            appsArray={mockNoAppsArray}
            accountData={mockNoAccountData}
            docData={mockErrorDocData}
						hasNoDocuments={false}
          />
        );
      },
    });
    expect(page.root).toEqualHtml(`
     <dd-login-options-accordion-wrapper  class="dd-login-options-accordion-wrapper">
      <div>
        <dd-empty-state type="app"></dd-empty-state>
      </div>
        <dd-empty-state error="" type="id">
				 De status van uw identiteitskaart kon niet worden opgehaald.
				</dd-empty-state>
        <dd-empty-state error="" type="id">
        De status van uw rijbewijs kon niet worden opgehaald. Probeer het later nogmaals.
				</dd-empty-state>
      </dd-login-options-accordion-wrapper>
   `);
  });

  it('should render all accordions if data is available and all document switches are on', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsAccordionWrapper],
      template: () => {
        return (
          <dd-login-options-accordion-wrapper
            appsArray={mockApps.app_authenticators}
            accountData={mockAccount}
            docData={mockDocDataSwitchesOn}
						hasNoDocuments={false}
          />
        );
      },
    });

    expect(page.root).toEqualHtml(`
     <dd-login-options-accordion-wrapper  class="dd-login-options-accordion-wrapper">
      <div>
         <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
        <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
      <dd-login-options-username view="accordion"></dd-login-options-username>
      </div>
      <div>
         <dd-login-options-id view='accordion'></dd-login-options-id>
      </div>
      <div>
         <dd-login-options-id view='accordion'></dd-login-options-id>
      </div>
      </dd-login-options-accordion-wrapper>
   `);
  });

	it('should not render a driving licence accordion if the switch is off', async () => {
		const page = await newSpecPage({
			components: [DdLoginOptionsAccordionWrapper],
			template: () => {
				return (
					<dd-login-options-accordion-wrapper
						appsArray={mockApps.app_authenticators}
						accountData={mockAccount}
						docData={mockDocDataNoLicence}
						hasNoDocuments={false}
					/>
				);
			},
		});
		expect(page.root).toEqualHtml(`
 <dd-login-options-accordion-wrapper  class="dd-login-options-accordion-wrapper">
		      <div>
         <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
        <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
      <dd-login-options-username view="accordion"></dd-login-options-username>
      </div>
      <div>
         <dd-login-options-id view='accordion'></dd-login-options-id>
      </div>
      </dd-login-options-accordion-wrapper>
   `);
	});

	it('should not render a identity document accordion if the switch is off', async () => {
		const page = await newSpecPage({
			components: [DdLoginOptionsAccordionWrapper],
			template: () => {
				return (
					<dd-login-options-accordion-wrapper
						appsArray={mockApps.app_authenticators}
						accountData={mockAccount}
						docData={mockDocDataNoID}
						hasNoDocuments={false}
					/>
				);
			},
		});
		expect(page.root).toEqualHtml(`
		 <dd-login-options-accordion-wrapper  class="dd-login-options-accordion-wrapper">
		      <div>
         <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
        <dd-login-options-app view='accordion'></dd-login-options-app>
      </div>
      <div>
      <dd-login-options-username view="accordion"></dd-login-options-username>
      </div>
      <div>
         <dd-login-options-id view='accordion'></dd-login-options-id>
      </div>
      </dd-login-options-accordion-wrapper>
   `);
	});
});
