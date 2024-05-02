import { usingDevMode } from '../../global/global-methods';
import { defaultFetch } from './dd.service';

let CSRFtoken;

export default {
  getSession() {
    if (usingDevMode()) {
      return;
    }
    return defaultFetch('get_session_data').then(data => {
      //Set the CSRFtoken to be used to update the session with
      CSRFtoken = data.csrf;

      //Return the time at which the session will time out
      return { timestamp: new Date(data.timestamp).getTime(), minutes: data.minutes };
    });
  },
  updateSession() {
    if (usingDevMode()) {
      return;
    }
    return fetch('/api/touch_session', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': CSRFtoken },
    });
  },
};
