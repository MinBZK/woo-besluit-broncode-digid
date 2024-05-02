import { Component, Host, h, Prop, Event, EventEmitter, Element, State } from '@stencil/core';
import i18next from '../../utils/i18next';

const PAGINATOR_SIZE = 7;
const PAGINATOR_ELLIPSES_CLAMP = 5;

enum EllipsesType {
  NONE = 'none',
  END = 'end',
  START = 'start',
  BOTH = 'both',
}

@Component({
  tag: 'dd-paginator',
})
export class DdPaginator {
  @Element() el!: HTMLDdPaginatorElement;

  /**
   * The total amount of pages.
   */
  @Prop() totalPages: number;

  /**
   * The current page number.
   */
  @Prop({ mutable: true }) currentPage: number;

  /**
   * Emits an event when the currentPage changes.
   */
  @Event({ bubbles: true }) pageChange: EventEmitter<number>;

  @State() tick = {};

  constructor() {
    i18next.changeLanguage();
  }

  /**
   * Example input and output:
   * generateNumbersArray(3, 7) => [3, 4, 5, 6, 7]
   * generateNumbersArray(1, 3) => [1, 2, 3]
   */
  private generateNumbersArray(from, to) {
    return [...Array(to - from + 1).keys()].map(i => i + from);
  }

  private get endClampIndex() {
    return this.totalPages - PAGINATOR_ELLIPSES_CLAMP;
  }

  get ellipsesType() {
    if (this.totalPages <= PAGINATOR_SIZE) {
      return EllipsesType.NONE;
    } else {
      if (this.currentPage <= PAGINATOR_ELLIPSES_CLAMP) {
        return EllipsesType.END;
      } else if (this.currentPage > this.endClampIndex) {
        return EllipsesType.START;
      } else {
        return EllipsesType.BOTH;
      }
    }
  }

  //Add media listeners to change the numbersArray width based on screen width
  windowBreakpoint1 = window.matchMedia('(max-width: 800px)');
  windowBreakpoint2 = window.matchMedia('(max-width: 650px)');
  @State() paginatorCenterSize: number = 5;

  //Change the size of the paginator based on the screen size
  checkPageWidth() {
    if (this.windowBreakpoint2.matches) {
      this.paginatorCenterSize = 1;
    } else if (this.windowBreakpoint1.matches) {
      this.paginatorCenterSize = 3;
    } else {
      this.paginatorCenterSize = 5;
    }
  }

  get paginationItems() {
    if (this.ellipsesType === EllipsesType.END) {
      return this.generateNumbersArray(1, PAGINATOR_ELLIPSES_CLAMP + 1);
    } else if (this.ellipsesType === EllipsesType.BOTH) {
      return this.generateNumbersArray(
        this.currentPage - Math.floor(this.paginatorCenterSize / 2),
        this.currentPage + Math.floor(this.paginatorCenterSize / 2),
      );
    } else if (this.ellipsesType === EllipsesType.START) {
      return this.generateNumbersArray(this.endClampIndex, this.totalPages);
    } else {
      return this.generateNumbersArray(1, this.totalPages);
    }
  }

  private gotoPage(index) {
    this.currentPage = index;
    this.pageChange.emit(index);
  }

  private createButton(index) {
    return (
      <li>
        <button
          class={{
            'dd-btn dd-paginator__item': true,
            'dd-btn--subtle': this.currentPage !== index,
            'dd-btn--primary dd-paginator__item--active': this.currentPage === index,
          }}
          onClick={() => this.gotoPage(index)}
          aria-label={i18next.t('paginator.goto', { index })}
          tabindex="0"
          aria-current={(this.currentPage === index) + ''}
        >
          {index}
        </button>
      </li>
    );
  }

  private createEllipses() {
    return (
      <li>
        <div class="dd-paginator__ellipses">...</div>
      </li>
    );
  }

  componentDidLoad() {
    //Add an eventlistener for the page width change
    setTimeout(() => {
      [this.windowBreakpoint1, this.windowBreakpoint2].forEach(el => {
        el.addEventListener('change', () => {
          this.checkPageWidth();
        });
      });
    }, 1000);
  }

  render() {
    return (
      <Host class={{ 'dd-paginator': true }}>
        <nav class="dd-paginator__nav" role="navigation" aria-label={i18next.t('paginator.nav')}>
          <ul class="dd-paginator__list">
            <li>
              <button
                class="dd-btn dd-btn--prev-next"
                disabled={this.currentPage === 1}
                aria-label={i18next.t('paginator.prev-aria-label')}
                onClick={() => this.gotoPage(this.currentPage - 1)}
              >
                <dd-chevron class="dd-btn__chevron" direction="left" />
                <span>{i18next.t('paginator.prev')}</span>
              </button>
            </li>
            {(this.ellipsesType === EllipsesType.START || this.ellipsesType === EllipsesType.BOTH) && [
              this.createButton(1),
              this.createEllipses(),
            ]}
            {this.paginationItems.map(index => this.createButton(index))}
            {(this.ellipsesType === EllipsesType.END || this.ellipsesType === EllipsesType.BOTH) && [
              this.createEllipses(),
              this.createButton(this.totalPages),
            ]}
            <li>
              <button
                class="dd-btn dd-btn--prev-next"
                disabled={this.currentPage === this.totalPages}
                aria-label={i18next.t('paginator.next-aria-label')}
                onClick={() => this.gotoPage(this.currentPage + 1)}
              >
                <span>{i18next.t('paginator.next')}</span>
                <dd-chevron class="dd-btn__chevron" direction="right" />
              </button>
            </li>
          </ul>
        </nav>
      </Host>
    );
  }
}
