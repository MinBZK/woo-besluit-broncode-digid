import { Component, h, Host, State } from '@stencil/core';
import i18next from '../../../utils/i18next';

@Component({
  tag: 'dd-header',
})
export class DdHeader {
  @State() loggedOut = false;

  private clickLogout() {
    this.loggedOut = true;
    window.location.href = window.location.origin + '/uitloggen';
  }

  render() {
    return (
      <Host class="dd-header">
        <div class="dd-header__links">
          <dd-skip-to-buttons />
          <dd-lang-toggle class="dd-header__links__toggle" />
        </div>
        <a href="https://www.digid.nl" class="dd-header__rijksvaandel" aria-label={i18next.t('header.logo-alt')}>
          <img src="/assets/logo/rijksvaandel.svg" alt="" />
        </a>
        <div class="dd-header__logout">
          <dd-button
            class="dd-header__logout__button"
            onClick={() => this.clickLogout()}
            theme="primary"
            text={i18next.t('header.logout')}
          />
        </div>
        <div class="dd-header__menu">
          <dd-menu id="navigation" />
        </div>
      </Host>
    );
  }
}
