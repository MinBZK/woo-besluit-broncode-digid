import { Component, Host, h } from '@stencil/core';

@Component({tag: 'logius-panel', styleUrl: 'panel.css'})
export class Panel {
  render() {
    return (
      <Host>
        <logius-inner-panel>
        <slot></slot>
        </logius-inner-panel>
       </Host>
     );
  }
}
