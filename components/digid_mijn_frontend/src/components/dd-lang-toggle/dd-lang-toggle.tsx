import { Component, Host, h, State, EventEmitter, Event } from '@stencil/core';
import i18next from 'i18next';
import { getCookie, setCookie } from '../../global/global-methods';

@Component({
  tag: 'dd-lang-toggle',
})
export class DdLangToggle {
  //The currently selected language, default is NL
  @State() selectedLang = 'nl';

  @Event({ bubbles: true }) languageToggled: EventEmitter;

  private changeLang(lang) {
    this.selectedLang = lang;

    //Change to the selected language
    i18next.changeLanguage(lang).then(() => {
      //Emit en event that will refresh all components
      this.languageToggled.emit();

      //Change the language in the cookies
      setCookie('locale', lang, 365);
    });
  }

  private keydownHandler(event) {
    if (event.key === 'Enter') {
      this.changeLang(event.target.lang);
    }
  }

  componentWillRender() {
    //Extract the language from cookies, if there is no lang in de cookies the default will remain nl
    const languageFromCookies = getCookie('locale') || 'nl';

    if (languageFromCookies !== this.selectedLang || languageFromCookies !== i18next.language) {
      this.changeLang(languageFromCookies);
    }

    //Set the lang attribute in the html element
    document.documentElement.lang = this.selectedLang;
  }
  render() {
    return (
      <Host class="dd-lang-toggle">
        <a
          class={{
            'dd-lang-toggle__link': true,
            'active-link': this.selectedLang === 'nl',
            'inactive-link': this.selectedLang === 'en',
          }}
          lang="nl"
          onClick={() => this.changeLang('nl')}
          onKeyDown={event => this.keydownHandler(event)}
          aria-label={i18next.t('language.Dutch')}
          tabindex="0"
          role="button"
        >
          NL
        </a>
        <div class="dd-lang-toggle__divider">|</div>
        <a
          class={{
            'dd-lang-toggle__link': true,
            'active-link': this.selectedLang === 'en',
            'inactive-link': this.selectedLang === 'nl',
          }}
          lang="en"
          onClick={() => this.changeLang('en')}
          onKeyDown={event => this.keydownHandler(event)}
          aria-label={i18next.t('language.English')}
          tabindex="0"
          role="button"
        >
          EN
        </a>
      </Host>
    );
  }
}
