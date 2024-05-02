import { newSpecPage } from '@stencil/core/testing';
import { DdHighlight } from './dd-highlight';

describe('dd-highlight', () => {
  it('renders the highlighted text as strong tags', async () => {
    const page = await newSpecPage({
      components: [DdHighlight],
      html: `<dd-highlight text="Belastingdienst - Inkomstenbelasting over 2019" highlight-text="belast"></dd-highlight>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should return the same text if there is no highligh-text', async () => {
    const page = await newSpecPage({
      components: [DdHighlight],
      html: `<dd-highlight text="Belastingdienst" highlight-text=""></dd-highlight>`,
    });
    expect(page.root.text).toBe('Belastingdienst');
  });
});
