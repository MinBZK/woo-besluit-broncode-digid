import { newSpecPage } from '@stencil/core/testing';
import { Column } from '../column';

describe('logius-col', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [Column],
      html: `<logius-col>Column</logius-col>`,
    });
    expect(page.root).toEqualHtml(`
      <logius-col>
        Column
      </logius-col>
    `);
  });
});
