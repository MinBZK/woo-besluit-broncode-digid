const changeLanguageMock = jest.fn().mockImplementation(() => Promise.resolve());
const translateMock = jest.fn();

jest.mock('i18next', () => ({
  changeLanguage: changeLanguageMock,
  t: translateMock,
  use: jest.fn(() => {
    return { init: jest.fn() };
  }),
  LanguageDetector: jest.fn(),
  language: 'nl-NL',
}));

import { newSpecPage } from '@stencil/core/testing';
import { DdLangToggle } from './dd-lang-toggle';
import { h } from '@stencil/core';

describe('dd-lang-toggle', () => {
  it('should render with NL as default', async () => {
    const page = await newSpecPage({
      components: [DdLangToggle],
      template: () => {
        return <dd-lang-toggle />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-lang-toggle class="dd-lang-toggle">
        <a  class="active-link dd-lang-toggle__link" lang="nl" role="button" tabindex="0">NL</a>
        <div class="dd-lang-toggle__divider">|</div>
        <a  class="inactive-link dd-lang-toggle__link" lang="en" role="button" tabindex="0">EN</a>
      </dd-lang-toggle>
    `);
  });

  it('should change language to EN on click', async () => {
    const page = await newSpecPage({
      components: [DdLangToggle],
      template: () => {
        return <dd-lang-toggle />;
      },
    });
    /*
    TODO: Test refreshing of window, or only refresh stencil components
    const locationSpy = jest.spyOn(window.location, 'reload');
    expect(locationSpy).toHaveBeenCalled();
    */

    page.rootInstance.changeLang('en');
    expect(changeLanguageMock).toHaveBeenCalledWith('en');
  });

  it('should call changeLang on keypress Enter', async () => {
    const page = await newSpecPage({
      components: [DdLangToggle],
      template: () => {
        return <dd-lang-toggle />;
      },
    });
    const event = new KeyboardEvent('keydown', { key: 'Enter' });
    const changeLangSpy = jest.spyOn(page.rootInstance, 'changeLang');
    page.root.querySelector('.dd-lang-toggle__link').dispatchEvent(event);
    expect(changeLangSpy).toHaveBeenCalled();
  });
});
