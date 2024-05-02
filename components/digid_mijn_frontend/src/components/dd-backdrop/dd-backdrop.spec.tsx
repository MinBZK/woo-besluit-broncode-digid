import { newSpecPage } from '@stencil/core/testing';
import { DdBackdrop } from './dd-backdrop';

describe('dd-backdrop', () => {
  it('renders a backdrop', async () => {
    const page = await newSpecPage({
      components: [DdBackdrop],
      html: `<dd-backdrop></dd-backdrop>`,
    });
    expect(page.root).toEqualHtml(`
			<dd-backdrop class="dd-backdrop">
				<div class="dd-backdrop__shadow"></div>
				<div class="dd-backdrop__children"></div>
			</dd-backdrop>
		`);
  });

  it('should be able to show and hide an element and toggle the scrollblock', async () => {
    const page = await newSpecPage({
      components: [DdBackdrop],
      html: `<dd-backdrop></dd-backdrop>`,
    });
    const rcBackdropChildren = document.body.children[0].children[1];
    const elParent = document.createElement('div');
    const el = document.createElement('article');
    elParent.append(el);

    await page.rootInstance.showElement(el);

    expect(rcBackdropChildren.children[0].nodeName).toBe('ARTICLE');
    expect(document.documentElement.className).toBe('dd-backdrop-body-scrollblock');

    await page.rootInstance.hideElement(el);

    expect(rcBackdropChildren.children.length).toBe(0);
    expect(document.documentElement.className).toBe('');
  });
});
