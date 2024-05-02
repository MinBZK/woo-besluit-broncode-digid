import { newSpecPage } from '@stencil/core/testing';
import { DdTooltipContent } from './dd-tooltip-content';

describe('dd-tooltip-content', () => {
  it('should render with the information icon', async () => {
    const page = await newSpecPage({
      components: [DdTooltipContent],
      html: `<dd-tooltip-content id="test-content"></dd-tooltip-content>`,
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
			<dd-tooltip-content class="dd-tooltip-content"  id="test-content">
         <dd-icon name="information" ></dd-icon>
         <div></div>
    </dd-tooltip-content>
		`);
  });
});
