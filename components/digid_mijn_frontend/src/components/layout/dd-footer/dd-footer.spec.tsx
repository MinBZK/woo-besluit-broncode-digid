import { newSpecPage } from '@stencil/core/testing';
import { DdFooter } from './dd-footer';

describe('dd-footer', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdFooter],
      html: `<dd-footer></dd-footer>`,
    });
    expect(page.root).toMatchSnapshot();
  });
});
