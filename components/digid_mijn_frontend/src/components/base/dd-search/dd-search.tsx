import { Component, Event, EventEmitter, h, Element, Host, Prop, State, Watch, Listen } from '@stencil/core';
import i18next from '../../../utils/i18next';
import usageHistorySuggestions from '../../../assets/data/usage-history-search-suggestions.json';

@Component({
  tag: 'dd-search',
})
export class DdSearch {
  private inputEl: HTMLInputElement;
  private searchSuggestions = JSON.stringify(usageHistorySuggestions);

  @Element() private host; //: HTMLRcSearchElement;

  @Prop() isExecuted = false;

  @State() opened = false;

  /**
   * The label to show above the input field.
   */
  @Prop() label: string;

  /**
   * The placeholder to display when no input is showing.
   */
  @Prop() placeholder: string;

  /**
   * The value of the input.
   */
  @Prop({ reflect: true, mutable: true }) value;

  /**
   * Toggles the loading block in the dropdown.
   */
  @Prop() isLoading = false;

  /**
   * Toggles the loading block in the dropdown.
   */
  @Prop() hideDropdown = false;

  /**
   * The results of the search.
   */
  @Prop() results: any;
  @State() innerResults = [];
  @Watch('results')
  private parseResults(newValue: string) {
    if (newValue) this.innerResults = JSON.parse(newValue);
  }

  /**
   * Logic for showing the relevant dropdown suggestions
   */
  @Watch('value')
  private filterAndShowDropdown(newValue: string) {
    if (newValue.length < 2) {
      this.innerResults = [];
      return;
    }

    //Truncate long values to prevent overlapping the buttons
    if (newValue.length > 40) {
      this.value = this.truncateString(newValue, 40);
    }

    //Filter the suggestions based on the
    this.innerResults = JSON.parse(this.searchSuggestions)
      .filter(item => item.title.toLowerCase().replace(/ /g, '').includes(newValue.toLowerCase().replace(/ /g, '')))
      .slice(0, 5)
      //Sort the results alphabetically
      .sort((a, b) => {
        const nameA = a.title.toUpperCase();
        const nameB = b.title.toUpperCase();
        if (nameA < nameB) {
          return -1;
        }
        if (nameA > nameB) {
          return 1;
        }
      });
  }

  /**
   * Dispatched the search event when the search icon is clicked or the enter key is pressed.
   */
  @Event({ bubbles: true }) search: EventEmitter;

  /**
   * Dispatched the select event when a search option is clicked.
   */
  @Event() resultSelected: EventEmitter;

  /**
   * Dispatched the clear event when the X is clicked.
   */
  @Event() clear: EventEmitter;

  @Listen('keydown')
  searchKeyDownHandler(event) {
    const navigableElements = Array.from(this.host.querySelectorAll('[can-navigate]'));
    const activeElement = document.activeElement;
    const canNavigateElementIsActive = activeElement.hasAttribute('can-navigate');
    const indexOfActive = navigableElements.findIndex(element => element == activeElement);

    /**
     * A user can only navigate from one navigable element to another.
     */
    if (this.innerResults.length <= 0 || !canNavigateElementIsActive) {
      return;
    }

    if (event.key === 'ArrowUp' && indexOfActive > 0) {
      event.preventDefault();
      (navigableElements[indexOfActive - 1] as HTMLElement).focus();
    } else if (event.key === 'ArrowDown' && indexOfActive < navigableElements.length - 1) {
      event.preventDefault();
      (navigableElements[indexOfActive + 1] as HTMLElement).focus();
    }
  }

  private documentClickHandler = event => this.handleDocumentClick(event);
  private documentKeyDownHandler = event => this.handleDocumentKeyDown(event);

  componentWillLoad() {
    this.parseResults(this.results);
  }

  componentDidLoad() {
    this.inputEl = this.host.querySelector('input');
    ['click', 'focus'].forEach(evt => {
      this.inputEl.addEventListener(evt, () => this.handleInputClick());
    });
  }

  disconnectedCallback() {
    ['click', 'focus'].forEach(evt => {
      this.inputEl.removeEventListener(evt, event => this.handleDocumentClick(event));
    });
  }

  private truncateString(str, n) {
    return str.length > n ? str.substr(0, n - 1) + `...` : str;
  }

  private open() {
    setTimeout(() => {
      document.addEventListener('click', this.documentClickHandler);
      document.addEventListener('keydown', this.documentKeyDownHandler);
    });

    this.opened = true;
  }

  private close() {
    document.removeEventListener('click', this.documentClickHandler);
    document.removeEventListener('keydown', this.documentKeyDownHandler);
    this.opened = false;
  }

  private handleInputInput(event) {
    this.value = event.target.value;
  }

  private handleInputKeyDown(event) {
    if (event.key === 'Enter') {
      this.dispatchSearch(event.target.value);
      this.inputEl.blur();
    }

    if (['ArrowUp', 'ArrowDown'].includes(event.key)) {
      this.handleActivedescendant(event);
    }

    if (event.shiftKey && event.keyCode == 9) {
      this.close();
    }
  }

  private handleClearClicked() {
    const event = new window.Event('input', {
      bubbles: true,
      cancelable: true,
    });

    this.value = '';
    this.inputEl.value = '';

    this.clear.emit();
    this.dispatchSearch('');
    this.inputEl.dispatchEvent(event);
  }

  private handleInputClick() {
    this.open();
  }

  private handleDocumentClick(event) {
    if (event.target !== this.inputEl) {
      this.close();
    }
  }

  private handleDocumentKeyDown(event) {
    if (event.key === 'Escape') {
      this.close();
    }
  }

  private handleResultClick(result) {
    this.searchSelectedResult(result.title);
  }

  private handleResultKeyDown(event, result) {
    if (event.key === 'Enter') {
      this.searchSelectedResult(result.title);
    }

    if (['ArrowUp', 'ArrowDown'].includes(event.key)) {
      this.handleActivedescendant(event);
    }

    if (event.key === 'Tab') {
      this.close();
    }
  }

  private handleClearButtonOnKeyDown(event) {
    if (event.key === 'Tab') {
      this.close();
    }
  }

  private handleActivedescendant(event) {
    const inputField = document.querySelector('#dd-search-history');
    const activeElement = document.activeElement;

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      const prevSibling = activeElement.parentElement.previousElementSibling;

      if (prevSibling) {
        activeElement.parentElement.removeAttribute('aria-selected');
        prevSibling.setAttribute('aria-selected', 'true');
        inputField.setAttribute('aria-activedescendant', prevSibling.id);
      }
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      const nextSibling = activeElement.parentElement.nextElementSibling;
      this.selectFirstItem()

      if (nextSibling) {
        activeElement.parentElement.removeAttribute('aria-selected');
        nextSibling.setAttribute('aria-selected', 'true');
        inputField.setAttribute('aria-activedescendant', nextSibling.id);
      }
    }
  }

  private selectFirstItem() {
    if (!this.isExecuted) {
      document.getElementById('search-list').children[0].setAttribute('aria-selected', 'true');
      this.isExecuted = true;
    }
  };

  private searchSelectedResult(value: string) {
    this.value = value;
    this.dispatchSearch(value);
    this.filterAndShowDropdown(value);
    this.resultSelected.emit(value);
  }

  private dispatchSearch(value: unknown) {
    this.search.emit(value);
    this.close();
  }

  private createResults() {
    return (
      <ul role="listbox" class="dd-search__dropdown dd-search__dropdown--result-list" aria-labelledby="Search Results">
        {this.innerResults.map((result, iteration) => {
          return (
            <li
              //1st results = id
              id={'result-' + iteration}
              role="option"
              class="dd-search__result"
              onClick={() => this.handleResultClick(result)}
              onKeyDown={event => this.handleResultKeyDown(event, result)}
            >
              <button type="button" tabindex="-1" can-navigate>
                <dd-highlight text={result.title} highlight-text={this.value} />
                <dd-chevron class="dd-search__result__chevron" direction="right" />
              </button>
            </li>
          );
        })}
      </ul>
    );
  }

  private createEmptyResults() {
    return <p class="dd-search__dropdown dd-search__dropdown--empty">{i18next.t('search.no-results')}</p>;
  }

  private createLoadingBlock() {
    return (
      <p class="dd-search__dropdown dd-search__dropdown--loading">
        <dd-icon class="dd-icon--spin" name="loading" />
      </p>
    );
  }

  get shouldShowLoading() {
    return !this.hideDropdown && this.opened && this.isLoading;
  }

  get shouldShowResults() {
    return !this.hideDropdown && this.opened && !this.isLoading && this.innerResults.length > 0;
  }

  get shouldShowEmptyResults() {
    return (
      this.value &&
      this.value.length > 1 &&
      !this.hideDropdown &&
      this.opened &&
      !this.isLoading &&
      this.innerResults.length <= 0
    );
  }

  get isExpanded() {
    return this.shouldShowLoading || this.shouldShowResults || this.shouldShowEmptyResults;
  }

  constructor() {
    i18next.changeLanguage();
  }

  render() {
    return (
      <Host
        class={{
          'dd-search': true,
          'dd-search--expanded': this.isExpanded,
        }}
      >
        <form role="search">
          {this.label && (
            <h2 class="dd-search__header">
              <label class="dd-search__header__label" htmlFor={`dd-search-history`}>
                {this.label}
              </label>
            </h2>
          )}
          <input
            id='dd-search-history'
            aria-activedescendant="result-0"
            class="dd-input__input"
            placeholder={this.placeholder}
            type="search"
            value={this.value}
            onInput={event => this.handleInputInput(event)}
            onKeyDown={event => this.handleInputKeyDown(event)}
            ref={el => (this.inputEl = el)}
            role="combobox"
            aria-owns="search-list"
            aria-autocomplete="list"
            aria-haspopup="listbox"
            aria-expanded={this.isExpanded && !this.shouldShowEmptyResults ? 'true' : 'false'}
            aria-controls="search-list"
            autocomplete="off"
            can-navigate
          />
          <button
            type="button"
            class="dd-search__search-btn"
            onClick={() => this.dispatchSearch(this.value)}
            aria-label={i18next.t('search.search-aria-label')}
          >
            <dd-icon class="dd-search__search-btn__icon" name="search" showMargin={false} />
          </button>
          {!!this.value && (
            <button
              type="button"
              class="dd-input__clear-btn"
              onClick={() => this.handleClearClicked()}
              onKeyDown={event => this.handleClearButtonOnKeyDown(event)}
              aria-label={i18next.t('input.clear-aria-label')}
            >
              <dd-icon name="cross" class="dd-input__clear-btn__icon" showMargin={false} />
            </button>
          )}
          <div aria-live="polite">{this.shouldShowResults && this.createResults()}</div>
          <div role="status">
            {this.shouldShowEmptyResults && this.createEmptyResults()}
            {this.shouldShowLoading && this.createLoadingBlock()}
          </div>
        </form>
      </Host>
    );
  }
}
