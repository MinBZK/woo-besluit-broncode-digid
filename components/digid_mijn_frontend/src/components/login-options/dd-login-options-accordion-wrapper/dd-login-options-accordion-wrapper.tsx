import { Component, Prop, Host, h, Fragment } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { contactHelpdeskLink } from '../../../global/global-jsx';

@Component({
	tag: 'dd-login-options-accordion-wrapper',
})
export class DdLoginOptionsAccordionWrapper {
	//Login method data passed from login options
	@Prop() appsArray: any[];
	@Prop() accountData: any;
	@Prop() docData: any;

	@Prop() hasNoDocuments: boolean;

	//Whether the apps are disabled
	@Prop() appsDisabled = false;

	//Check if there is an RVIG or RDW error before rendering the document accordions
	private renderIDAccordion() {
		if(this.docData.show_identity_cards){
			if (this.docData.rvig_error) {
				return (
					<dd-empty-state type='id' error={true}>
						{i18next.t('empty-state.no-connection-RVIG')}
						{this.docData.rvig_error === 'try_again' && i18next.t('empty-state.try-again')}
						{this.docData.rvig_error === 'contact' && contactHelpdeskLink()}
					</dd-empty-state>
				);
			}
			else if (!!this.docData.identity_cards.length) {
				return (
					<div>
						<dd-login-options-id view='accordion' document={this.docData.identity_cards[0]} />
					</div>
				);
			}
		}
	}

	private renderLicenceAccordion() {
		if(this.docData.show_driving_licences){
      if (this.docData.rdw_error) {
				return (
					<dd-empty-state type='id' error={true}>
						{i18next.t('empty-state.no-connection-RDW')}
						{this.docData.rdw_error === 'try_again' && i18next.t('empty-state.try-again')}
						{this.docData.rdw_error === 'contact' && contactHelpdeskLink()}
					</dd-empty-state>
				);
			}
			else if (!!this.docData.driving_licences.length) {
				return (
					<div>
						<dd-login-options-id view='accordion' document={this.docData.driving_licences[0]} />
					</div>
				);
			}
		}
	}

	private renderDocumentAccordions() {
		if (this.hasNoDocuments) {
			//If there is no RVIG/RDW error and the user has no documents, render the empty state
			return (
				<div>
					<dd-empty-state type='id' card={false} documentData={this.docData} />
				</div>
			);
		}
		return (
			<Fragment>
				{this.renderIDAccordion()}
				{this.renderLicenceAccordion()}
			</Fragment>
		);
	}

	private renderAppAccordion() {
		if (this.appsDisabled) {
			return (
				<dd-empty-state class='dd-login-options__app-disabled' type='app' error>
					{i18next.t('empty-state.apps-disabled')}
				</dd-empty-state>
			);
		}
		return (
			this.appsArray.map(app => (
				<div>
					<dd-login-options-app view='accordion' appData={app} />
				</div>
			))
		);
	}

	render() {
		return (
			<Host class='dd-login-options-accordion-wrapper'>
				{
					(this.appsArray?.length ? (
						this.renderAppAccordion()
					) : (
						<div>
							<dd-empty-state type='app' card={false} />
						</div>
					))}
				{this.accountData?.username && (
					<div>
						<dd-login-options-username view='accordion' accountData={this.accountData} />
					</div>
				)}
				{this.docData && this.renderDocumentAccordions()}
			</Host>
		);
	}
}
