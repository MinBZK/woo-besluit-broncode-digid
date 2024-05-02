import { newSpecPage } from '@stencil/core/testing';
import { DdChevron } from './dd-chevron';
import { h } from '@stencil/core';

describe('dd-chevron', () => {
  it('renders chevron-right as default', async () => {
    const page = await newSpecPage({
      components: [DdChevron],
      html: `<dd-chevron></dd-chevron>`,
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-chevron class='dd-chevron'>
        <svg class='dd-chevron__vertical' focusable='false' aria-hidden="true">
        	<use href='/assets/icons/icons.svg#chevron-right'></use>
       </svg>
      </dd-chevron>
    `);
  });

  it('renders chevron-left with left as input', async () => {
    const page = await newSpecPage({
      components: [DdChevron],
      html: `<dd-chevron></dd-chevron>`,
      template: () => {
        return <dd-chevron direction={'left'} />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-chevron class='dd-chevron'>
       <svg class='dd-chevron__vertical' focusable='false' aria-hidden="true">
        	<use href='/assets/icons/icons.svg#chevron-left'></use>
       </svg>
      </dd-chevron>
    `);
  });

  it('renders chevron-up with up as input', async () => {
    const page = await newSpecPage({
      components: [DdChevron],
      html: `<dd-chevron></dd-chevron>`,
      template: () => {
        return <dd-chevron direction={'up'} />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-chevron class='dd-chevron'>
				<svg class='dd-chevron__horizontal' focusable='false' aria-hidden="true">
						<use href='/assets/icons/icons.svg#chevron-up'></use>
				 </svg>
      </dd-chevron>
    `);
  });
});
