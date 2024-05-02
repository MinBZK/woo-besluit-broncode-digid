import { Component, Prop, h, Host, Event, EventEmitter, Listen, State } from '@stencil/core';
import { RouterHistory } from '@stencil/router';
import i18next from '../../utils/i18next';
import {
  clearToasts,
  globalErrorHandler,
  htmlEncode,
  setDocumentTitle,
  trackPageView,
  trackSiteSearch,
} from '../../global/global-methods';
import historyService from '../../api/services/dd.history.service';
import { parseDateFromISO, parseTimeFromISO } from '../../global/global-date-and-time';
import sessionService from '../../api/services/dd.session.service';
import { contactHelpdeskLink, hiddenNewWindow } from '../../global/global-jsx';

@Component({
  tag: 'dd-usage-history',
})
export class DdUsageHistory {
  // Add a media Query event Listener to check the viewport
  mediaQuery = matchMedia('(max-width: 432px)');
  @State() isMobile: boolean;

  //Property for accessing the router
  @Prop() history: RouterHistory;

  //Custom event when the navigation button is clicked
  @Event({ bubbles: true, composed: true }) navigationClicked: EventEmitter<string>;

  //Array containing all parsed usage history data
  @State() historyArray: Array<any>;

  //Paginated array, shown in the view
  @State() paginatedArray: Array<any>;

  //Amount of usage history allowed per page
  @State() itemsPerPage: number = 10;

  //The current page
  @State() currentPage: string = '1';

  //Total amount of usage history pages
  @State() totalPages: number;

  //Total amount of usage history items
  @State() totalItems: number;

  //Boolean used to indicate whether a filtered search has taken place
  @State() filteredResults: boolean = false;

	//The filter value
	@State() value: string;

  //The filter query
  @State() filter: string = '';

  //Listen for a pagination change from the paginator
  @Listen('pageChange', { target: 'body' })
  paginationChanged(event: CustomEvent) {
    if (event.detail) {
      //change content accordingly
      return this.fetchUserHistory(event.detail);
    }
  }

  //Listen for a search change from the input
  @Listen('search', { target: 'body' })
  searchResults(event: CustomEvent) {

  	this.value = event.detail;

    //Encode the searched value to remove non-whitelisted characters
    const searchedValue = htmlEncode(event.detail);

    //Assign the filter, if the filter is empty all history will be shown
    this.filter = searchedValue;

    //Set filteredResults to true if a filter has been entered for the first time
    this.filteredResults = true;

    //Fetch the history
    return this.fetchUserHistory('1').then(() => {
      if (event.detail.length > 0) {
        trackSiteSearch(searchedValue, this.totalItems);
      }
    });
  }

  componentWillLoad() {
    setDocumentTitle('Gebruiksgeschiedenis');
    trackPageView();
    return Promise.all([sessionService.updateSession(), this.fetchUserHistory('1')]);
  }

  componentWillRender() {
    document.title = i18next.t('history.page-title');
  }

  componentDidLoad() {
    if (this.mediaQuery?.matches) {
      this.checkViewport(this.mediaQuery);
      this.mediaQuery.addEventListener('change', this.checkViewport)
    }
  }

  // Check the view of mobile and desktop
  checkViewport = (mediaQuery) => {
    this.isMobile = mediaQuery.matches;
  };

  disconnectedCallback() {
    return clearToasts();
  }

  //Parse the ISO date string into readable text
  private parseHistoryArray(array) {
    array.forEach(item => {
      item.date = parseDateFromISO(item.created_at, true);
      item.time = parseTimeFromISO(item.created_at, true);
    });
    return array;
  }

  //Navigate to the login options
  private navigateLoginOptions() {
    if (this.history) {
      this.history.push(`/home`, {});
    }
    this.navigationClicked.emit('home');
  }

  private fetchUserHistory(index: string) {
    return historyService
      .getHistory(index, this.filter)
      .then(data => {
        this.paginatedArray = this.parseHistoryArray(data.account_logs);
        this.totalPages = data.total_pages;
        this.totalItems = data.total_items;
        this.currentPage = index;
      })
      .catch(() => {
        globalErrorHandler();
      });
  }

  private getSearchBar() {
    //return the search bar to be used multiple times
    return (
      <dd-search
        value={this.value}
        class="dd-usage-history__search"
        label={i18next.t('search.search-usage-history-label')}
        placeholder={i18next.t('search.input-placeholder')}
        disable-autocomplete={true}
      />
    );
  }

  private getAmountOfResults(){
  	return (
					<div class="dd-usage-history__results-status" aria-live="polite" role="log">
						{this.filteredResults && (i18next.t('history.displaying') +
						this.totalItems +
						i18next.t('history.results'))}
					</div>
				);
	}

  render() {
    return (
      <Host class="dd-usage-history">
        {this.isMobile ? (
          <div>
            <h1 class="dd-usage-history__header-mobile">{i18next.t('history.header')}</h1>
            <p>
              {i18next.t('history.subtitle')} {contactHelpdeskLink()}
            </p>
            {this.getSearchBar()}
            {this.getAmountOfResults()}
            <table class="dd-usage-history__grid" summary={i18next.t('history.header')}>
              <tr class="dd-usage-history__table--hidden-header">
                <th scope="col">{i18next.t('history.table-action-header')}</th>
                <th scope="col">{i18next.t('history.table-date-time-header')}</th>
              </tr>
              {this.paginatedArray &&
                this.paginatedArray.map(item => (
                  <tr class="dd-usage-history__grid__item">
                    <td class="dd-usage-history__grid__item--activity">
                      <dd-highlight text={item.name} highlight-text={this.filter} blue={true} />
                    </td>
                    <td class="dd-usage-history__grid__item--date">
                      {item.date} {i18next.t('general.at')} {item.time}
                    </td>
                  </tr>
                ))}
            </table>
            <div>{i18next.t('history.dutch-time-footer')}</div>
          </div>
      ): (
        <div>
          <div class="dd-usage-history__header">
            <h1>{i18next.t('history.header')}</h1>
            <p class="dd-usage-history__header__p">
              {i18next.t('history.subtitle')}{' '}
              <a
                href="https://www.digid.nl/contact/"
                target="_blank"
                aria-label={i18next.t('general.contact1') + ', ' + i18next.t('general.new-window')}
              >
                {i18next.t('general.contact1')}
              </a>{' '}
							{hiddenNewWindow()}
            </p>
          </div>
          {this.getSearchBar()}
					{this.getAmountOfResults()}
          <table class="dd-usage-history__table" summary={i18next.t('history.header')}>
            <tr class="dd-usage-history__table--hidden-header">
              <th scope="col">{i18next.t('history.table-date-header')}</th>
              <th scope="col">{i18next.t('history.table-time-header')}</th>
              <th scope="col">{i18next.t('history.table-action-header')}</th>
            </tr>
            {this.paginatedArray &&
              this.paginatedArray.map(item => (
                <tr class="dd-usage-history__grid">
                  <td class="dd-usage-history__grid--date">{item.date}</td>
                  <td>{item.time}</td>
                  <td class="dd-usage-history__grid--activity">
                    <dd-highlight text={item.name} highlight-text={this.filter} blue={true} />
                  </td>
                </tr>
              ))}
          </table>
          <div class="dd-usage-history__table-footer">
						{this.totalItems > 0 ?
							(i18next.t('history.dutch-time-footer'))
							:
							(i18next.t('history.no-results'))
							}
						</div>
        </div>
        )}
        {this.totalItems > 0 && (
          <div>
            <dd-paginator
              class="dd-usage-history__paginator"
              total-pages={this.totalPages}
              current-page={this.currentPage}
            />
          </div>
        )}
        <section>
          <div>
            <h2 class={this.isMobile ? 'h2-large' : undefined}>{i18next.t('login.header')}</h2>
          </div>
          <p>{i18next.t('login.sub-header-1')}</p>
          <dd-button onClick={() => this.navigateLoginOptions()} text={i18next.t('login.btn')} arrow="after" />
        </section>
      </Host>
    );
  }
}
