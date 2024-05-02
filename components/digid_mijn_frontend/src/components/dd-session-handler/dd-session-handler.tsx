import { Component, h, Host, Method, State } from '@stencil/core';
import i18next from 'i18next';
import sessionService from '../../api/services/dd.session.service';
import { Components } from '../../components';
import DdModal = Components.DdModal;
import { usingDevMode } from '../../global/global-methods';

@Component({
  tag: 'dd-session-handler',
})
export class DdSessionHandler {
  //Interval for checking the session. A check takes place every 10 seconds.
  private checkSessionInterval = setInterval(() => this.getSessionAndRenderModals(), 10000);

  //The amount of minutes until the session expires, defaults to 15
  @State() expiryDurationInMinutes: number = 15;

  //Kill the interval and show the timeout modal
  @Method()
  async timeoutSession() {
    clearInterval(this.checkSessionInterval);
    return Promise.all([this.timeoutModal.showModal(), this.warningModal.hideModal()]);
  }

  private timeoutModal: DdModal;
  private warningModal: DdModal;

  componentWillRender() {
    //kill the interval if dev mode is activated
    if (usingDevMode()) {
      clearInterval(this.checkSessionInterval);
    }
  }

  public getSessionAndRenderModals() {
    sessionService
      .getSession()
      // @ts-ignore
      .then(response => {
        const expiryTime = response.timestamp;
        const current = new Date().getTime();
        const minutesLeftBeforeExpiry = (expiryTime - current) / 60000;

        const shouldShowWarning = minutesLeftBeforeExpiry > 0 && minutesLeftBeforeExpiry < 1;
        const shouldShowTimeout = minutesLeftBeforeExpiry < 0;

        if (response.minutes) {
          this.expiryDurationInMinutes = response.minutes;
        }

        if (shouldShowWarning) {
          return this.warningModal.showModal();
        } else if (shouldShowTimeout) {
          return this.timeoutSession();
        }
      })
      .catch(err => {
        clearInterval(this.checkSessionInterval);
        console.log(err);
      });
  }

  updateSessionAndCloseModal() {
    return [this.warningModal.hideModal(), sessionService.updateSession()];
  }

  render() {
    return (
      <Host>
        <dd-modal ref={el => (this.timeoutModal = el)} header={i18next.t('modal.timeout-header')}>
          <div slot="body">
            <p>
              {i18next.t('modal.timeout-p1', {
                minutes: this.expiryDurationInMinutes ? this.expiryDurationInMinutes : 15,
              })}
            </p>
            <p>
              {i18next.t('modal.timeout-p2-a')}
              <a href={`${window.location.origin}/inloggen`}>{i18next.t('modal.my-digid')}</a>
              {i18next.t('modal.timeout-p2-b')}(<a href="https://www.digid.nl">www.digid.nl</a>)
            </p>
            <p>
              {i18next.t('modal.timeout-p3')}
              <ul>
                <li>{i18next.t('modal.timeout-p3-li1')}</li>
                <li>{i18next.t('modal.timeout-p3-li2')}</li>
                <li>{i18next.t('modal.timeout-p3-li3')}</li>
              </ul>
            </p>
          </div>
        </dd-modal>

        <dd-modal ref={el => (this.warningModal = el)} header={i18next.t('modal.warning-header')}>
          <div slot="body">
            <p>{i18next.t('modal.warning-p')}</p>
          </div>
          <div slot="actions">
            <dd-button
              class="dd-modal-btn"
              touchscreen={true}
              onClick={() => {
                return this.updateSessionAndCloseModal();
              }}
              text={i18next.t('modal.extend')}
            />
          </div>
        </dd-modal>
      </Host>
    );
  }
}
