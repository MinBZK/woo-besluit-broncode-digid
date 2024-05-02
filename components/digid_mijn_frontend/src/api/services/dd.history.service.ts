import { defaultFetch } from './dd.service';

export default {
  /**
   * Retrieve the usage history
   * @page: the to be shown page
   * @query: the filter, can be empty
   */
  getHistory(page: string, query: string) {
    return defaultFetch('accounthistory?page=' + page + '&query=' + query);
  },
};
