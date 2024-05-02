import { Component, Host, h, Prop, Fragment, Event, EventEmitter, Method, Element } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { parseDateFromISO } from '../../../global/global-date-and-time';
import { getAccordionCloseButton, getCardMoreDetailsButton } from '../../../global/global-jsx';
import { getSMSlinks } from '../../../global/global-links';
import { getGlobalApps, getSMStooltip, trackInteractionEvent } from '../../../global/global-methods';
@Component({
  tag: 'dd-login-options-username',
})
export class DdLoginOptionsUsername {
  @Element() el;
  /**
   * The app data object provided as input
   */
  @Prop({ mutable: true }) accountData: any;

  /**
   * Whether the component is shown as a card or as an accordion
   * @Default card
   */
  @Prop({ mutable: true }) view: string = 'card';

  /**
   * Whether the accordion is opened
   * @Default false
   */
  @Prop({ mutable: true }) accordionOpen: boolean = false;

  /**
   * An event triggered when the accordion is opened by the user
   */
  @Event({ bubbles: true, composed: true }) accordionOpened: EventEmitter<string>;

  //Close accordion either from the toggle method or from the parent component
  @Method()
  async closeAccordion() {
    //close accordion
    this.accordionOpen = false;
  }

  //Toggle accordion opened and closed
  toggleAccordion() {
    this.accordionOpen = !this.accordionOpen;
    if (this.accordionOpen) {
      trackInteractionEvent('opent-accordion', 'username');
    }
  }

  private keydownHandler(event) {
    if (event.key === 'Enter') {
      this.toggleAccordion();
    }
  }

  private scrollAccordionToTop() {
    this.el.scrollIntoView({ behavior: 'auto', block: 'start' });
  }

  private closeAccordionButton() {
    this.closeAccordion();
    //Scroll the top of the accordion back into the view
    this.scrollAccordionToTop();

    trackInteractionEvent('knop-sluiten-accordion', 'username');
  }

  //Show warning if accordion is closed, two factor authentication is switched off and the user is able to turn it on
  private showAccordion2FAwarning() {
    return (
      !this.accordionOpen &&
      this.accountData.zekerheidsniveau === 10 &&
      (this.accountData.sms === 'active' || getGlobalApps().length > 0)
    );
  }

  render() {
    return (
      <Host class="dd-login-options-username">
        {this.view === 'card' && (
          <div id={'username-card'} class="dd-login-options-username__card">
            <div class="dd-login-options-username__card-content">
              <div class="dd-login-options-username__card-content__title">
                <dd-icon name="username" />
                <h4 class="loginName">{i18next.t('username.title-card')}</h4>
              </div>
              <div class="dd-login-options-username__card--userpw">
                <div class="dd-login-options-username__card--userpw--label">{i18next.t('username.usern')}: </div>
                <div class="dd-login-options-username__card--userpw--data">{this.accountData.username}</div>
              </div>
              <div class="dd-login-options-username__card--userpw">
                <div class="dd-login-options-username__card--userpw--label">{i18next.t('username.pw')}: </div>
                <div class="dd-login-options-username__card--userpw--data">••••••••••</div>
              </div>
              {this.accountData.sms === 'active' ? (
                //Render Active text and Icon
                <div class="dd-login-options-username__card--sms">
                  <dd-icon name="safety-on" />
                  <p class="status-active">{i18next.t('username.sms-active')}</p>
                </div>
              ) : (
                //Render Inactive text and Icon
                <div class="dd-login-options-username__card--sms">
                  <dd-icon name="safety-off" />
                  <p class="status-inactive">{i18next.t(`username.sms-${this.accountData.sms}`)}</p>
                </div>
              )}

              {this.accountData.zekerheidsniveau === 20 ? (
                //Render Active text and Icon
                <div class="dd-login-options-username__card--twofa">
                  <dd-icon name="safety-on" />
                  <p class="status-active">{i18next.t('username.two-fa-active')}</p>
                </div>
              ) : (
                //Render Inactive text and Icon
                <div class="dd-login-options-username__card--twofa">
                  <dd-icon name="safety-off" />
                  <p class="status-inactive">{i18next.t('username.two-fa-inactive')}</p>
                </div>
              )}
            </div>
            {getCardMoreDetailsButton('username')}
          </div>
        )}
        {this.view === 'accordion' && (
          <div
            class={{
              'dd-login-options-username__accordion': true,
              'dd-login-options-username__accordion--opened': this.accordionOpen,
            }}
						aria-expanded={this.accordionOpen}
          >
            <div
              class="dd-login-options-username__accordion__title"
              onClick={() => this.toggleAccordion()}
              onKeyDown={event => this.keydownHandler(event)}
              tabindex="0"
              role="button"
              aria-label={i18next.t('username.accordion-aria')}
              aria-controls={`accordion_username`}

            >
              <dd-icon class="dd-login-options-username__accordion__title--icon" name="username" />
              <div class="dd-login-options-username__accordion__title--text">
                <h3 class="tiny">
                  {i18next.t('username.title')}
                  <span class="dd-login-options-username__accordion__title--text__login">
                    {this.accountData.username}
                  </span>
                </h3>
              </div>
              <div class="dd-login-options-username__accordion__title--chevron">
                {this.accordionOpen ? <dd-chevron direction="up" /> : <dd-chevron direction="down" />}
              </div>
            </div>

            {this.showAccordion2FAwarning() && (
              <div class="dd-login-options-username__accordion-warning">
                <dd-icon name="safety-off" />
                <p class="tiny status-inactive">{i18next.t('username.two-fa-inactive')}</p>
              </div>
            )}
            <div
              id={`username-accordion`}
              class={{
                'dd-login-options-username__accordion__content': true,
                'dd-login-options-username__accordion__content--opened': this.accordionOpen,
              }}
            >
              <div class="dd-login-options-username__accordion__content--item dd-login-options-username__accordion__content--tooltip">
                <p class="title">{i18next.t('username.pw')}</p>
                <p class="content">••••••••••</p>
                <dd-link
                  class="link"
                  link={
                    !!getGlobalApps().length ? '/keuze_wachtwoord_wijzigen_opnieuw_instellen' : '/wachtwoord_wijzigen'
                  }
                  text={i18next.t('username.pw-link')}
                />
                <dd-tooltip
                  id={`username-pw-button`}
                  ariaText={i18next.t('username.pw')}
                  contentId={`username-pw-content`}
                  class="icon"
                />
                <dd-tooltip-content id={`username-pw-content`} class="tooltip-content">
                  {i18next.t('username.pw-tooltip')}
                </dd-tooltip-content>
              </div>

              <div class="dd-login-options-username__accordion__content--item dd-login-options-username__accordion__content--tooltip">
                <p class="title">{i18next.t('username.sms')}</p>
                <dd-status
                  class="content"
                  active={this.accountData.sms === 'active'}
                  activation={this.accountData.sms === 'pending'}
                />
                <div class="link">{getSMSlinks(this.accountData.sms, this.accountData.zekerheidsniveau)}</div>
                <dd-tooltip
                  id={`username-sms-button`}
                  ariaText={i18next.t('username.sms')}
                  contentId={`username-sms-content`}
                  class="icon"
                />
                <dd-tooltip-content id={`username-sms-content`} class="tooltip-content">
                  {getSMStooltip(this.accountData.sms)}
                </dd-tooltip-content>
              </div>
              {this.accountData.sms !== 'inactive' && (
                <Fragment>
                  <div class="dd-login-options-username__accordion__content--item dd-login-options-username__accordion__content--tooltip">
                    <p class="title">{i18next.t('username.tel')}</p>
                    <p class="content">{this.accountData.phone_number}</p>
                    <dd-link
                      class="link"
                      link={this.accountData.phone_number ? '/telefoonnummer/nieuw' : '/sms_uitbreiding'}
                      text={i18next.t(`username.tel-link${this.accountData.phone_number ? '' : '-add'}`)}
                    />
                    <dd-tooltip
                      id={`username-tel-button`}
                      ariaText={i18next.t('username.tel')}
                      contentId={`username-tel-content`}
                      class="icon"
                    />
                    <dd-tooltip-content id={`username-tel-content`} class="tooltip-content">
                      {i18next.t('username.tel-tooltip')}
                    </dd-tooltip-content>
                  </div>

                  <div class="dd-login-options-username__accordion__content--item dd-login-options-username__accordion__content--tooltip">
                    <p class="title">{i18next.t('username.spoken-sms')}</p>
                    <p class="content">
                      {this.accountData.gesproken_sms ? i18next.t('general.active') : i18next.t('general.inactive')}
                    </p>{' '}
                    <dd-link
                      class="link"
                      link="/gesproken_sms"
                      text={
                        this.accountData.gesproken_sms
                          ? i18next.t('username.spoken-sms-link-deactivate')
                          : i18next.t('username.spoken-sms-link-activate')
                      }
                    />
                    <dd-tooltip-content id={`username-spoken-sms-content`} class="tooltip-content">
                      {this.accountData.gesproken_sms
                        ? i18next.t('username.spoken-sms-tooltip-active')
                        : i18next.t('username.spoken-sms-tooltip-inactive')}
                    </dd-tooltip-content>
                    <dd-tooltip
                      id={`username-spoken-sms-button`}
                      contentId={`username-spoken-sms-content`}
                      ariaText={i18next.t('username.spoken-sms')}
                      class="icon"
                    />
                  </div>
                </Fragment>
              )}
              <div class="dd-login-options-username__accordion__content--item dd-login-options-username__accordion__content--tooltip">
                <p class="title narrow"> {i18next.t('username.two-fa')}</p>
                <dd-status class="content" active={this.accountData.zekerheidsniveau === 20} />
                <dd-link
                  class="link narrow"
                  link="/inloggen_voorkeur"
                  text={
                    this.accountData.zekerheidsniveau === 20
                      ? i18next.t('username.two-fa-link-off')
                      : i18next.t('username.two-fa-link-on')
                  }
                />
                <dd-tooltip
                  id={`username-2FA-button`}
                  contentId={`username-2FA-content`}
                  ariaText={i18next.t('username.two-fa')}
                  class="icon"
                />
                <dd-tooltip-content id={`username-2FA-content`} class="tooltip-content">
                  {i18next.t('username.two-fa-exp')}
                </dd-tooltip-content>
              </div>

              {this.accountData.zekerheidsniveau === 20 && (
                <Fragment>
                  <div class="dd-login-options-username__accordion__content--item">
                    <p> {i18next.t('username.two-fa-since-long')}</p>
                    <p>{this.accountData.two_faactivated_date ? parseDateFromISO(this.accountData.two_faactivated_date) : i18next.t('app.null')}</p>
                  </div>
                </Fragment>
              )}
              {getAccordionCloseButton(
                'username',
                () => this.closeAccordionButton(),
                event => this.keydownHandler(event),
                i18next.t('username.close-accordion'),
              )}
            </div>
          </div>
        )}
      </Host>
    );
  }
}
