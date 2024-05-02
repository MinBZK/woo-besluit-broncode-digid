import { Component, Host, h, Prop, State } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../../utils/i18next';
import { getDocumentLinks } from '../../../global/global-links';
import { loginMethodDetailsTable } from '../../../global/global-jsx';
import {
  canRequestPinOnDrivingLicences,
  getGlobalCards,
  getGlobalLicences,
  getStatus,
  getStatusTooltip,
  setDocumentTitle,
  trackPageView,
} from '../../../global/global-methods';
import ddService from '../../../api/services/dd.login-methods.service';
import sessionService from '../../../api/services/dd.session.service';

@Component({
  tag: 'dd-login-options-id-details',
})
export class DdLoginOptionsIdDetails {
  @Prop() history: RouterHistory;

  //The document data
  @Prop({ mutable: true }) docData: any;

  //The title of the page, to be determined based on the document type
  @State() pageTitle: string = '';

  //An array of all other available documents
  @State() otherCards: any[];

  //Table arrays
  @State() _rowNames = [];
  @State() _rowContent;
  @State() _rowLinks;
  @State() _rowTooltips;
  @State() _rowIndented = [false, true];
  @State() _rowTooltipContent;

  componentWillLoad() {
    setDocumentTitle('Document details');
    trackPageView();
    //extract passed data
    if (this.history && this.history.location.state) {
      this.docData = this.history.location.state.data;

      if (canRequestPinOnDrivingLicences() === undefined) {
        return ddService.getDocumentData();
      }
    }
    //If navigating through the URL, retrieve the users document data
    else {
      let tempData;

      ddService
        .getDocumentData()
        .then(data => {
          tempData = data;
        })
        .catch(err => {
          console.error(err);
        })
        .finally(() => {
          //If the user has no documents, redirect to home
          if (!tempData.driving_licences.length && !tempData.identity_cards.length) {
            this.returnToLogin();
          }
          //If the user has a document, add the data to the page
          else if (!!tempData.identity_cards.length) {
            this.docData = tempData.identity_cards[0];
          } else if (!!tempData.driving_licences.length) {
            this.docData = tempData.driving_licences[0];
          }
        });
    }
  }

  componentWillRender() {
    document.title = i18next.t('id.page-title');
    if (this.docData) {
      this.showOtherCards();

      //Parse the status again in case of a language toggle
      this.docData.parsed_status = getStatus(this.docData.status);
      this.fillData(this.docData);
    }
  }

  componentDidLoad() {
    this.addAriaDescribedby();
    sessionService.updateSession();
  }

  private showOtherCards() {
    //Filter out the other documents to show on the bottom of the page
    if (this.docData.documentType === 'id') {
      this.otherCards = getGlobalCards().filter(card => card.doc_num != this.docData.doc_num);
    } else {
      this.otherCards = getGlobalLicences().filter(licence => licence.doc_num != this.docData.doc_num);
    }
  }

  //Return to the login method page
  private returnToLogin() {
    if (this.history) {
      this.history.push('/home');
    }
  }

  //Fill data that could be re-rendered
  private fillData(data) {
    this._rowLinks = ['', getDocumentLinks(data)];
    this._rowContent = [
      data.doc_num,
      data.active ? (
        <dd-status active={data.active} icon={false} />
      ) : (
        <dd-status active={data.active} status-text={data.parsed_status} icon={false} />
      ),
    ];

    this._rowTooltips = [true, true];

    this._rowTooltipContent = [
      i18next.t(`id.doc-num-tooltip-${data.documentType}`),
      getStatusTooltip(data.status, data.documentType),
    ];

    this._rowNames = [i18next.t(`id.doc-num-${data.documentType}`), i18next.t('id.login-status')];

    this.pageTitle = i18next.t(`id.title-${data.documentType}`);
  }

private addAriaDescribedby() {
  // Get all the rows with the class `dd-login-options-id-details__table-grid`
  const rows = document.querySelectorAll('.dd-login-options-id-details__table-grid');
  let lastId;

  // Iterate through each row
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
      <Host class="dd-login-options-id-details">
        <div class="dd-login-options-id-details__button">
          <dd-button
            theme="secondary"
            arrow="before"
            text={i18next.t('button.back-to-login')}
            onClick={() => this.returnToLogin()}
          />
        </div>
        <section>
          <h1>{this.pageTitle}</h1>
          <table class="dd-login-options-id-details__table">
            {Object.keys(this._rowNames).map((_item, index) => (
              <tr
                id={!this._rowIndented[index] ? `details-${index}` : undefined}
                class={{ 'dd-login-options-id-details__table-grid': true, 'indented-row': this._rowIndented[index] }}>
                {loginMethodDetailsTable(
                  'id',
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
        {!!this.otherCards?.length && (
          <section>
            <h2>
              {i18next.t(
                `id.other-revoked-${this.docData.documentType}-${this.otherCards.length > 1 ? 'multiple' : 'single'}`,
              )}
            </h2>
            <ul class="dd-login-options-id-details__card-wrapper">
              {this.otherCards.map(card => (
                <li>
                  <dd-login-options-id document={card} allowMultipleDocumentNumbers={false} />
                </li>
              ))}
            </ul>
          </section>
        )}
      </Host>
    );
  }
}
