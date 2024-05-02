import { Component, Host, Prop, h } from '@stencil/core';

@Component({tag: 'logius-button', styleUrl: 'button.css' })
export class Button {
  @Prop() withArrow: boolean = false;
  @Prop() theme: string;

  renderClasses= (): string => {
    let classes = "";

    if (this.withArrow) classes += " arrow"
    if (this.theme == "primary") classes += " primary"
    if (this.theme == "secondary") classes += " seconday"
    if (this.theme == "white") classes += " white"

    return classes;
  }

  render() {
    return (
      <Host role="button" tabindex="0" class={this.renderClasses()}>
        <slot></slot>
      </Host>
    );
  }

}
