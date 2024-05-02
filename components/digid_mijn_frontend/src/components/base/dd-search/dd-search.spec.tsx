import { newSpecPage } from '@stencil/core/testing';
import { DdSearch } from './dd-search';
import { mockHistorySuggestions } from '../../../global/dev-data';

describe('dd-search', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search></dd-search>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('renders with results specified', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search results='[{ "title": "abc", "value": 1 }]' value="ab"></dd-search>`,
    });

    page.rootInstance.inputEl.dispatchEvent(new MouseEvent('click'));

    await page.waitForChanges();

    expect(page.root).toMatchSnapshot();
  });

  it('renders message with no results specified', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search results='[]' value="ab"></dd-search>`,
    });

    page.rootInstance.inputEl.dispatchEvent(new MouseEvent('click'));

    await page.waitForChanges();

    expect(page.root).toMatchSnapshot();
  });

  it('renders loading when is-loading is set', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search value="ab" is-loading="true"></dd-search>`,
    });

    page.rootInstance.inputEl.dispatchEvent(new MouseEvent('click'));

    await page.waitForChanges();

    expect(page.root).toMatchSnapshot();
  });

  it('dispatches clear event when clear icon is clicked', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search value="ab"></dd-search>`,
    });

    const clearEventSpy = jest.spyOn(page.rootInstance.clear, 'emit');
    const clearBtn = page.doc.querySelector('.dd-input__clear-btn');

    clearBtn.dispatchEvent(new MouseEvent('click'));

    expect(clearEventSpy).toHaveBeenCalled();
  });

  it('dispatches search event when search icon is clicked', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search value="ab"></dd-search>`,
    });

    const searchEventSpy = jest.spyOn(page.rootInstance.search, 'emit');
    const searchBtn = page.doc.querySelector('.dd-search__search-btn');

    searchBtn.dispatchEvent(new MouseEvent('click'));

    expect(searchEventSpy).toHaveBeenCalledWith('ab');
  });

  it('dispatches search event when enter is pressed on input', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search value="ab"></dd-search>`,
    });

    const searchEventSpy = jest.spyOn(page.rootInstance.search, 'emit');

    page.rootInstance.inputEl.blur = jest.fn(); // stub blur for testing environment
    page.rootInstance.inputEl.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(searchEventSpy).toHaveBeenCalledWith('ab');
  });

  it('dispatches resultSelected event when enter is pressed on result', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search results='[{ "title": "abc", "value": 1 }]' value="ab"></dd-search>`,
    });

    const searchEventSpy = jest.spyOn(page.rootInstance.resultSelected, 'emit');

    page.rootInstance.inputEl.dispatchEvent(new MouseEvent('click'));
    await page.waitForChanges();

    page.doc.querySelector('.dd-search__result').dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(searchEventSpy).toHaveBeenCalledWith('abc');
  });

  it('should dispatch resultSelected event when a result is clicked', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search results='[{ "title": "abc", "value": 1 }]' value="ab"></dd-search>`,
    });

    const searchEventSpy = jest.spyOn(page.rootInstance.resultSelected, 'emit');

    page.rootInstance.inputEl.dispatchEvent(new MouseEvent('click'));
    await page.waitForChanges();

    page.doc.querySelector('.dd-search__result').dispatchEvent(new MouseEvent('click'));

    expect(searchEventSpy).toHaveBeenCalledWith('abc');
  });

  it('should show a dropdown with suggestions based on input', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search ></dd-search>`,
    });

    page.rootInstance.searchSuggestions = mockHistorySuggestions;
    page.rootInstance.filterAndShowDropdown('test');
    expect(page.rootInstance.innerResults).toEqual([{ title: 'new test' }, { title: 'other test' }, { title: 'test' }]);
  });

  it('should truncate long strings', async () => {
    const page = await newSpecPage({
      components: [DdSearch],
      html: `<dd-search ></dd-search>`,
    });

    page.rootInstance.searchSuggestions = mockHistorySuggestions;
    page.rootInstance.filterAndShowDropdown('Very long string that wil now be used for a unit test');
    expect(page.rootInstance.value).toBe('Very long string that wil now be used f...');
  });
});
