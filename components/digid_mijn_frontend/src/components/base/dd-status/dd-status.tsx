import { Component, h, Host, Prop, State } from '@stencil/core';
import i18next from 'i18next';

@Component({
  tag: 'dd-status',
})
export class DdStatus {
  //Whether the status is active
  @Prop() active: boolean;

  //Whether an Icon is shown, true by default
  @Prop() icon: boolean = true;

  //Whether the text states 'Completed' instead of 'Active', false by default
  @Prop() completed: boolean = false;

  //Turn inactive text into waiting for activation
  @Prop() activation: boolean = false;

  //Optional alternative status to be provided as input
  @Prop() statusText: string;

  //Shown text, can be altered
  @State() shownText: string;

  getIcon() {
    if (this.icon) {
      if (this.active) {
        //Render Active Icon
        return <dd-icon class="dd-status__icon" name="safety-on" />;
      } else {
        //Render Inactive Icon
        return <dd-icon class="dd-status__icon" name="safety-off" />;
      }
    } else {
      //Render no icon
      return '';
    }
  }

  componentWillRender() {
    //If an alternative text is available, assign that text
    if (this.statusText) {
      this.shownText = this.statusText;
    } else {
      const inactivationText = this.activation
        ? i18next.t('general.waiting-activation')
        : i18next.t('general.inactive');

      if (!this.completed) {
        this.shownText = this.active ? i18next.t('general.active') : inactivationText;
      } else {
        this.shownText = this.active ? i18next.t('general.completed') : i18next.t('general.incomplete');
      }
    }
  }

  render() {
    return (
      <Host class="dd-status">
        {this.getIcon()}
        {this.active ? <p class="status-active">{this.shownText}</p> : <p class="status-inactive">{this.shownText}</p>}
      </Host>
    );
  }
}
