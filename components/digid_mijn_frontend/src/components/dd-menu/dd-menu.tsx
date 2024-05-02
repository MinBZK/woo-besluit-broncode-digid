import { Component, Host, h, Prop, Listen } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../utils/i18next';

@Component({
  tag: 'dd-menu',
})
export class DdMenu {
  @Prop() history: RouterHistory;
  @Prop({ mutable: true }) showUsageHistory: boolean;

  homeLink!: HTMLStencilRouteLinkElement;
  historyLink!: HTMLStencilRouteLinkElement;

  //Listen for a navigation change from somewhere in the application
  @Listen('navigationClicked', { target: 'body' })
  navigationChangedFromComponent(event: CustomEvent) {
    if (event.detail) {
      //change content accordingly
      this.changeContent(event.detail);
    }
  }

  componentDidLoad() {
    //Change selected content if navigating through the URL
    this.changeContent(window.location.pathname);
  }

  private changeContent(content: string) {
    const homeLink = this.homeLink.querySelector('a');
    const historyLink = this.historyLink.querySelector('a');
    const showUsageHistory = content.endsWith('gebruiksgeschiedenis');

    this.showUsageHistory = showUsageHistory;
    homeLink?.setAttribute('aria-current', `${!showUsageHistory}`);
    historyLink?.setAttribute('aria-current', `${showUsageHistory}`);
  }

  render() {
    return (
      <Host class="dd-menu">
        <nav>
          <stencil-route-link
            url="/home"
            urlMatch="/"
            activeClass={this.showUsageHistory ? '' : 'active-link'}
            onClick={() => this.changeContent('home')}
						aria-label={i18next.t('login.homepage') + ' ' + i18next.t('login.header')}
            ref={(el) => this.homeLink = el as HTMLStencilRouteLinkElement}
          >
            {i18next.t('login.header')}
          </stencil-route-link>
          <stencil-route-link
            url="/gebruiksgeschiedenis"
            activeClass={this.showUsageHistory ? 'active-link' : ''}
            onClick={() => this.changeContent('gebruiksgeschiedenis')}
            ref={(el) => this.historyLink = el as HTMLStencilRouteLinkElement}
          >
            {i18next.t('history.header')}
          </stencil-route-link>
        </nav>
      </Host>
    );
  }
}
