import { newSpecPage } from '@stencil/core/testing';
import { DdModal } from './dd-modal';

describe('dd-modal', () => {
  it('renders a modal without a close icon by default', async () => {
    const page = await newSpecPage({
      components: [DdModal],
      html: `<dd-modal header="header text"></dd-modal>`,
    });
    expect(page.root).toEqualHtml(`
		<dd-modal class="dd-modal" header="header text" aria-labelledby="dd-modal-header-1" aria-modal="true" class="dd-modal" header="header text" role="dialog">
		   <div aria-hidden="true" class="dd-visually-hidden" tabindex="0"></div>
       <header id="dd-modal-header-1" class="dd-modal__header">
        <dd-icon class="dd-modal__header__icon" name="warning"></dd-icon>
        <h2>header text</h2>
       </header>
		   <div aria-hidden="true" class="dd-visually-hidden" tabindex="0"></div>
    </dd-modal>
		`);
  });

  it('renders a modal with a close icon', async () => {
    const page = await newSpecPage({
      components: [DdModal],
      html: `<dd-modal header="header text" ></dd-modal>`,
    });
    expect(page.root).toEqualHtml(`
		<dd-modal class="dd-modal" header="header text" aria-labelledby="dd-modal-header-2" aria-modal="true" class="dd-modal" header="header text" role="dialog">
		    <div aria-hidden="true" class="dd-visually-hidden" tabindex="0"></div>
       <header id="dd-modal-header-2" class="dd-modal__header">
        <dd-icon class="dd-modal__header__icon" name="warning"></dd-icon>
       <h2>header text</h2>
       </header>
		    <div aria-hidden="true" class="dd-visually-hidden" tabindex="0"></div>
    </dd-modal>
		`);
  });

  it('passes the host to the backdropManager on show and hide', async () => {
    const page = await newSpecPage({
      components: [DdModal],
      html: `<dd-modal header="header text" allow-close="false"></dd-modal>`,
    });
    const backdropManager = {
      showElement: jest.fn(),
      hideElement: jest.fn(),
    };
    page.rootInstance.backdropManager = backdropManager;

    page.rootInstance.showModal();
    expect(backdropManager.showElement).toHaveBeenCalledWith(page.rootInstance.host);

    page.rootInstance.hideModal();
    expect(backdropManager.hideElement).toHaveBeenCalledWith(page.rootInstance.host);
  });
});
