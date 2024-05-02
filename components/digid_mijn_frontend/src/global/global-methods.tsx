import i18next from '../utils/i18next';
import { EnvironmentConfigService } from '../environment-config.service';
const dev: boolean = EnvironmentConfigService.getInstance().get('dev');

export function usingDevMode() {
  return window.location.hostname === 'localhost' && dev;
}

/**
 * Cookie handler functions
 */

function getSubDomain(domain) {
  if (domain) {
    return '.' + domain.match(/[^\.]*\.[^.]*$/);
  }
}

export function setCookie(name, value, days) {
  const d = new Date();
  d.setTime(d.getTime() + days * 24 * 60 * 60 * 1000);
  const expires = 'expires=' + d.toUTCString();
  document.cookie = name + '=' + value + ';' + expires + ';path=/;domain=' + getSubDomain(document.domain) + ';secure';
}

export function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  return parts.pop().split(';').shift();
}

//Global function to call in a catch block to suppress console errors. Can be used for global error handling
let errors = 0;
export function globalErrorHandler() {
  errors = errors + 1;
}

//Function used to encode non-whitelisted values into HTML entities to prevent Cross Site Scripting
export function htmlEncode(str) {
  return String(str).replace(/[^\w. ]/gi, function (c) {
    return '&#' + c.charCodeAt(0) + ';';
  });
}

/**
 * Matomo functions
 */

//used to disable matomo tracking, can be overwritten from the Config API
let matomoEnabled;

//Used to console log matomo events, false by default. Set to true if you want to log events
const consoleLogMatomo = false;

export function logMatomo(logString) {
  if (consoleLogMatomo) {
    console.info(logString);
  }
}

export function setMatomoEnabled(enabled) {
  matomoEnabled = enabled;
}

export function matomoObject() {
  if (matomoEnabled) {
    return (window as any)._paq || [];
  } else {
    return [];
  }
}

export function setDocumentTitle(title) {
  if (matomoEnabled) {
    matomoObject().push(['setDocumentTitle', 'Mijn DigiD | ' + title]);
  } else {
    logMatomo('setDocumentTitle: Mijn DigiD | ' + title);
  }
}

export function trackPageView() {
  if (matomoEnabled) {
    matomoObject().push(['trackPageView']);
  } else {
    logMatomo('trackPageView');
  }
}

export function trackInteractionEvent(action, name?) {
  if (matomoEnabled) {
    matomoObject().push(['trackEvent', 'interactie', action, name]);
  } else {
    logMatomo('trackEvent: ' + action + ': ' + name);
  }
}

export function trackGoal(goalNumber) {
  if (matomoEnabled) {
    matomoObject().push(['trackGoal', goalNumber]);
  } else {
    logMatomo('trackGoal: ' + goalNumber);
  }
}

export function trackSiteSearch(searchedTerm, numberOfResults) {
  if (matomoEnabled) {
    matomoObject().push([
      'trackSiteSearch',
      // Search keyword searched for
      searchedTerm,
      // Search category selected in your search engine. If you do not need this, set to false
      false,
      // Number of results on the Search results page. Zero indicates a 'No Result Search Keyword'. Set to false if you don't know
      numberOfResults || false,
    ]);
  } else {
    logMatomo('trackSiteSearch: ' + searchedTerm + ', results: ' + numberOfResults);
  }
}

/**
 * Detect browser
 */

export function detectBrowser() {
  let browserName = (function (agent) {
    switch (true) {
      case agent.indexOf('edge') > -1:
        return 'MS Edge';
      case agent.indexOf('edg/') > -1:
        return 'Edge ( chromium based)';
      case agent.indexOf('opr') > -1:
        return 'Opera';
      case agent.indexOf('chrome') > -1:
        return 'Chrome';
      case agent.indexOf('trident') > -1:
        return 'MS IE';
      case agent.indexOf('firefox') > -1:
        return 'Mozilla Firefox';
      case agent.indexOf('safari') > -1:
        return 'Safari';
      default:
        return 'other';
    }
  })(window.navigator.userAgent.toLowerCase());
  if (browserName === 'MS IE') {
    return createToastMessage({
      type: 'information',
      message:
        'Deze browser is niet geschikt voor mijn DigiD. U kunt De browser afsluiten en een andere browser openen zoals Chrome, Edge, Firefox of Safari.',
    });
  }
}

/**
 * Global getters and setters
 */

//Initiate with the login confirmation text
let ariaUpdateText = i18next.t('login.logged-in');

export function setAriaUpdateText(text) {
	ariaUpdateText = text;
}

export function getAriaUpdateText() {
	return ariaUpdateText;
}

let userIsDeceased: boolean;

export function setDeceased(deceased) {
  userIsDeceased = deceased;
}

export function getDeceased() {
  return userIsDeceased;
}

let pinResetDrivingLicences: boolean;

export function setPinResetDrivingLicences(pinReset) {
  pinResetDrivingLicences = pinReset;
}

export function canRequestPinOnDrivingLicences() {
  return pinResetDrivingLicences;
}

//Setting the array of apps globally, to be used by multiple components
let globalApps = [];

export function setGlobalApps(apps) {
  globalApps = apps;
}

export function getGlobalApps() {
  return globalApps;
}

//Setting the array of apps globally, to be used by multiple components
let globalIdentityCards = [];

export function setGlobalCards(cards) {
  globalIdentityCards = cards;
}

export function getGlobalCards() {
  return globalIdentityCards;
}

let globalLicences = [];

export function setGlobalLicences(licences) {
  globalLicences = licences;
}

export function getGlobalLicences() {
  return globalLicences;
}

//Whether the user has only 1 app as login method, used to hide the DigiD app deactivation link
let userAppIsLastLoginMethod: boolean;

export function setAppIsLastLoginMethod(has1app) {
	userAppIsLastLoginMethod = has1app;
}

export function getAppIsLastLoginMethod() {
  return userAppIsLastLoginMethod;
}

let show2FAWarningWhenDeactivatingApp: boolean;

export function setShow2FAWarningWhenDeactivatingApp(showWarning){
	show2FAWarningWhenDeactivatingApp = showWarning;
}

export function getShow2FAWarningWhenDeactivatingApp(){
	return show2FAWarningWhenDeactivatingApp;
}

/**
 * Create a toast alert message
 */
export async function createToastMessage(message) {
  await customElements.whenDefined('dd-toast');
  const toast = document.getElementById('mainToast') as HTMLDdToastElement;
  // @ts-ignore
  await toast.pushMessage(message);
}

/**
 * Remove all toast alert messages
 */
export async function clearToasts() {
  await customElements.whenDefined('dd-toast');
  const toast = document.getElementById('mainToast') as HTMLDdToastElement;
  // @ts-ignore
  await toast.clearMessages();
}

//Assign an i18next status
export function getStatus(status) {
  return {
    actief: i18next.t('general.active'),
    niet_actief: i18next.t('general.inactive'),
    geblokkeerd: i18next.t('general.blocked'),
    geschorst: i18next.t('general.suspended'),
    ingetrokken: i18next.t('general.revoked'),
  }[status];
}

//Return tooltip content related to the document state
export function getStatusTooltip(status, type) {
  if (status === 'actief') {
    return i18next.t(`id.login-status-tooltip-active-${type}`);
  } else if (status === 'geblokkeerd') {
    return i18next.t(`id.login-status-tooltip-blocked-${type}`);
  } else if (status === 'ingetrokken') {
    return i18next.t(`id.login-status-tooltip-revoked-${type}`);
  } else if (status === 'niet_actief') {
    return i18next.t(`id.login-status-tooltip-inactive-${type}`);
  }
}

//Return tooltip content related to the SMS verification
export function getSMStooltip(sms) {
  if (sms === 'pending') {
    return i18next.t('username.sms-tooltip-pending');
  } else if (sms === 'niet_actief') {
    return i18next.t('username.sms-tooltip-inactive');
  } else {
    return i18next.t('username.sms-tooltip-active');
  }
}
