import { Component, Host, h, Prop, State, getAssetPath } from '@stencil/core';

@Component({
  tag: 'dd-icon',
  assetsDirs: ['assets'],
})
export class DdIcon {
  //Name of the icon from the assets/icon file
  @Prop() name: string;

  //Used to show standard margin, true by default
  @Prop() showMargin = true;

  //Alternative size for the icon, regular by default
  @State() size: string;

  //The folder to retrieve the icons from
  @State() darkmode: boolean;

  private iconPath = getAssetPath('/assets/icons/icons.svg');

  checkMode() {
    this.darkmode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  checkSize() {
    switch (this.name) {
      case 'cross':
      case 'search':
        this.size = 'small';
        break;
      case 'id-card':
      case 'phone':
      case 'username':
        this.size = 'large';
        break;
      default:
        this.size = 'regular';
    }
  }

  componentWillRender() {
    this.checkMode();
    this.checkSize();
  }

  componentDidRender() {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      this.checkMode();
    });
  }

  render() {
    return (
      <Host
        class={{
          'dd-icon': true,
          [`dd-icon--${this.size}`]: true,
          'dd-icon--right-margin': this.showMargin,
        }}
      >
        <svg class="dd-icon__svg" focusable="false" aria-hidden="true">
          <use href={`${this.iconPath}#${this.darkmode ? 'darkmode-' : ''}${this.name}`} />
        </svg>
      </Host>
    );
  }
}
