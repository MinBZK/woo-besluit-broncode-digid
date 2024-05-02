import { Component, Host, h, Prop, State, Fragment, Listen } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../../utils/i18next';
import {
	getGlobalApps,
	getShow2FAWarningWhenDeactivatingApp,
	setDocumentTitle,
	trackPageView,
} from '../../../global/global-methods';
import { hiddenNewWindow, loginMethodDetailsTable } from '../../../global/global-jsx';
import { getCompleteTime, parseDateFromISO } from '../../../global/global-date-and-time';
import ddService from '../../../api/services/dd.login-methods.service';
import sessionService from '../../../api/services/dd.session.service';
import { Components } from '../../../components';
import DdModal = Components.DdModal;
import { getAppDeactivationLink } from '../../../global/global-links';

@Component({
	tag: 'dd-login-options-app-details',
})
export class DdLoginOptionsAppDetails {
	private twoFactorWarningModal: DdModal;

	//Property used to access the router
	@Prop() history: RouterHistory;

	//The data of the currently displayed app
	@Prop({ mutable: true }) data: any;

	//Text used to update text to speech users
	@State() ariaUpdateText : string;

	//An array of all other available apps
	@State() otherApps: any[];

	//Table arrays
	@State() _rowNames = [];
	@State() _rowContent;
	@State() _rowLinks;
	@State() _rowTooltips;
	@State() _rowIndented;
	@State() _rowTooltipContent;

	componentWillLoad() {
		setDocumentTitle('App details');
		trackPageView();

		//Extract data if navigating from a click on the app card
		if (this.history && this.history.location.state) {
			this.data = this.history.location.state.data;
			return ddService.getApps();
		}
		//If navigating through the URL, retrieve the users appdata
		else {
			ddService
				.getApps()
				.catch(err => {
					console.error(err);
				})
				.finally(() => {
					//Check if the user has an app
					if (getGlobalApps().length) {
						//Fill the page with data of the first app
						this.data = getGlobalApps()[0];
					}
					//If not, send user back to home
					else {
						this.returnToLogin();
					}
				});
		}
	}

	componentWillRender() {
		document.title = i18next.t('app.page-title');
		//Filter out the other apps to show on the bottom of the page
		this.otherApps = getGlobalApps().filter(app => app.id != this.data.id);
		//Fill all data
		this.fillData();

	}

	componentDidLoad() {
		this.addAriaDescribedby();
		sessionService.updateSession().then(() => {

			if (getShow2FAWarningWhenDeactivatingApp()) {
				//Add an eventListener to the deactivation link showing a warning
				document.getElementById('app-deactivation-link').firstChild.addEventListener('click', (e) => {
					e.preventDefault();
					return this.twoFactorWarningModal.showModal();
				});
			}
		});
	}

	@Listen('bannerClosed')
	updateAriaUsers(event){
		this.ariaUpdateText = (event.detail);
	}

	private IDcheckInactiveTooltip() {
		return (
			<Fragment>
				{i18next.t('app.details-id-inactive')}{' '}
				<a href='https://www.digid.nl/inlogmethodes/digid-app/#voeg-id-check-toe/' target='_blank'>
					{i18next.t('app.details-id-link')}
				</a>{' '}
				{hiddenNewWindow()}
			</Fragment>
		);
	}

	//Return to the login methods page
	private returnToLogin() {
		if (this.history) {
			this.history.push('/home');
		}
	}

	//Fill data that could be re-rendered
	private fillData() {
		if (!this.data) return;
		const data = this.data;

		//Array with the names of each row
		this._rowNames = [
			i18next.t('app.name'),
			i18next.t('app.status'),
			i18next.t('app.log'),
			i18next.t('app.activated'),
			i18next.t('app.code'),
			i18next.t('general.ID-check'),
			i18next.t('app.type'),
		];

		//Array with the display data of each row
		this._rowContent = [
			data.device_name,
			<dd-status active={!!data.activated_at} icon={false} activation={true} />,
			!!data.last_sign_in_at ? getCompleteTime(data.last_sign_in_at, true) : i18next.t('general.never'),
			parseDateFromISO(data.activated_at, false, true),
			data.app_code,
			<dd-status active={!!data.substantieel_activated_at} completed={true} />,
			i18next.t(`app.${data.substantieel_document_type}`),
		];

		this._rowLinks = [
			'',
			!!data.activated_at ? (
				getAppDeactivationLink(data.id)
			) : (
				<dd-link id='app-deactivation-link' link={`/apps/verwijderen/${data.id}/waarschuwing`}
								 text={i18next.t('app.remove')} />
			),
			'',
			'',
			'',
		];

		//Array with booleans to determine whether a row has a tooltip
		this._rowTooltips = [false, !data.activated_at, false, false, true, true, true, true];

		//Array with the tooltip content of each row
		this._rowTooltipContent = [
			'',
			this.data.active ? '' : i18next.t('app.status-tooltip'),
			'',
			'',
			i18next.t('app.code-tooltip'),
			<div>
				{!!data.substantieel_activated_at ? i18next.t('app.details-id-active') : this.IDcheckInactiveTooltip()}
			</div>,
			i18next.t('app.type-tooltip'),
		];

		this._rowIndented = [false, true, true, true, true, false, true, true];

		//Cut of the rows related to the ID-check if the ID-check is false
		if (!this.data.substantieel_activated_at) {
			this._rowNames = this._rowNames.splice(0, 6);
		}

		//If the app status is not active, remove the activated at row
		if (!this.data.activated_at) {
			[
				this._rowContent,
				this._rowNames,
				this._rowIndented,
				this._rowLinks,
				this._rowTooltips,
				this._rowTooltipContent,
			].forEach(array => {
				array.splice(3, 1);
			});
		}
	}

	//Display the data of another available app
	private displayOtherApp(data: any) {
		this.data = data;
		window.scrollTo({ top: 0 });
		this.fillData();
	}

	private addAriaDescribedby() {
		// Get all the rows with the class `dd-login-options-app-details__table-grid`
		const rows = document.querySelectorAll('.dd-login-options-app-details__table-grid');
		let lastId;

		// loop through all rows in the table
		for (let i = 0; i < rows.length; i++) {
			const row = rows[i];
			// if the row doesn't have the class 'indented-row',
			// check if the row has an id
			if (!row.classList.contains('indented-row')) {
				const rowId = row.id;
				// This code assigns the value of 'rowId' to 'lastId',
				// unless 'rowId' is null or undefined, in which case
				// it assigns 'undefined' to 'lastId'.
				lastId = rowId || undefined;
			} else if (lastId !== undefined) {
				// set the rows aria-describedby attribute to lastId
				row.setAttribute('aria-describedby', lastId);
			}
		}
	}

	render() {
		return (
			<Host class='dd-login-options-app-details'>
				<dd-modal id='pim' ref={el => (this.twoFactorWarningModal = el)} header={i18next.t('modal.2fa-warning-b')}>
					<div slot='body'>
						<p>{i18next.t('modal.2fa-warning-a')}</p>
					</div>
					<div slot='actions'>
						<dd-button
							class='dd-modal-btn'
							theme='secondary'
							touchscreen={true}
							onClick={() => {
								return this.twoFactorWarningModal.hideModal();
							}}
							text={i18next.t('modal.cancel')}
						/>
						<dd-button
							class='dd-modal-btn'
							touchscreen={true}
							onClick={() => {
								window.location.href = window.location.origin + `/apps/deactiveren/${this.data.id}/start`
							}}
							text={i18next.t('modal.yes')}
						/>
					</div>
				</dd-modal>
				<div class='dd-login-options-app-details__header-margin' />
				{/*Show the ID-check banner if the app is active and the ID-check is inactive*/}
				{!this.data?.substantieel_activated_at && this.data?.status === 'active' &&
					<dd-banner theme='idCheck' />}
				<div class="hidden-element" aria-live="polite" role='log'>
					{this.ariaUpdateText}
				</div>
				<div class='dd-login-options-app-details__button'>
					<dd-button
						theme='secondary'
						arrow='before'
						text={i18next.t('button.back-to-login')}
						onClick={() => this.returnToLogin()}
					/>
				</div>
				<section aria-labelledby='your-app-details'>
					<h1 id='your-app-details'>{i18next.t('app.title')}</h1>
					<table class='dd-login-options-app-details__table'>
						{Object.keys(this._rowNames).map((_item, index) => (
							<tr
								id={!this._rowIndented[index] ? `details-${index}` : undefined}
								class={{ 'dd-login-options-app-details__table-grid': true, 'indented-row': this._rowIndented[index] }}
							>
								{loginMethodDetailsTable(
									'app',
									index,
									this._rowNames,
									this._rowContent,
									this._rowIndented,
									this._rowLinks,
									this._rowTooltips,
									this._rowTooltipContent,
								)}
							</tr>
						))}
					</table>
				</section>
				{!!this.otherApps.length && (
					<section>
						<h2>{i18next.t('app.other')}</h2>
						<ul class='dd-login-options-app-details__card-wrapper'>
							{this.otherApps.map(app => (
								<li>
									<dd-login-options-app onClick={() => this.displayOtherApp(app)} appData={app} />
								</li>
							))}
						</ul>
					</section>
				)}
			</Host>
		);
	}
}
