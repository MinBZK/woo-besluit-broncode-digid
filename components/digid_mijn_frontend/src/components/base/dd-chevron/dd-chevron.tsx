import { Component, Host, Prop, h, State, getAssetPath } from '@stencil/core';

@Component({
  tag: 'dd-chevron',
})
export class DdChevron {
  /**
   * The direction the chevron is facing
   * @default right
   * @options right, left, up, down
   */
  @Prop() direction: string = 'right';

  /**
   * The orientation of the chevron, depending on the direction
   * @default vertical
   * @options vertical, horizontal
   */
  @State() orientation: string = 'vertical';

  //The folder to retrieve the icons from
  @State() iconFolder: string;

  private iconPath = getAssetPath('/assets/icons/icons.svg');

  componentWillLoad() {
    //determine orientation based on the direction
    if (this.direction === 'right' || this.direction === 'left') {
      this.orientation = 'vertical';
    } else {
      this.orientation = 'horizontal';
    }
  }

  render() {
    return (
      <Host class="dd-chevron">
        <svg class={`dd-chevron__${this.orientation}`} focusable="false" aria-hidden="true">
          <use href={`${this.iconPath}#chevron-${this.direction}`} />
        </svg>
      </Host>
    );
  }
}
