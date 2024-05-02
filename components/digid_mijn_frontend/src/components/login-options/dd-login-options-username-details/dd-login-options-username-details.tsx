import { Component, Host, h, Prop, State, Listen } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../../utils/i18next';
import { loginMethodDetailsTable } from '../../../global/global-jsx';
import { parseDateFromISO } from '../../../global/global-date-and-time';
import { getSMSlinks } from '../../../global/global-links';
import {
	getSMStooltip,
	getGlobalApps,
	setDocumentTitle,
	trackPageView,
} from '../../../global/global-methods';
import ddService from '../../../api/services/dd.login-methods.service';
import sessionService from '../../../api/services/dd.session.service';

@Component({
  tag: 'dd-login-options-username-details',
})
export class DdLoginOptionsUsernameDetails {
  @Prop() history: RouterHistory;

  @Prop({ mutable: true }) userData: any;

	//Text used to update text to speech users
	@State() ariaUpdateText : string;

  //Table arrays
  @State() _rowNames = [];
  @State() _rowContent;
  @State() _rowLinks;
  @State() _rowTooltips;
  @State() _rowIndented = [false, false, false, true, true, false, true];
  @State() _rowTooltipContent;

  componentWillLoad() {
    setDocumentTitle('Username details');
    trackPageView();

    //extract passed data
    if (this.history && this.history.location.state) {
      this.userData = this.history.location.state.data;
    }
    //If navigating through the URL, retrieve the userdata
    else {
      return ddService
        .getAccountData()
        .then(data => {
          this.userData = data.combined_account_data;
        })
        .catch(err => {
          console.error(err);
        })
        .finally(() => {
          //If the user has no username, send back to home
          if (!this.userData.username) {
            this.returnToLogin();
          }
        });
    }
  }

  componentWillRender() {
    document.title = i18next.t('username.page-title');
    this.fillData();
  }

  componentDidLoad() {
    this.addAriaDescribedby();
    sessionService.updateSession();
  }

	@Listen('bannerClosed')
	updateAriaUsers(event){
		this.ariaUpdateText = (event.detail);
	}

  private returnToLogin() {
    this.history.push('/home');
  }

  //Fill data that could be re-rendered
  private fillData() {
    const data = this.userData;

    this._rowNames = [
      i18next.t('username.usern'),
      i18next.t('username.pw'),
      i18next.t('username.sms'),
      i18next.t('username.tel'),
      i18next.t('username.spoken-sms'),
      i18next.t('username.two-fa'),
      i18next.t('username.two-fa-since'),
    ];

    this._rowContent = [
      data.username,
      '••••••••••',
      <dd-status active={data.sms === 'active'} activation={data.sms === 'pending'} />,
      data.phone_number,
      <div>{data.gesproken_sms ? i18next.t('general.active') : i18next.t('general.inactive')}</div>,
      <dd-status active={data.zekerheidsniveau === 20} />,
			data.two_faactivated_date ? parseDateFromISO(data.two_faactivated_date, false, true) : i18next.t('app.null')
    ];

    this._rowLinks = [
      '',
      <dd-link
        link={!!getGlobalApps().length ? '/keuze_wachtwoord_wijzigen_opnieuw_instellen' : '/wachtwoord_wijzigen'}
        text={i18next.t('username.pw-link')}
      />,
      getSMSlinks(data.sms, data.zekerheidsniveau),
      <dd-link
        link={data.phone_number ? '/telefoonnummer/nieuw' : '/sms_uitbreiding'}
        text={i18next.t(`username.tel-link${data.phone_number ? '' : '-add'}`)}
      />,
      <dd-link
        link="/gesproken_sms"
        text={
          data.gesproken_sms
            ? i18next.t('username.spoken-sms-link-deactivate')
            : i18next.t('username.spoken-sms-link-activate')
        }
      />,
      <dd-link
        link="/inloggen_voorkeur"
        text={
          data.zekerheidsniveau === 20 ? i18next.t('username.two-fa-link-off') : i18next.t('username.two-fa-link-on')
        }
      />,
    ];

    this._rowTooltips = [false, true, true, true, true, data.zekerheidsniveau === 10, false, true];

    this._rowTooltipContent = [
      '',
      i18next.t('username.pw-tooltip'),
      getSMStooltip(data.sms),
      i18next.t('username.tel-tooltip'),
      data.spokenSMS
        ? i18next.t('username.spoken-sms-tooltip-active')
        : i18next.t('username.spoken-sms-tooltip-inactive'),
      i18next.t('username.two-fa-exp'),
      '',
    ];

    //Cut of the rows related to the ID-check if the ID-check is false
    if (this.userData.zekerheidsniveau === 10) {
      this._rowNames = this._rowNames.splice(0, 6);
    }

    //If SMS verification is inactive, remove the phone number and spoken SMS rows
    if (this.userData.sms === 'inactive') {
      [
        this._rowContent,
        this._rowNames,
        this._rowIndented,
        this._rowLinks,
        this._rowTooltips,
        this._rowTooltipContent,
      ].forEach(array => {
        array.splice(3, 2);
      });
    }
  }

  private addAriaDescribedby() {
      // Get all the rows with the class `dd-login-options-username-details__table-grid`
    const rows = document.querySelectorAll('.dd-login-options-username-details__table-grid');
    let lastId;

    // Loop through each row
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
      <Host class="dd-login-options-username-details">
        <div class="dd-login-options-username-details__header-margin" />
        {this.userData && this.userData.zekerheidsniveau === 10 && this.userData.sms === 'active' && (
          <dd-banner theme="twoFactor" />
        )}
				<div class="hidden-element" aria-live="polite" role='log'>
					{this.ariaUpdateText}
				</div>
        <div class="dd-login-options-username-details__button">
          <dd-button
            theme="secondary"
            arrow="before"
            text={i18next.t('button.back-to-login')}
            onClick={() => this.returnToLogin()}
          />
        </div>
        <h1>{i18next.t('username.title')}</h1>
        <table class="dd-login-options-username-details__table">
          {Object.keys(this._rowNames).map((_item, index) => (
            <tr
              id={!this._rowIndented[index] ? `details-${index}` : undefined}
              class={{
                'dd-login-options-username-details__table-grid': true,
                'indented-row': this._rowIndented[index],
              }}
            >
              {loginMethodDetailsTable(
                'username',
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
      </Host>
    );
  }
}
