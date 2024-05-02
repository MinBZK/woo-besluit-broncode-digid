import { Component, h, Host, Element, Listen, forceUpdate } from '@stencil/core';
import ddService from '../../api/services/dd.service';
import sessionService from '../../api/services/dd.session.service';
import {
  createToastMessage,
  detectBrowser,
  globalErrorHandler,
  matomoObject,
  trackPageView,
} from '../../global/global-methods';
import { HTMLStencilElement } from '@stencil/core/internal';

@Component({
  tag: 'app-root',
})
export class AppRoot {
  @Element() el: HTMLStencilElement;

  connectedCallback() {
    return sessionService.getSession();
  }

  componentShouldUpdate() {
    return sessionService.getSession().catch(() => {
      globalErrorHandler();
    });
  }

  componentDidRender() {
    //Grab the Matomo Site ID from the Config and start tracking activity
    ddService
      .getConfig()
      .then(data => {
        matomoObject().push(['setSiteId', data.analytics.site_id]);
        trackPageView();
      })
      .catch(() => {
        globalErrorHandler();
      });

    ddService
      .getNotifications()
      .then(notifications => {
        Object.keys(notifications).forEach(key => {
          return createToastMessage({ type: key, message: notifications[key] });
        });
      })
      .catch(() => {
        globalErrorHandler();
      });

		ddService.getNewsItems()
			.then(data => {
				data.news_items.forEach(message => {
					return createToastMessage({ type: 'information', message: `${message.name} \n${message.body}`});
				});
			})
			.catch(() => {
				globalErrorHandler();
			});
    return detectBrowser();
  }

  componentDidLoad() {
    const spinner = document.querySelector('#app-loader');
    //Remove the spinner
    if (spinner) {
			(spinner as any ).ariaBusy = false;
    	spinner.remove();
    }

		return sessionService.updateSession();
  }

  //Update all components when the language gets toggled
  @Listen('languageToggled')
  reRenderComponents() {
    forceUpdate(this.el);
    Array.from(document.querySelectorAll('*'))
      .filter(c => c.nodeName.indexOf('DD-') >= 0)
      .forEach(el => forceUpdate(el));
  }

  render() {
    return (
      <Host class="app-root">
        <header class="main-grid app-root__header">
          <aside />
          <dd-header />
          <aside />
        </header>

        <main class="main-grid app-root__main-content">
          <aside />
          <div id="main">
            <dd-toast id="mainToast" />
            <stencil-router class="column">
              <stencil-route-switch scrollTopOffset={1}>
                <stencil-route url="/home" component="dd-login-options" exact={true} />
                <stencil-route url="/gebruiksgeschiedenis" component="dd-usage-history" />
                <stencil-route url="/digid-app-details" component="dd-login-options-app-details" />
                <stencil-route url="/document-details" component="dd-login-options-id-details" />
                <stencil-route url="/gebruiker-details" component="dd-login-options-username-details" />
                <stencil-route component="dd-login-options" />
              </stencil-route-switch>
            </stencil-router>
            <div class="app-root__main-content__bottom-logo">
              <img src="assets/logo/rijksvaandel-bottom.svg" alt="" />
            </div>
          </div>
          <aside />
        </main>

        <dd-session-handler />

        <footer class="main-grid app-root__footer">
          <aside />
          <dd-footer />
          <aside />
        </footer>
      </Host>
    );
  }
}
