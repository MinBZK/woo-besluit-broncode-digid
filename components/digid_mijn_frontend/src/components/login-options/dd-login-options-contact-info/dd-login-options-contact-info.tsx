import { Component, Host, h, Prop, State, Fragment } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { getEmailLinks } from '../../../global/global-links';

@Component({
  tag: 'dd-login-options-contact-info',
})
export class DdLoginOptionsContactInfo {
  //The account data containing the contact info
  @Prop() accountData: any;

  //Array with all table row data, to be filled when data is loaded
  @State() _rowData: string[];

  //Array with all table row names
  @State() _rowNames;

  //Array with all table row tooltips
  @State() _rowTooltips;

  //Identifier of each row, used to create the ID
  private _rowIdentifiers = ['email', 'tel', 'pref_lang'];

  componentWillRender() {
    //Add contact data to array
    this._rowData = this.getRowData();
    this.fillData();
  }

  //Factory for the status "Not yet added" to prevent shared nodes in the same renderer
  private get notYetAdded() {
    return <div class="status-inactive">{i18next.t('contact.not-yet-added')}</div>;
  }

  private phoneLink() {
    return (
      <dd-link
        link={this.accountData.phone_number ? '/telefoonnummer/nieuw' : '/sms_uitbreiding'}
        text={i18next.t(`username.tel-link${this.accountData.phone_number ? '' : '-add'}`)}
      />
    );
  }

  private prefLangLink() {
    return <dd-link link="/voorkeurstaal" text={i18next.t('contact.pref-lang-link')} />;
  }

  //Return the correct email status
  private getEmail() {
    if (!this.accountData.email) {
      return this.notYetAdded;
    } else if (this.accountData.email.status === 'not_verified' || this.accountData.email.status === 'blocked') {
      return <div class="status-inactive">{this.accountData.email.adres + i18next.t('contact.not-yet-verified')}</div>;
    } else {
      return this.accountData.email.adres;
    }
  }

  //Return the correct tooltip text based on the email status
  private getEmailTooltip() {
    if (this.accountData.email && this.accountData.email.status === 'not_verified') {
      return (
        i18next.t('contact.email-not-verified-tooltip-a') +
        this.accountData.email.adres +
        i18next.t('contact.email-not-verified-tooltip-b')
      );
    } else if (this.accountData.email && this.accountData.username) {
      return i18next.t('contact.email-added-with-username-tooltip');
    } else if (this.accountData.email && !this.accountData.username) {
      return i18next.t('contact.email-added-without-username-tooltip');
    } else if (!this.accountData.email && this.accountData.username) {
      return i18next.t('contact.email-not-added-with-username-tooltip');
    } else if (!this.accountData.email && !this.accountData.username) {
      return i18next.t('contact.email-not-added-without-username-tooltip');
    }
  }

  private getRowData() {
    //Compute the preferred language
    const preferredLang = this.accountData.pref_lang === 'nl' ? i18next.t('language.NL') : i18next.t('language.EN');

    return ['', this.accountData.phone_number ? this.accountData.phone_number : 'not-yet-added', preferredLang];
  }

  getRowLinks() {
    return [
      getEmailLinks(this.accountData.email),
      this.accountData.sms === 'inactive' ? this.prefLangLink() : this.phoneLink(),
      this.prefLangLink(),
    ];
  }

  private fillData() {
    this._rowNames = [i18next.t('contact.email'), i18next.t('username.tel'), i18next.t('contact.pref-lang')];

    this._rowTooltips = [
      this.getEmailTooltip(),
      i18next.t('username.tel-tooltip'),
      i18next.t('contact.pref-lang-tooltip'),
    ];

    //Remove the telephone number row if SMS verification is inactive
    if (this.accountData.sms === 'inactive') {
      this._rowNames.splice(1, 1);
      this._rowData.splice(1, 1);
      this._rowTooltips.splice(1, 1);
      this._rowIdentifiers.splice(1, 1);
    }
  }

  render() {
    return (
      <Host class="dd-login-options-contact-info">
        {this._rowData && (
          <Fragment>
            <div class="dd-login-options-contact-info--mobile">
              <h2 class="dd-login-options-contact-info__title">{i18next.t('contact.title')}</h2>

              <table class="dd-login-options-contact-info__table" summary={i18next.t('contact.summary')}>
                {this._rowNames.map((_item, index) => (
                  <tr class="dd-login-options-contact-info__table-item">
                    <th scope="row" class="dd-login-options-contact-info__table-item-title">{this._rowNames[index]}</th>
                    <td class="dd-login-options-contact-info__table-item-content">
                      {/*inserting data directly to prevent reused nodes*/}
                      {index === 0 && this.getEmail()}
                      {index === 1 && this._rowData[index] === 'not-yet-added'
                        ? this.notYetAdded
                        : this._rowData[index]}
                    </td>
                    <td class="dd-login-options-contact-info__table-item-link">{this.getRowLinks()[index]}</td>
                    <td>
                      <dd-tooltip
                        id={`dd-login-option-mobile-${this._rowIdentifiers[index]}-button`}
                        class="dd-login-options-contact-info__table-item-tooltip"
                        contentId={`dd-login-option-mobile-${this._rowIdentifiers[index]}-content`}
                        ariaText={this._rowNames[index]}
                      />
                    </td>
                    <dd-tooltip-content
                      id={`dd-login-option-mobile-${this._rowIdentifiers[index]}-content`}
                      class="tooltip-content__mobile"
                    >
                      {this._rowTooltips[index]}
                    </dd-tooltip-content>
                  </tr>
                ))}
              </table>
            </div>
            <div class="dd-login-options-contact-info--desktop">
              <h2 class="h2-large">{i18next.t('contact.title')}</h2>
              <p>{i18next.t('contact.sub-title')}</p>

              <table class="dd-login-options-contact-info__table" summary={i18next.t('contact.summary')}>
                {this._rowNames.map((_user, index) => (
                  <tr class="dd-login-options-contact-info__table-grid">
                    <th scope="row" class="dd-login-options-contact-info__table-grid-name">{this._rowNames[index]}</th>
                    <td class="dd-login-options-contact-info__table-grid-info">
                      {/*inserting data directly to prevent reused nodes*/}
                      {index === 0 && this.getEmail()}
                      {index === 1 && this._rowData[index] === 'not-yet-added'
                        ? this.notYetAdded
                        : this._rowData[index]}
                    </td>
                    <td class="dd-login-options-contact-info__table-grid-link">{this.getRowLinks()[index]}</td>
                    <td class="dd-login-options-contact-info__table-grid-tooltip">
                      <dd-tooltip
                        id={`dd-login-option-desktop-${this._rowIdentifiers[index]}-button`}
                        class="dd-login-options-contact-info__table-grid-tooltip"
                        contentId={`dd-login-option-desktop-${this._rowIdentifiers[index]}-content`}
                        ariaText={this._rowNames[index]}
                      />
                    </td>
                    <dd-tooltip-content
                      id={`dd-login-option-desktop-${this._rowIdentifiers[index]}-content`}
                      class="tooltip-content__desktop"
                    >
                      {this._rowTooltips[index]}
                    </dd-tooltip-content>
                  </tr>
                ))}
              </table>
            </div>
          </Fragment>
        )}
      </Host>
    );
  }
}
