import { newSpecPage } from '@stencil/core/testing';
import { DdTooltip } from './dd-tooltip';
import { h } from '@stencil/core';

beforeAll(() => {
  //Mock getElementById and setAttribute
  Object.defineProperty(document, 'getElementById', {
    writable: true,
    value: jest.fn().mockImplementation(() => ({
      setAttribute: jest.fn(),
      style: { display: 'flex' },
    })),
  });
});

describe('dd-tooltip', () => {
  it('should be closed by default', async () => {
    const page = await newSpecPage({
      components: [DdTooltip],
      html: `<dd-tooltip id="test-button" contentId="test-content" ></dd-tooltip>`,
      supportsShadowDom: false,
    });

    expect(page.root).toEqualHtml(`
			<dd-tooltip id="test-button" contentId='test-content' >
				<button class="dd-tooltip" aria-label='' >
					 <dd-icon name="tooltip" />
				</button>
			</dd-tooltip>
		`);
  });

  it('Should open and emit an event after the icon is clicked', async () => {
    const page = await newSpecPage({
      components: [DdTooltip],
      html: `<dd-tooltip id="test-button" contentId="test-content" ></dd-tooltip>`,
      supportsShadowDom: false,
    });
    const icon = page.body.querySelector('dd-icon');
    icon.click();
    await page.waitForChanges();

    expect(page.root).toEqualHtml(`
		<dd-tooltip id="test-button" contentId='test-content' >
			<button aria-expanded=""  aria-label='' class="dd-tooltip" >
         <dd-icon name="tooltip-clicked" />
      </button>
    </dd-tooltip>
		`);
  });

  it('should be able to render opened', async () => {
    const page = await newSpecPage({
      components: [DdTooltip],
      html: `<dd-tooltip id="test-button" tooltip-opened="true" contentId="test-content" ></dd-tooltip>`,
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
		<dd-tooltip id="test-button" contentId='test-content' tooltip-opened="true">
			<button aria-expanded="" aria-label="" class="dd-tooltip" >
         <dd-icon name="tooltip-clicked" />
      </button>
    </dd-tooltip>
		`);
  });

	it('should render with an aria-label if ariaText is provided', async () => {
		const page = await newSpecPage({
			components: [DdTooltip],
			template: () => { return  <dd-tooltip id="test-button" tooltip-opened="true" contentId="test-content" ariaText='test'/>},
			supportsShadowDom: false,
		});
		expect(page.root).toEqualHtml(`
		<dd-tooltip id="test-button" tooltip-opened="true" >
			<button  aria-controls="test-content" aria-expanded="" aria-label="Informatie over test" class="dd-tooltip" >
         <dd-icon name="tooltip-clicked" />
      </button>
    </dd-tooltip>
		`);
	});
});
