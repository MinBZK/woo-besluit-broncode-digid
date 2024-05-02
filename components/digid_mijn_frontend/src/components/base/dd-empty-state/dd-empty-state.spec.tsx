import { newSpecPage } from '@stencil/core/testing';
import { DdEmptyState } from './dd-empty-state';
import { h } from '@stencil/core';

describe('dd-empty-state', () => {
  const docData = {
    status: 'OK',
    driving_licences: [],
    show_driving_licences: true,
    identity_cards: [],
    show_identity_cards: true,
  };

  it('should render as an ID empty state with the id type', async () => {
    const page = await newSpecPage({
      components: [DdEmptyState],
      template: () => {
        return <dd-empty-state documentData={docData} type="id" />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render as an app empty state with the app type', async () => {
    const page = await newSpecPage({
      components: [DdEmptyState],
      html: `<dd-empty-state type="app"></dd-empty-state>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render without a body text when card mode is false', async () => {
    const page = await newSpecPage({
      components: [DdEmptyState],
      html: `<dd-empty-state type="app" card="false"></dd-empty-state>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render an error empty state', async () => {
    const page = await newSpecPage({
      components: [DdEmptyState],
      html: `<dd-empty-state type="error"></dd-empty-state>`,
    });
    expect(page.root).toMatchSnapshot();
  });
});
