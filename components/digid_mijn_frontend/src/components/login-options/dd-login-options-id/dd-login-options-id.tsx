import { Component, Host, h, Prop, Event, EventEmitter, Method, Element, State, Fragment } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { getAccordionCloseButton, getCardMoreDetailsButton } from '../../../global/global-jsx';
import { getDocumentLinks } from '../../../global/global-links';
import {
  getGlobalCards,
  getGlobalLicences,
  getStatus,
  getStatusTooltip,
  trackInteractionEvent,
} from '../../../global/global-methods';

@Component({
  tag: 'dd-login-options-id',
})
export class DdLoginOptionsId {
  @Element() el;

  /**
   * The document data object provided as input
   */
  @Prop({ mutable: true }) document: any;

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
   * Prop used to display multiple document numbers in the same card
   * @Default true
   */
  @Prop() allowMultipleDocumentNumbers = true;

  //An array of all other available documents
  @State() revokedCards: any[];

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

  componentWillRender() {
    if (this.document) {
      //Filter out the other documents to show on the bottom of the page
      if (this.document.documentType === 'id') {
        this.revokedCards = getGlobalCards().filter(card => card.status === 'ingetrokken');
      } else {
        this.revokedCards = getGlobalLicences().filter(licence => licence.status === 'ingetrokken');
      }

      this.document.parsed_status = getStatus(this.document.status);
    }
  }

  //Toggle accordion opened and closed
  private toggleAccordion() {
    this.accordionOpen = !this.accordionOpen;
    if (this.accordionOpen) {
      trackInteractionEvent('opent-accordion', `${this.document.documentType}`);
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

    trackInteractionEvent('knop-sluiten-accordion', `${this.document.documentType}`);
  }

  private get revokedDocument() {
    return this.document.status === 'ingetrokken';
  }

  private renderDocumentNumber() {
    if (this.revokedDocument && this.allowMultipleDocumentNumbers && this.revokedCards) {
      return this.revokedCards.map(card => (
        <div class="dd-login-options-id__card__docs--docnum">
          <div>{i18next.t('app.doc-num')}:</div>
          {card.doc_num}
        </div>
      ));
    } else {
      return (
        <div class="dd-login-options-id__card__docs--docnum">
          <div>{i18next.t('app.doc-num')}:</div>
          {this.document.doc_num}
        </div>
      );
    }
  }

  render() {
    return (
      <Host class="dd-login-options-id">
        {!!this.document && this.view === 'card' && (
          <div
            id={`document-${this.document.documentType}-card`}
            class={{
              'dd-login-options-id__card': true,
              'dd-login-options-id--revoked': this.revokedDocument,
            }}
          >
            <div class="dd-login-options-id__card-content">
              <div class="dd-login-options-id__card-content__title">
                <dd-icon name="id-card" />
                <h4>{i18next.t(`id.title-${this.document.documentType}`)}</h4>
              </div>
              <div class="dd-login-options-id__card__docs">{this.renderDocumentNumber()}</div>
              <div class="dd-login-options-id__card--activation-status">
                <div>{i18next.t('id.login-status-short')}:</div>
                <dd-status active={this.document.active} icon={false} statusText={this.document.parsed_status} />
              </div>
            </div>
            {!this.revokedDocument &&
              getCardMoreDetailsButton('id', i18next.t(`id.title-${this.document.documentType}`))}
          </div>
        )}
        {!!this.document && this.view === 'accordion' && (
          <div
            id={`document-${this.document.documentType}-accordion`}
            class={{
              'dd-login-options-id__accordion': true,
              'dd-login-options-id__accordion--opened': this.accordionOpen,
              'dd-login-options-id__accordion--revoked': this.revokedDocument,
            }}
            aria-expanded={this.accordionOpen}
          >
            <div
              class="dd-login-options-id__accordion__title"
              onClick={() => this.toggleAccordion()}
              onKeyDown={event => this.keydownHandler(event)}
              role="button"
              tabindex='0'
              aria-label={i18next.t('id.details-aria', {
                name: i18next.t(`id.title-${this.document.documentType}`),
              })}
              aria-controls={`accordion_idcard`}
            >
              <dd-icon class="dd-login-options-id__accordion__title--icon" name="id-card" />
              <div class="dd-login-options-id__accordion__title--text">
                <h3 class="tiny">
                  {i18next.t(`id.accordion-title-${this.document.documentType}`)}
                  <span class="dd-login-options-id__accordion__title--text__login">
                    {i18next.t('id.login')}{' '}
                    {this.document.active ? (
                      //Render Active text
                      <sup class="status-active dd-login-options-id__accordion__title--text__login--status">
                        {this.document.parsed_status}
                      </sup>
                    ) : (
                      //Render Inactive text
                      <sup class="status-inactive dd-login-options-id__accordion__title--text__login--status">
                        {this.document.parsed_status}
                      </sup>
                    )}
                  </span>
                </h3>
              </div>
              <div class="dd-login-options-id__accordion__title--chevron">
                {this.accordionOpen ? <dd-chevron direction="up" /> : <dd-chevron direction="down" />}
              </div>
            </div>

            {/* provide a unique ID in case of multiple accordions */}
            <div
              id={`accordion_idcard`}
              class={{
                'dd-login-options-id__accordion__content': true,
                'dd-login-options-id__accordion__content--opened': this.accordionOpen,
              }}
            >
              {!this.revokedDocument && (
                <Fragment>
                  <div class="dd-login-options-id__accordion__content--item">
                    <p>{i18next.t('app.doc-num')}</p>
                    <p>{this.document.doc_num}</p>
                  </div>
                  <div class="dd-login-options-id__accordion__content--item dd-login-options-id__accordion__content--tooltip">
                    <p class="title">{i18next.t('id.login-status')}</p>

                    <dd-status
                      active={this.document.active}
                      status-text={this.document.parsed_status}
                      class="content dd-login-options-id__accordion--status"
                      icon={false}
                    />
                    <div class="link">{getDocumentLinks(this.document)}</div>
                    <dd-tooltip-content id={`document-${this.document.doc_num}-status-content`} class="tooltip-content">
                      {getStatusTooltip(this.document.status, this.document.documentType)}
                    </dd-tooltip-content>
                    <dd-tooltip
                      id={`document-${this.document.doc_num}-status-button`}
                      contentId={`document-${this.document.doc_num}-status-content`}
                      ariaText={i18next.t('id.login-status')}
                    />
                  </div>
                </Fragment>
              )}
              {!!this.revokedCards.length && (
                <div class="dd-login-options-id__accordion__revoked-cards">
                  <div class="dd-login-options-id__accordion__content--item">
                    <p class="dd-login-options-id__accordion__revoked-cards__title">
                      {i18next.t(
                        `id.other-revoked-${this.document.documentType}-${this.revokedCards.length > 1 ? 'multiple' : 'single'
                        }`,
                      )}
                    </p>
                    {this.revokedCards.map(card => (
                      <p class="dd-login-options-id__accordion__revoked-cards__item">
                        <dd-icon class="dd-login-options-id__accordion__title--icon" name="id-card" />
                        {card.doc_num}
                      </p>
                    ))}
                  </div>
                </div>
              )}
              {getAccordionCloseButton(
                'id',
                () => this.closeAccordionButton(),
                event => this.keydownHandler(event),
                i18next.t('id.close-accordion', {
                  name: i18next.t(`id.title-${this.document.documentType}`),
                }),
              )}
            </div>
          </div>
        )}
      </Host>
    );
  }
}
