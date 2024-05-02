import { Component, Host, Prop, h } from '@stencil/core';

@Component({tag: 'logius-col', styleUrl: 'column.css' })
export class Column {
  @Prop() xs: number = 0;
  @Prop() sm: number = 0;
  @Prop() md: number = 0;
  @Prop() lg: number = 0;
  @Prop() xl: number = 0;
  @Prop() flex: boolean = false;

  renderClasses= (): string => {
    let classes = this.flex ? "flex " : "";

    ["xs", "sm", "md", "lg", "xl"].map(type => {
      if (this[type]) classes += `col-${type}-${this[type]} `;
    })
    return classes;
  }

  render() {
    return (
      <Host class={this.renderClasses()}>
        <slot></slot>
      </Host>
    );
  }

}
