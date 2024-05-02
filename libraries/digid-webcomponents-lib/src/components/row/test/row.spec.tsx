import { newSpecPage } from '@stencil/core/testing';
import { Row } from '../row';

describe('logius-row', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [Row],
      html: `<logius-row>Row</logius-row>`,
    });
    expect(page.root).toEqualHtml(`
      <logius-row>
        Row
      </logius-row>
    `);
  });
});
