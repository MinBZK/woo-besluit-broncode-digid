import { Component, h, Host } from '@stencil/core';
import i18next from 'i18next';

@Component({
  tag: 'dd-skip-to-buttons',
})
export class DdSkipToButtons {
  render() {
    return (
      <Host class="dd-skip-to-buttons">
        <a class="dd-skip-to-buttons__link" href="#main" tabindex="0">
          {i18next.t('skip-buttons.content')}
        </a>
        <a class="dd-skip-to-buttons__link" href="#navigation" tabindex="0">
          {i18next.t('skip-buttons.navigation')}
        </a>
      </Host>
    );
  }
}
