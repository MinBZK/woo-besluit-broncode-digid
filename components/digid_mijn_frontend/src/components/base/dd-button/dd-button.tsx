import { Component, Host, Prop, h, Watch, State } from '@stencil/core';

@Component({
  tag: 'dd-button',
})
export class DdButton {
  /**
   * The button text to display.
   */
  @Prop({ mutable: true }) text: string;

  /**
   * The theme of the button.
   * @default primary
   * @options primary, secondary and tertiary
   */
  @Prop() theme: string = 'primary';
  //  @Prop() theme!: 'primary' | 'secondary' | 'tertiary';

  /**
   * Toggles whether or not to display the a chevron.
   * @options before, after
   */
  @Prop() arrow: string;

  //Property used to disable a button's size increasing on touchscreens
  @Prop() sizeChange: boolean = true;

  //Property used to disable a button's hover effect
  @Prop() noHover: boolean = false;

  //Property used to prevent the button from being focussed
  @Prop() noFocus: boolean = false;

  //Text to be used as Aria label
  @Prop() ariaText: string;

  //Used to override the aria role, defaults to button
  @Prop() roleOverride: string;

  //Used to track if the user is using a touch screen device
  @Prop({ mutable: true }) touchscreen: boolean = false;

  /**
   * The color of the chevron
   * Set to null to get the default chevron color if the theme is secondary or tertiary
   * @default white
   * @options white, null
   */
  @State() chevronColor: string = 'white';

  //Throw an error if there is no text or arrow present
  @Watch('text')
  checkForContent(newValue: any) {
    if (!newValue && !this.arrow) {
      throw new Error('Button needs a text attribute!');
    }
    this.text = newValue;
  }

  componentWillLoad() {
    //Increase button padding when using a touchscreen device
    if (window.matchMedia('(pointer: coarse)').matches && this.sizeChange) {
      this.touchscreen = true;
    }

    if (this.theme === 'secondary' || this.theme === 'tertiary') {
      //color set to null so the arrow defaults to black in the chevron component
      this.chevronColor = null;
    }
  }

  //check if data is present
  componentDidLoad() {
    this.checkForContent(this.text);
  }

  private get tabindex() {
    return this.noFocus ? '-1' : '0';
  }

  render() {
    return (
      <Host
        class={{
          [`dd-button--${this.theme}`]: true,
          'dd-button--no-hover': this.noHover,
          'dd-button__touchscreen': this.touchscreen,
        }}
      >
        <button class="dd-button" tabindex={this.tabindex} aria-label={this.ariaText} role={this.roleOverride}>
          {this.arrow && this.arrow === 'before' && <dd-chevron class="dd-button__chevron-before" direction="left" />}
          <span class="dd-button__text">{this.text}</span>
          {this.arrow && this.arrow === 'after' && <dd-chevron class="dd-button__chevron-after" direction="right" />}
        </button>
      </Host>
    );
  }
}
