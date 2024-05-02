import { Component, Host, h, Prop } from '@stencil/core';

@Component({
  tag: 'dd-highlight',
})
export class DdHighlight {
  /**
   * The text to display.
   */
  @Prop() text: string;

  /**
   * The text to highlight.
   */
  @Prop() highlightText: string;

  /**
   * Highlight using a blue background instead of bold.
   */
  @Prop() blue = false;

  private getHighlightedHTML() {
    if (typeof this.highlightText === 'undefined' || this.highlightText.trim() === '') {
      return this.text;
    }

    const check = new RegExp(this.highlightText, 'ig');

    return this.text
      .toString()
      .replace(
        check,
        matchedText => `<span class=dd-highlight--${this.blue ? 'blue' : 'bold'}>` + matchedText + '</span>',
      );
  }

  render() {
    return <Host class="dd-highlight" innerHTML={this.getHighlightedHTML()} />;
  }
}
