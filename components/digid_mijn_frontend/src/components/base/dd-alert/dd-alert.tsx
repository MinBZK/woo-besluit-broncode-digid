import { Component, Host, h, Prop, Event, EventEmitter, Element, State } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { usingDevMode } from '../../../global/global-methods';

@Component({
  tag: 'dd-alert',
})
export class DdAlert {
  @Element() el: HTMLElement;

  /**
   * The body text to display.
   */
  @Prop() body: string;

  /**
   * Toggles whether or not to display the close icon.
   * @default false
   */
  @Prop() allowClose: boolean = false;

  /**
   * The type of the alert, toggling the correct styling and icon.
   * @default 'info'
   */
  @Prop({ mutable: true }) type: string = 'info';

  /**
   * Array used for buttons in the array.
   */
  @State() alertButtons: any;

  /**
   * Dispatched the dismiss event when the X is clicked.
   */
  @Event() dismiss: EventEmitter;

  componentWillLoad() {
    //change 'message' into an error alert
    if (this.type === 'message') {
      this.type = 'error';
    }
    i18next.changeLanguage();

    if (usingDevMode() && this.type === 'warning') {
      this.alertButtons = [
        {
          method: () => this.switchUserId('1'),
          label: 'User 1',
        },
        {
          method: () => this.switchUserId('2'),
          label: 'User 2',
        },
        {
          method: () => this.switchUserId('3'),
          label: 'User 3',
        },
      ];
    }
  }

  private switchUserId(id): void {
    window.localStorage.setItem('devUserId', id);
    location.reload();
  }

  private handleCloseClicked = () => {
    this.dismiss.emit();
    this.el.classList.add('dd-alert--hidden');
  };

  render() {
    return (
      <Host
        class={{
          'dd-alert': true,
          [`dd-alert--${this.type}`]: true,
        }}
      >
        <dd-icon class="dd-alert__type-icon" name={this.type} />
        <p class="dd-alert__body">
          {!this.body && <slot name="body" />}
          {this.body}
        </p>
        {this.allowClose && (
          <button
            class="dd-alert__close-btn"
            onClick={this.handleCloseClicked}
            aria-label={i18next.t('alert.close-aria-label')}
          >
            <dd-icon name="cross" showMargin={false} />
          </button>
        )}
        {this.alertButtons && (
          <div>
            {this.alertButtons.map(button => {
              return <button onClick={button.method}>{button.label}</button>;
            })}{' '}
          </div>
        )}
      </Host>
    );
  }
}
