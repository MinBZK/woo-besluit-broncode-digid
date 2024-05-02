import { Component, Host, h, Element, Prop, State, Event, EventEmitter } from '@stencil/core';
import i18next from '../../utils/i18next';
import { trackInteractionEvent } from '../../global/global-methods';

@Component({
  tag: 'dd-banner',
})
export class DdBanner {
  @Element() el: HTMLElement;

  private appBtnAriaLabel = i18next.t(`banner.btn-app`) + ', ' + i18next.t('general.new-window');

  /**
   * The theme of the banner.
   * @options twoFactor, app and idCheck
   */
  @Prop() theme!: 'twoFactor' | 'app' | 'idCheck';

  /**
   * Whether the banner is hidden.
   * @default false
   */
  @State() hidden = false;

	//Event used to send aria update text confirming that the banner has been closed.
	@Event() bannerClosed: EventEmitter<string>;

  private closeBanner() {
		//Add text to the ariaUpdateText to inform text to speech users
		this.bannerClosed.emit(i18next.t('banner.banner-closed'));
    this.hidden = true;
    window.localStorage.setItem('hideBanner', 'true');
  }

  private clickBtn() {
    if (this.theme === 'twoFactor') {
      trackInteractionEvent('click-2factor-nudging-banner');
      window.location.href = window.location.origin + '/inloggen_voorkeur';
    } else {
      trackInteractionEvent('click-app-nudging-banner');
      window.open('SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS', '_newtabu');
    }
  }

  private renderTitle() {
    if (this.theme === 'idCheck') {
      return i18next.t('banner.title-idCheck');
    } else {
      return i18next.t('banner.title');
    }
  }

  private renderBtn() {
    if (this.theme === 'idCheck') {
      return '';
    } else {
      return (
        <dd-button
          class="dd-banner__btn"
          onClick={() => this.clickBtn()}
          arrow="after"
          text={i18next.t(`banner.btn-${this.theme === 'twoFactor' ? '2fa' : 'app'}`)}
          aria-text={this.theme === 'app' ? this.appBtnAriaLabel : ''}
        />
      );
    }
  }

  private renderText() {
    if (this.theme === 'idCheck') {
      return i18next.t('banner.text-idCheck');
    } else if (this.theme === 'twoFactor') {
      return i18next.t('banner.text-2fa');
    } else {
      return i18next.t('banner.text-app');
    }
  }

  render() {
    return (
      <Host class={{ 'dd-banner': true, 'dd-banner--hidden': this.hidden }} role="dialog">
        <dd-icon class="dd-banner__warning" name="safety" />
        <h2 class="dd-banner__title">{this.renderTitle()}</h2>
        {this.renderBtn()}
        <p class="dd-banner__text">{this.renderText()}</p>
        <button
          class="dd-banner__cross"
          onClick={() => this.closeBanner()}
          aria-label={i18next.t('alert.close-aria-label')}
        >
          <dd-icon name="cross" showMargin={false} />
        </button>
      </Host>
    );
  }
}
