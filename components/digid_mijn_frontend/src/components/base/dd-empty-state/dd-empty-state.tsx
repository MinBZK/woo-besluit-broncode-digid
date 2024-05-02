import { Component, Host, h, Prop, Fragment } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { hiddenNewWindow } from '../../../global/global-jsx';

@Component({
  tag: 'dd-empty-state',
})
export class DdEmptyState {
  /**
   * The type of empty state shown.
   * @options App, ID, Username
   */
  @Prop() type!: 'app' | 'id' | 'error';

  /**
   * Whether the empty state is for a card
   * Setting this value to false will show no body text
   * @default true
   */
  @Prop() card = true;

	/**
	 * Whether the empty state is displaying an error
	 * @default false
	 */
	@Prop() error = false

  /**
   * the document data property, used to alter the document empty state text
   */
  @Prop() documentData: any;

  private getDocBody() {
    /**
     * Determine the body text shown for a document empty state based on the active switches
     * Properties show_identity_cards and show_driving_licences reflect these switches
     */
    let text;
    const showID = this.documentData.show_identity_cards;
    const showLicence = this.documentData.show_driving_licences;

    if (showID && showLicence) {
      text = i18next.t(`empty-state.id-body-2-id-licence`);
    } else if (!showID && showLicence) {
      text = i18next.t(`empty-state.id-body-2-licence`);
    } else if (showID && !showLicence) {
      text = i18next.t(`empty-state.id-body-2-id`);
    } else {
      return '';
    }

    return (
      <p>
        {i18next.t(`empty-state.id-body-1`)}
        {text}
        {i18next.t(`empty-state.id-body-3`)}
      </p>
    );
  }

  //Return a link based on the used device
  private static getAppLink() {
    //Used to determine the mobile operating system.
    let userAgent = navigator.userAgent || navigator.vendor;

    //If the user is using a mobile device
    if (/android/i.test(userAgent) || (/iPad|iPhone|iPod/.test(userAgent) && !(window as any).MSStream)) {
      return (
        <p>
          <a
            href="SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
            target="_blank"
            aria-label={i18next.t('empty-state.app-footer-mobile') + ', ' + i18next.t('general.new-window')}
          >
            {i18next.t(`empty-state.app-footer-mobile`)}
          </a>{' '}
					{hiddenNewWindow()}
        </p>
      );
    }

    //In neither Androis nor IOS is used, return the default text
    return (
      <p>
        <a
          href="SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
          target="_blank"
          aria-label={i18next.t('empty-state.app-footer') + ', ' + i18next.t('general.new-window')}
        >
          {i18next.t(`empty-state.app-footer`)}
        </a>{' '}
				{hiddenNewWindow()}
      </p>
    );
  }

  render() {
    return (
      <Host class={{ 'dd-empty-state': true, 'dd-empty-state__app': this.type === 'app', 'dd-empty-state--error' : this.error }}>
        <dd-icon class="dd-empty-state__icon" name={this.type === 'app' ? 'phone' : 'id-card'} />
        <div class="dd-empty-state__body">
          <h4
            class={{
              'dd-empty-state__body__header': true,
              'dd-empty-state__body__header--error': this.error,
            }}
          >
            {this.error ? <slot /> : i18next.t(`empty-state.${this.type}-header`)}
          </h4>

          {this.type === 'app' && !this.error && (
            <Fragment>
							{this.card && <p>{i18next.t(`empty-state.${this.type}-body`)}</p>}
              {DdEmptyState.getAppLink()}
            </Fragment>
          )}

          {this.type === 'id' && !this.error && (
            <p>
              {this.documentData && this.card && this.getDocBody()}
              <a
                href="SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS"
                target="_blank"
                aria-label={i18next.t(`empty-state.${this.type}-footer`) + ', ' + i18next.t('general.new-window')}
              >
                {i18next.t(`empty-state.${this.type}-footer`)}
              </a>{' '}
							{hiddenNewWindow()}
            </p>
          )}
        </div>
      </Host>
    );
  }
}
