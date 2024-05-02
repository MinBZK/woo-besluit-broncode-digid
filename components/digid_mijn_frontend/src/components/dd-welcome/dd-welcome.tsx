import { Component, Host, h, Prop, State } from '@stencil/core';
import i18next from '../../utils/i18next';

@Component({
  tag: 'dd-welcome',
})
export class DdWelcome {
  // Add a media Query event Listener to check the viewport
  mediaQuery = matchMedia('(max-width: 432px)');
  @State() isMobile: boolean;
  @Prop() data: any;
  @State() bsn: string;

  componentWillRender() {
    // Check if data exists
    if (this.data) {
      this.bsn = this.data.bsn;
    }
  }

  componentDidLoad() {
    if (this.mediaQuery?.matches) {
      this.checkViewport(this.mediaQuery);
      this.mediaQuery.addEventListener('change', this.checkViewport)
    }
  }

  // Check the view of mobile and desktop
  checkViewport = (mediaQuery) => {
    this.isMobile = mediaQuery.matches;
  };

  render() {
    return (
      <Host class="dd-welcome">
        <img src="assets/logo/digid.svg" alt="DigiD logo" />
        <div class="dd-welcome__text">
          <header>
            <h1 class={this.isMobile ? 'dd-welcome__mobile' : undefined}>{i18next.t('general.welcome')}</h1>
          </header>
          {this.bsn && (
            <p>
              {i18next.t('general.your-bsn')} {this.bsn}
            </p>
          )}
        </div>
      </Host>
    );
  }
}
