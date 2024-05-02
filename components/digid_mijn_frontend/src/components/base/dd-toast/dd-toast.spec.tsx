import { newSpecPage } from '@stencil/core/testing';
import { DdToast } from './dd-toast';

describe('dd-toast', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [DdToast],
      html: `<dd-toast></dd-toast>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-toast class="dd-toast" role="alert">
      </dd-toast>
    `);
  });

  it('renders', async () => {
    const page = await newSpecPage({
      components: [DdToast],
      html: `<dd-toast></dd-toast>`,
    });
    const pushMessageSpy = jest.spyOn(page.rootInstance, 'pushMessage');
    page.rootInstance.pushMessage({ type: 'success', message: 'This is a success message for the user, yay! :)' });

    expect(pushMessageSpy).toHaveBeenCalled();
    expect(page.root).toEqualHtml(`

      <dd-toast class="dd-toast" role="alert"></dd-toast>

    `);
  });
});
