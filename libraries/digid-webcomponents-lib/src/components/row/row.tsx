import { Component, Host, h } from '@stencil/core';

@Component({tag: 'logius-row', styleUrl: 'row.css'})
export class Row {
  render() {
    return (
      <Host>
        <slot></slot>
       </Host>
     );
  }
}
