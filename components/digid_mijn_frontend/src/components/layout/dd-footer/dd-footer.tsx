import { Component, h, Host } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { hiddenNewWindow } from '../../../global/global-jsx';

@Component({
  tag: 'dd-footer',
})
export class DdFooter {
  render() {
    return (
      <Host class="dd-footer">
        <h2 class="dd-footer__title">{i18next.t('general.questions-title')}</h2>
        <p>
          <a
            href="https://www.digid.nl/"
            target="_blank"
            aria-label={i18next.t('general.website') + ', ' + i18next.t('general.new-window')}
          >
            {i18next.t('general.website')}
          </a>{' '}
					<span aria-hidden='true'>[{i18next.t('general.new-window')}]</span> {i18next.t('general.or')}
          <a
            href="https://www.digid.nl/contact/"
            target="_blank"
            aria-label={i18next.t('general.contact2') + ', ' + i18next.t('general.new-window')}
          >
            {i18next.t('general.contact2')}
          </a>{' '}
					{hiddenNewWindow()}
        </p>
      </Host>
    );
  }
}
