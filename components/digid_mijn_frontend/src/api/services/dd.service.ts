import { setMatomoEnabled, usingDevMode } from '../../global/global-methods';
import devUsers from '../../global/dev-data';

export function //Defining the default service call to be reused
defaultFetch(api: string) {
  const baseUrl = '/api/';

  //Return test data is DEV mode is being used
  if (usingDevMode()) {
    const devUserId = localStorage.getItem('devUserId') || 1; // set from the dropdown or user selection as you suggested in our call
    const responsesForUser = devUsers[devUserId];
    const mockedResponses = Object.keys(responsesForUser);
    if (mockedResponses.indexOf(api) >= 0) {
      return Promise.resolve(responsesForUser[api]);
    }
    return;
  }

  //If DEV mode is disabled, call the API
  return fetch(`${baseUrl}` + api).then(response => {
    //If the HTTP status is 401 unauthorized, throw an Error
    if (response.status === 401) {
      document.querySelector('dd-session-handler').timeoutSession();
      throw new Error('Session expired');
    }
    return response.json();
  });
}

export default {
  getNotifications() {
    return defaultFetch('notification');
  },
	getNewsItems() {
  	return defaultFetch('news_items?page=Mijn DigiD')
	},
  getConfig() {
    return defaultFetch('config').then(data => {
      setMatomoEnabled(data.analytics.enabled);
      return data;
    });
  },
};
