import { Component, Host, h, Prop, Event, EventEmitter, Method, Fragment, Element } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { getCompleteTime, parseDateFromISO } from '../../../global/global-date-and-time';
import { getAccordionCloseButton, getCardMoreDetailsButton } from '../../../global/global-jsx';
import { getAppDeactivationLink } from '../../../global/global-links';
import { trackInteractionEvent } from '../../../global/global-methods';

@Component({
  tag: 'dd-login-options-app',
})
export class DdLoginOptionsApp {
  @Element() el;

  /**
   * The app data object provided as input
   */
  @Prop({ mutable: true }) appData: any;

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
  private toggleAccordion() {
    this.accordionOpen = !this.accordionOpen;
    //check if the accordion should be opened or closed
    if (this.accordionOpen) {
      trackInteractionEvent('opent-accordion', `digid app ${this.appData.device_name}`);
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
    //Scroll the top of the accordion back into the view
    this.scrollAccordionToTop();
    this.closeAccordion();

    trackInteractionEvent('knop-sluiten-accordion', `digid app ${this.appData.device_name}`);
  }

  render() {
    return (
      <Host class="dd-login-options-app">
        {this.view === 'card' && (
          <div id={`app_${this.appData.id}-card`} class="dd-login-options-app__card">
            <div class="dd-login-options-app__card-content">
              <div class="dd-login-options-app__card-content__title">
                <dd-icon name="phone" />
                <h4 class="dd-login-options-app__card__title">{this.appData.device_name}</h4>
              </div>
              <div class="dd-login-options-app__card__logged-in">
                {i18next.t('app.log-card')}{' '}
                {!!this.appData.last_sign_in_at
                  ? getCompleteTime(this.appData.last_sign_in_at, false)
                  : i18next.t('general.never')}
              </div>
              {
                //Display the green Icon and Text if the ID-check had succeeded
                //Display the red Icon and Text if the ID-check has not succeeded
                !!this.appData.substantieel_activated_at ? (
                  //Render Active text and Icon
                  <div class="dd-login-options-app__card__id-check">
                    <dd-icon name="safety-on" />
                    <p class="status-active">{i18next.t('id.ID-active')}</p>
                  </div>
                ) : (
                  //Render Inactive text and Icon
                  <div class="dd-login-options-app__card__id-check">
                    <dd-icon name="safety-off" />
                    <p class="status-inactive">{i18next.t('id.ID-inactive')}</p>
                  </div>
                )
              }
            </div>
            {getCardMoreDetailsButton('app', this.appData.device_name)}
          </div>
        )}{' '}
        {this.view === 'accordion' && (
          <div
            id={`app_${this.appData.id}-accordion`}
            class={{
              'dd-login-options-app__accordion': true,
              'dd-login-options-app__accordion--opened': this.accordionOpen,
            }}
						aria-expanded={this.accordionOpen}
          >
            <div
              class="dd-login-options-app__accordion__title"
              onClick={() => this.toggleAccordion()}
              onKeyDown={event => this.keydownHandler(event)}
              tabindex="0"
              role="button"
              aria-label={i18next.t('app.details-aria', { name: this.appData.device_name })}
              aria-controls={`accordion_${this.appData.id}`}
            >
              <dd-icon class="dd-login-options-app__accordion__title--icon" name="phone" />
              <div class="dd-login-options-app__accordion__title--text">
                <h3 class="tiny">
                  {i18next.t('app.title')}
                  <span class="dd-login-options-app__accordion__title--text__login">{this.appData.device_name}</span>
                </h3>
              </div>
              <div class="dd-login-options-app__accordion__title--chevron">
                {this.accordionOpen ? <dd-chevron direction="up" /> : <dd-chevron direction="down" />}
              </div>
            </div>

            {/* provide a unique ID in case of multiple accordions */}
            <div
              id={`accordion_${this.appData.id}`}
              class={{
                'dd-login-options-app__accordion__content': true,
                'dd-login-options-app__accordion__content--opened': this.accordionOpen,
              }}
            >
              <div class="dd-login-options-app__accordion__content--item dd-login-options-app__accordion__content--tooltip">
                <p class="title">{i18next.t('app.status')}</p>
                <dd-status class="content" active={!!this.appData.activated_at} icon={false} activation={true} />
                <div class="link">
                  {!!this.appData.activated_at ? (
                    getAppDeactivationLink(this.appData.id)
                  ) : (
                    <dd-link
                      link={`/apps/verwijderen/${this.appData.id}/waarschuwing`}
                      text={i18next.t('app.remove')}
                    />
                  )}
                </div>
                {!this.appData.activated_at && (
                  <Fragment>
                    <dd-tooltip
                      id={`app-status-${this.appData.id}-button`}
                      contentId={`app-status-${this.appData.id}-content`}
                      ariaText={i18next.t('app.status')}
                      class="icon"
                    />
                    <dd-tooltip-content id={`app-status-${this.appData.id}-content`} class="tooltip-content">
                      {i18next.t('app.status-tooltip')}
                    </dd-tooltip-content>
                  </Fragment>
                )}
              </div>

              <div class="dd-login-options-app__accordion__content--item">
                <p>{i18next.t('app.log-card')}</p>
                <p>
                  {!!this.appData.last_sign_in_at
                    ? getCompleteTime(this.appData.last_sign_in_at, true)
                    : i18next.t('general.never')}
                </p>
              </div>

              {this.appData.activated_at && (
                <Fragment>
                  <div class="dd-login-options-app__accordion__content--item">
                    <p>{i18next.t('app.activated')}</p>
                    <p>{parseDateFromISO(this.appData.activated_at, false, true)}</p>
                  </div>
                </Fragment>
              )}

              <div class="dd-login-options-app__accordion__content--item dd-login-options-app__accordion__content--tooltip">
                <p class="title">{i18next.t('app.code')}</p>
                <p class="content">{this.appData.app_code}</p>
                <dd-tooltip
                  id={`app-code-${this.appData.id}-button`}
                  contentId={`app-code-${this.appData.id}-content`}
                  ariaText={i18next.t('app.code')}
                  class="icon"
                />
                <dd-tooltip-content id={`app-code-${this.appData.id}-content`} class="tooltip-content">
                  {i18next.t('app.code-tooltip')}
                </dd-tooltip-content>
              </div>

              <div class="dd-login-options-app__accordion__content--item dd-login-options-app__accordion__content--tooltip">
                <p class="title">{i18next.t('id.ID-check')}</p>
                <dd-status class="content" active={!!this.appData.substantieel_activated_at} completed={true} />
                <dd-tooltip
                  id={`ID-check-${this.appData.id}-button`}
                  contentId={`ID-check-${this.appData.id}-content`}
                  ariaText={i18next.t('id.ID-check')}
                  class="icon"
                />
                <dd-tooltip-content id={`ID-check-${this.appData.id}-content`} class="tooltip-content">
                  {this.appData.substantieel_activated_at
                    ? i18next.t('app.details-id-active')
                    : i18next.t('app.details-id-inactive')}
                </dd-tooltip-content>
              </div>
              {this.appData.substantieel_activated_at && (
                <Fragment>
                  <div class="dd-login-options-app__accordion__content--item">
                    <p>{i18next.t('app.type')}</p>
                    <p>{i18next.t(`app.${this.appData.substantieel_document_type}`)}</p>
                  </div>
                </Fragment>
              )}
              {getAccordionCloseButton(
                'app',
                () => this.closeAccordionButton(),
                event => this.keydownHandler(event),
                i18next.t('app.close-accordion', { name: this.appData.device_name }),
              )}
            </div>
          </div>
        )}
      </Host>
    );
  }
}
