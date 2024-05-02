import { Component, Host, Prop, h } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { getDeceased } from '../../../global/global-methods';

@Component({
  tag: 'dd-link',
})
export class DdLink {
  //Where the link should navigate to
  @Prop() link: string;

  //The shown text
  @Prop() text: string;

  //Used to track if the user is using a touch screen device
  @Prop({ mutable: true }) touchscreen = false;

  componentWillLoad() {
    //Increase link padding when using a touchscreen device
    if (window.matchMedia('(pointer: coarse)').matches) {
      this.touchscreen = true;
    }
  }

  //Add 'en' to the link if the language is set to English
  private static get languagePrefix() {
    return i18next.language == 'nl' ? '' : `/${i18next.language}`;
  }

  private get formattedUrl() {
    return `${DdLink.languagePrefix}${this.link}`;
  }

  render() {
    return (
      <Host class={{ 'dd-link': true, 'dd-link__touchscreen': this.touchscreen, 'dd-link--hidden': getDeceased() }}>
        <a href={this.formattedUrl} class="dd-link__link" tabindex="0">
          <dd-chevron class="dd-link__icon" />
          {this.text}
        </a>
      </Host>
    );
  }
}
