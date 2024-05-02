import { Component, Host, h, Element, Method, Prop, Event, EventEmitter } from '@stencil/core';
import BackdropManager from '../../utils/backdrop-manager';
import uniqueId from 'lodash.uniqueid';

function hasSomeParentTheClass(element, classname) {
  if ((element.className || '').indexOf(classname) >= 0) return true;
  return element.parentNode && hasSomeParentTheClass(element.parentNode, classname);
}

@Component({
  tag: 'dd-modal',
})
export class DdModal {
  @Element() private host: HTMLDdModalElement;
  private id: number;

  /**
   * The header text to display.
   */
  @Prop() header: string;

  /**
   * Dispatched the beforeHide before hiding the modal.
   */
  @Event() beforeHide: EventEmitter;

  /**
   * Dispatched the beforeClose before hiding the modal.
   */
  @Event() afterHide: EventEmitter;

  private readonly backdropManager: BackdropManager = BackdropManager.getInstance();

  /**
   * Shows the modal.
   */
  @Method()
  async showModal() {
    await this.backdropManager.showElement(this.host);
  }

  /**
   * Hides the modal.
   */
  @Method()
  async hideModal() {
    await this.backdropManager.hideElement(this.host);
  }

  constructor() {
    this.id = uniqueId();
  }

  componentDidLoad() {
    /**
     * Make beforeHide and afterHide available to dd-backdrop.
     */
    (this.host as any).beforeHide = this.beforeHide;
    (this.host as any).afterHide = this.afterHide;
  }

  componentDidRender() {
    const startTrap = this.host.querySelector('.dd-visually-hidden:first-of-type');
    const endTrap = this.host.querySelector('.dd-visually-hidden:last-of-type');

    startTrap.addEventListener('focus', () => {
      if (this.lastButton) {
        this.lastButton.focus();
      } else if (this.closeAnchor) {
        this.closeAnchor.focus();
      }
    });

    endTrap.addEventListener('focus', () => {
      if (this.closeAnchor) {
        this.closeAnchor.focus();
      } else if (this.lastButton) {
        this.lastButton.focus();
      }
    });

    this.closeAnchor?.addEventListener('keydown', e => {
      if (e.key === 'Enter') {
        this.hideModal();
      }
    });

    startTrap.addEventListener('focus', () => {
      if (this.lastAnchorTag) {
        this.lastAnchorTag.focus();
      }
    });

    endTrap.addEventListener('focus', () => {
      if (this.firstAnchorTag) {
        this.firstAnchorTag.focus();
      }
    });
  }

  connectedCallback() {
    const isInsideRcBackdrop = hasSomeParentTheClass(this.host, 'dd-backdrop');

    if (!isInsideRcBackdrop) {
      return;
    }

    setTimeout(() => this.focusOnInitialElement(), 100);
  }

  focusOnInitialElement() {
    if (this.firstButton) {
      this.firstButton.focus();
    } else if (this.closeAnchor) {
      this.closeAnchor.focus();
    }

    if (this.firstAnchorTag) {
      this.firstAnchorTag.focus();
    }
  }

  get firstButton(): HTMLButtonElement {
    return this.host.querySelector('[slot=actions] button:first-of-type');
  }

  get lastButton(): HTMLButtonElement {
    return this.host.querySelector('[slot=actions] button:last-of-type');
  }

  get closeAnchor(): HTMLAnchorElement {
    return this.host.querySelector('.dd-modal__close-a');
  }

  get firstAnchorTag(): HTMLAnchorElement {
    return this.host.querySelector('[slot=body] a:first-of-type');
  }

  get lastAnchorTag(): HTMLAnchorElement {
    return this.host.querySelector('[slot=body] a:last-of-type');
  }

  render() {
    const conditionalProps = {};

    if (this.header) {
      conditionalProps['aria-labelledby'] = 'dd-modal-header-' + this.id;
    }
    return (
      <Host {...conditionalProps} class="dd-modal" aria-modal="true" role="dialog">
        <div tabIndex={0} class="dd-visually-hidden" aria-hidden="true" />
        <header class="dd-modal__header" id={'dd-modal-header-' + this.id}>
          <dd-icon class="dd-modal__header__icon" name="warning" />
          <h2>{this.header}</h2>
        </header>
        <slot name="body" />
        <slot name="actions" />
        <div tabIndex={0} class="dd-visually-hidden" aria-hidden="true" />
      </Host>
    );
  }
}
