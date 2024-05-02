import { Component, Host, h, Method, State } from '@stencil/core';
import i18next from '../../utils/i18next';

const SCROLLBLOCK_CLASS_NAME = 'dd-backdrop-body-scrollblock';

@Component({
  tag: 'dd-backdrop',
})
export class DdBackdrop {
  private childrenEl: HTMLElement;

  @State() private hasChildren: boolean;

  /**
   * Moves the element from its current parent to the .dd-backdrop__children
   * element found inside the dd-backrop in the body.
   * @param element
   */
  @Method()
  public async showElement(element: Element) {
    if (!(element as any)._initialParent) {
      (element as any)._initialParent = element.parentElement;
    }

    if (this.children.indexOf(element) < 0) {
      this.childrenEl.appendChild(element);
      this.toggleScrollblock();
    }
  }

  /**
   * Moves the element back to its original parent from the .dd-backdrop__children
   * element found inside the dd-backrop in the body.
   * @param element
   */
  @Method()
  public async hideElement(element: any) {
    if (element._initialParent) {
      element.beforeHide && element.beforeHide.emit();
      element._initialParent.appendChild(element);
      element.afterHide && element.afterHide.emit();
      this.toggleScrollblock();
    }
  }

  private toggleScrollblock() {
    const docEl = document.documentElement;
    this.hasChildren = this.children.length > 0;

    if (this.hasChildren) {
      const top = (window.pageYOffset || docEl.scrollTop) - (docEl.clientTop || 0);

      docEl.style.top = `-${top}px`;
      docEl.classList.add(SCROLLBLOCK_CLASS_NAME);
    } else {
      const top = +docEl.style.top.replace('-', '').replace('px', '');

      docEl.style.top = '0px';
      docEl.classList.remove(SCROLLBLOCK_CLASS_NAME);
      docEl.scrollTop = +top;
    }
  }

  private get children() {
    return Array.from(this.childrenEl.children);
  }

  constructor() {
    i18next.changeLanguage();
  }

  render() {
    return (
      <Host class="dd-backdrop">
        <div class="dd-backdrop__shadow" />
        <div ref={el => (this.childrenEl = el as HTMLElement)} class="dd-backdrop__children" />
      </Host>
    );
  }
}
