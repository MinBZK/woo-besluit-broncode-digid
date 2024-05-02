import { newSpecPage } from '@stencil/core/testing';
import { DdSkipToButtons } from './dd-skip-to-buttons';

describe('dd-skip-to-buttons', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdSkipToButtons],
      html: `<dd-skip-to-buttons></dd-skip-to-buttons>`,
    });
    expect(page.root).toMatchSnapshot();
  });
});
