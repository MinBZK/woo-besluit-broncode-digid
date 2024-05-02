import { Component, h, Prop, Element } from '@stencil/core';
import i18next from '../../../utils/i18next';
import { trackInteractionEvent } from '../../../global/global-methods';

@Component({
  tag: 'dd-tooltip',
})
export class DdTooltip {
  //Define this element to perform computations on it
  @Element() el: HTMLElement;

  //Whether the tooltip is opened. False by default. Can be altered from outside the component
  @Prop({ mutable: true }) tooltipOpened: boolean = false;

  //The ID of the related content, used for ID binding
  @Prop() contentId: string;

  //Display additional Aria information about this tooltip
  @Prop() ariaText!: string;

  //The related contentElement, to be identified using the contentId
  private contentElement: HTMLDdTooltipContentElement;

  componentDidRender() {
    this.contentElement = document.getElementById(this.contentId) as HTMLDdTooltipContentElement;
    this.contentElement.setAttribute('aria-labelledby', this.el.id);
  }

  //Toggle the tooltip between opened and closed
  private toggleTooltip() {
    this.tooltipOpened = !this.tooltipOpened;
    this.contentElement.style.display = this.tooltipOpened ? 'flex' : 'none';

    if (this.tooltipOpened) {
      trackInteractionEvent('opent-tooltip', this.ariaText);
    }
  }

  render() {
    return (
      <button
        class="dd-tooltip"
        onClick={() => this.toggleTooltip()}
        aria-label={this.ariaText ? i18next.t('general.tooltip-aria') + ' ' + this.ariaText : ''}
        aria-expanded={this.tooltipOpened}
        aria-controls={this.contentId}
      >
        {this.tooltipOpened ? (
          <dd-icon name="tooltip-clicked" showMargin={false} />
        ) : (
          <dd-icon name="tooltip" showMargin={false} />
        )}
      </button>
    );
  }
}
