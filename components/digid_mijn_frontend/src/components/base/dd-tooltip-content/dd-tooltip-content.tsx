import { Component, Host, h } from '@stencil/core';

@Component({
  tag: 'dd-tooltip-content',
})
export class DdTooltipContent {
  render() {
    return (
      <Host class="dd-tooltip-content">
        <dd-icon name="information" />
        <div>
          <slot />
        </div>
      </Host>
    );
  }
}
