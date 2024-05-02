import { newSpecPage } from '@stencil/core/testing';
import { DdHeader } from './dd-header';

describe('dd-header', () => {
  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdHeader],
      html: `<dd-header></dd-header>`,
    });
    expect(page.root).toEqualHtml(`
     <dd-header class="dd-header">
       <div class="dd-header__links">
       	 <dd-skip-to-buttons></dd-skip-to-buttons>
         <dd-lang-toggle class="dd-header__links__toggle"></dd-lang-toggle>
      </div>
       <a aria-label="Logo Rijksoverheid - Ga naar www.digid.nl" class="dd-header__rijksvaandel" href="https://www.digid.nl">
        <img alt="" src="/assets/logo/rijksvaandel.svg">
      </a>
				<div class="dd-header__logout">
          <dd-button class="dd-header__logout__button" theme="primary" text="Uitloggen" />
        </div>
      <div class="dd-header__menu">
        <dd-menu id='navigation'></dd-menu>
      </div>
     </dd-header>
    `);
  });

  it('should call the logout function after the logout button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdHeader],
      html: `<dd-header></dd-header>`,
    });

    const btn: HTMLElement = page.root.querySelector('dd-button');
    btn.click();
    await page.waitForChanges();
    expect(page.rootInstance.loggedOut).toBe(true);
  });
});
