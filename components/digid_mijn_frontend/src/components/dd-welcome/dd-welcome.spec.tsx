import { newSpecPage } from '@stencil/core/testing';
import { DdWelcome } from './dd-welcome';
import { h } from '@stencil/core';

describe('dd-welcome', () => {
  it('renders', async () => {
    const mockAccountData = { bsn: 'PPPPPPPP' };
    const page = await newSpecPage({
      components: [DdWelcome],
      template: () => <dd-welcome data={mockAccountData} />,
    });
    expect(page.root).toEqualHtml(`
		<dd-welcome class="dd-welcome">
        <img src="assets/logo/digid.svg" alt="DigiD logo" />
        <div class="dd-welcome__text">
          <header>
            <h1> Welkom op Mijn DigiD</h1>
          </header>
          <p>
           Uw Burgerservicenummer is PPPPPPPP
          </p>
        </div>
      </dd-welcome>
		`);
  });
  TODO: it('Shows mobile version at a width of 400px', async () => {
    const page = await newSpecPage({
      components: [DdWelcome],
      html: `<dd-welcome></dd-welcome>`,
      supportsShadowDom: false,
    });
    global.matchMedia = jest.fn().mockImplementation(query => {
      return {
        addListener: () => { },
        removeListener: () => { },
        matches: query === '(max-width: 300px)',
      };
    });
    const header = page.body.querySelector('dd-welcome header');
    expect(header).not.toHaveClass('dd-welcome__mobile');
  });
});
