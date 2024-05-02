import { Component, Host, Prop, Event, EventEmitter, State, Fragment, Listen, h } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../../utils/i18next';
import {
  clearToasts,
  globalErrorHandler,
  setDeceased,
  setDocumentTitle,
  setAppIsLastLoginMethod,
  trackPageView,
  setShow2FAWarningWhenDeactivatingApp,
  getAriaUpdateText,
  setAriaUpdateText,
} from '../../../global/global-methods';
import ddService from '../../../api/services/dd.login-methods.service';
import { getCompleteTime } from '../../../global/global-date-and-time';
import sessionService from '../../../api/services/dd.session.service';
import { contactHelpdeskLink } from '../../../global/global-jsx';

@Component({
  tag: 'dd-login-options',
})
export class DdLoginOptions {
  // Add a media Query event Listener to check the viewport
  mediaQuery = matchMedia('(max-width: 432px)');
  @State() isMobile: boolean;

  //Property used to access the router
  @Prop() history: RouterHistory;

  //Text used to update text to speech users
  @State() ariaUpdateText = getAriaUpdateText();

  //Array of apps to display the users current DigiD apps
  @State() appsArray: any[];

  //Account data of the logged in user retrieved from the database
  @State() accountData: any;

  //Document data of the logged in user retrieved from the database
  @State() docData: any;

  //Boolean stating whether the logged in user has two linked documents
  @State() twoDocuments = false;

  //Whether the app cards should be hidden
  @State() disableApps = false;

  //Object containing the identity document
  @State() identityDocument;

  //Object containing the driving licence
  @State() drivingLicence;

  //Emit an event when the user navigates via a button, to update the header menu
  @Event({ bubbles: true, composed: true }) navigationClicked: EventEmitter<string>;

  componentWillLoad() {
    setDocumentTitle('Inlogmethodes');
    trackPageView();
    return Promise.all([this.fetchAppsData(), this.fetchAccountData(), this.fetchDocumentData()]).then(() => {
      //determine whether the user has only 1 login method
      setAppIsLastLoginMethod(this.appIsLastLoginMethod());
      setShow2FAWarningWhenDeactivatingApp(this.show2FAWarningWhenDeactivatingApp());

      return sessionService.updateSession();
    });
  }

  componentWillRender() {
    document.title = i18next.t('login.page-title');
  }

  componentDidLoad() {
    this.disableAriaUpdateText();
    // This checks if the mediaQuery contains a valid addEventListener function
    if (this.mediaQuery?.matches) {
      this.checkViewport(this.mediaQuery);
      this.mediaQuery.addEventListener('change', this.checkViewport)
    }
  }

  disconnectedCallback() {
    //Clear all alert messages
    return clearToasts();
  }

  @Listen('bannerClosed')
  updateAriaUsers(event) {
    this.ariaUpdateText = event.detail;
  }

  private disableAriaUpdateText() {
    //set ariaUpdateText to an empty string to make sure the text doesnt get read out again
    setAriaUpdateText('');
  }

  private fetchAppsData(): Promise<void> {
    return ddService
      .getApps()
      .then(data => {
        //Hide the apps if the status is disabled
        this.disableApps = data.status === 'DISABLED';

        const unsortedApps = data.app_authenticators;
        //Sort the apps on last sign in at descending order with null last
        const longAgo = new Date(0);
        this.appsArray = unsortedApps.sort((app1, app2) => {
          let dateA = app2.last_sign_in_at ? new Date(app2.last_sign_in_at) : longAgo;
          let dateB = app1.last_sign_in_at ? new Date(app1.last_sign_in_at) : longAgo;
          return dateA.getTime() - dateB.getTime();
        });
      })
      .catch(() => {
        globalErrorHandler();
      });
  }

  private fetchAccountData(): Promise<void> {
    return ddService
      .getAccountData()
      .then(data => {
        this.accountData = data.combined_account_data;
        setDeceased(this.accountData.deceased);
      })
      .catch(() => {
        globalErrorHandler();
      });
  }

  private sortDocumentsAndReturnNewest(documentArray) {
    // @ts-ignore
    return documentArray.sort((a, b) => new Date(b.activated_at) - new Date(a.activated_at))[0];
  }

  private assignDocumentData(data) {
    this.docData = data;
    if (!!this.docData.driving_licences.length && !!this.docData.identity_cards.length) {
      this.twoDocuments = true;
    }
    if (!!this.docData.driving_licences.length) {
      this.drivingLicence = this.sortDocumentsAndReturnNewest(this.docData.driving_licences);
    }
    if (!!this.docData.identity_cards.length) {
      this.identityDocument = this.sortDocumentsAndReturnNewest(this.docData.identity_cards);
    }
  }

  private fetchDocumentData(): Promise<void> {
    return ddService
      .getDocumentData()
      .then(data => {
        this.assignDocumentData(data);
      })
      .catch(() => {
        globalErrorHandler();
      });
  }

  private navigateToDetails(link: string, data: any) {
    //dont allow navigation if the event takes place with a revoked document
    if ((data.username || data.status) && data.status !== 'ingetrokken') {
      this.history.push(link, { data });
    }
  }

  private keydownNavigateToDetails(event, link, content) {
    if (event.key === 'Enter') {
      this.navigateToDetails(link, content);
    }
  }

  private navigateToUsageHistory() {
    if (this.history) {
      this.history.push(`/gebruiksgeschiedenis`, {});
    }
    this.navigationClicked.emit('gebruiksgeschiedenis');
  }

  private deleteDigid() {
    window.location.href = window.location.origin + '/bevestigen_opheffen_digid';
  }

  private lastSignIn() {
    if (this.accountData && this.accountData.last_sign_in_at) {
      return (
        ' ' +
        i18next.t('login.sub-header-2') +
        ' ' +
        getCompleteTime(this.accountData.last_sign_in_at, true) +
        ' (' +
        i18next.t('general.dutch-time') +
        ').'
      );
    }
    return '';
  }

  //Property used to determine if the current user only has 1 app as login method
  private appIsLastLoginMethod() {
    return !!this.appsArray && this.appsArray.length === 1 && !this.accountData.username;
  }

  private show2FAWarningWhenDeactivatingApp() {
    return (
      !!this.appsArray &&
      this.appsArray.length === 1 &&
      this.accountData.zekerheidsniveau === 20 &&
      (!this.accountData.username || (this.accountData.username && this.accountData.sms === 'active')) &&
      !this.identityDocument &&
      !this.drivingLicence
    );
  }

  private hasNoDocuments() {
    return this.docData && !this.hasIdentityDocument() && !this.hasDrivingLicence();
  }

  private hasIdentityDocument() {
    return this.docData.show_identity_cards && (!!this.identityDocument || this.docData.rvig_error);
  }

  private hasDrivingLicence() {
    return this.docData.show_driving_licences && (!!this.drivingLicence || this.docData.rdw_error);
  }

  private renderApps() {
    if (this.disableApps) {
      return (
        <dd-empty-state class="dd-login-options__app-disabled" type="app" error={true}>
          {i18next.t('empty-state.apps-disabled')}
        </dd-empty-state>
      );
    }
    return (
      <Fragment>
        <p class="dd-login-options__p">{i18next.t('login.digid-app-sub')}</p>
        <ul class="dd-login-options__card-wrapper">
          {this.appsArray.map(app => (
            <li>
              <dd-login-options-app
                onClick={() => this.navigateToDetails(`/digid-app-details`, app)}
                onKeyDown={event => this.keydownNavigateToDetails(event, `/digid-app-details`, app)}
                appData={app}
              />
            </li>
          ))}
        </ul>
      </Fragment>
    );
  }

  private renderIdentityDocument() {
    if (this.docData.rvig_error) {
      return (
        <li>
          <dd-empty-state type="id" error={true}>
            {i18next.t('empty-state.no-connection-RVIG')}
            {this.docData.rvig_error === 'try_again' && i18next.t('empty-state.try-again')}
            {this.docData.rvig_error === 'contact' && contactHelpdeskLink()}
          </dd-empty-state>
        </li>
      );
    }
    return (
      <li>
        <dd-login-options-id
          class={{ 'dd-login-options__id-card-area__two-docs': this.twoDocuments }}
          onClick={() => this.navigateToDetails(`/document-details`, this.identityDocument)}
          onKeyDown={event => this.keydownNavigateToDetails(event, `/document-details`, this.identityDocument)}
          document={this.identityDocument}
        />
      </li>
    );
  }

  private renderDrivingLicence() {
    if (this.docData.rdw_error) {
      return (
        <li>
          <dd-empty-state type="id" error={true}>
            {i18next.t('empty-state.no-connection-RDW')}
            {this.docData.rdw_error === 'try_again' && i18next.t('empty-state.try-again')}
            {this.docData.rdw_error === 'contact' && contactHelpdeskLink()}
          </dd-empty-state>
        </li>
      );
    }
    return (
      <li>
        <dd-login-options-id
          onClick={() => this.navigateToDetails(`/document-details`, this.drivingLicence)}
          onKeyDown={event => this.keydownNavigateToDetails(event, `/document-details`, this.drivingLicence)}
          document={this.drivingLicence}
        />
      </li>
    );
  }

  private renderBanner() {
    if (this.accountData && this.accountData.username && !localStorage.getItem('hideBanner')) {
      //Render the two factor promoting banner if the user has sms verification or at least 1 active DigiD app
      if (
        this.accountData.zekerheidsniveau === 10 &&
        (this.accountData.sms === 'active' ||
          (this.appsArray?.length > 0 && this.appsArray.some(app => app.status === 'active')))
      ) {
        return <dd-banner theme="twoFactor" />;

        //Render the DigiD app promoting banner if the user had no sms verification or DigiD apps
      } else if ((this.accountData.sms === 'pending' || 'inactive') && this.appsArray.length === 0) {
        return <dd-banner theme="app" />;
      }
    }
  }

  // Check the view of mobile and desktop
  private checkViewport = (mediaQuery) => {
    this.isMobile = mediaQuery.matches;
  };

  render() {
    return (
      <Host class="dd-login-options">
        <div class="dd-login-options__header-margin" />

        <div class="dd-login-options__status" aria-live="polite" role="log">
          {this.ariaUpdateText}
        </div>

        {!this.accountData?.deceased && this.appsArray && this.renderBanner()}
        <dd-welcome data={this.accountData} />
        {this.isMobile ? (
          <section class="dd-login-options__mobile">
            <h2>{i18next.t('login.title')}</h2>
            <dd-login-options-accordion-wrapper
              appsArray={this.appsArray}
              accountData={this.accountData}
              docData={this.docData}
              appsDisabled={this.disableApps}
              hasNoDocuments={this.hasNoDocuments()}
            />
          </section>
        ) : (
          <section class="dd-login-options__methods">
            <h2 class="h2-large">{i18next.t('login.title')}</h2>
            <p class="dd-login-options__p">{i18next.t('login.sub-header-1') + this.lastSignIn()}</p>

            <div class="container">
              <h3 class="digid">{i18next.t('app.title')}</h3>
              <div>{!!this.appsArray?.length ? this.renderApps() : <dd-empty-state type="app" />}</div>
            </div>

            <div class="dd-login-options__card-grid">
              <div class="container dd-login-options__id-card-area">
                <h3 class="dd-login-options__card-grid__header digid">{i18next.t('id.title')}</h3>
                {this.docData && (
                  <ul class="dd-login-options__card-wrapper">
                    {this.hasIdentityDocument() && this.renderIdentityDocument()}
                    {this.hasDrivingLicence() && this.renderDrivingLicence()}
                  </ul>
                )}
                {this.hasNoDocuments() && <dd-empty-state documentData={this.docData} type="id" />}
              </div>

              {!!this.accountData && this.accountData.username && (
                <div class="container dd-login-options__username-card-area">
                  <h3 class="dd-login-options__card-grid__header digid">{i18next.t('username.title')}</h3>
                  <dd-login-options-username
                    onClick={() => this.navigateToDetails(`/gebruiker-details`, this.accountData)}
                    onKeyDown={event => this.keydownNavigateToDetails(event, `/gebruiker-details`, this.accountData)}
                    accountData={this.accountData}
                  />
                </div>
              )}
            </div>
          </section>
        )}
        {!!this.accountData && (
          <section>
            <dd-login-options-contact-info accountData={this.accountData} />
          </section>
        )}
        <section>
          {this.isMobile ? (
            <div class="dd-login-options__mobile">
              <h2>{i18next.t('history.header')}</h2>
            </div>
          ) : (
            <div>
              <h2 class="h2-large">{i18next.t('history.header')}</h2>
            </div>
          )}
          <p>
            {i18next.t('history.subtitle')} {contactHelpdeskLink()}
          </p>
          <dd-button
            id="navToHistBtn"
            onClick={() => this.navigateToUsageHistory()}
            text={i18next.t('history.btn')}
            arrow="after"
          />
        </section>
        {this.accountData && !this.accountData.deceased && (
          <section>
            {this.isMobile ? (
              <div class="dd-login-options__mobile">
                <h2>{i18next.t('login.delete-title')}</h2>
              </div>
            ) : (
              <div>
                <h2 class="h2-large">{i18next.t('login.delete-title')}</h2>
              </div>
            )}
            <p>{i18next.t('login.delete-subtitle')}</p>
            <dd-button
              id="deleteBtn"
              class="dd-login-options__deleteBtn"
              onClick={() => this.deleteDigid()}
              theme="secondary"
              text={i18next.t('login.delete-btn')}
              arrow="after"
            />
          </section>
        )}
      </Host>
    );
  }
}
