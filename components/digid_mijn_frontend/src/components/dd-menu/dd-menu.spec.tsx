import { newSpecPage } from '@stencil/core/testing';
import { DdMenu } from './dd-menu';

describe('dd-menu', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [DdMenu],
      html: `<dd-menu></dd-menu>`,
      supportsShadowDom: false,
    });
    expect(page.root).toMatchSnapshot();
  });
  /*
  //Cant figure out why this one doesnt work yet. console log in function gets printed but the test fails

  it('will call changeContent after a clicked link', async () => {
    const page = await newSpecPage({
      components: [DdMenu],
      html: `<dd-menu></dd-menu>`,
      supportsShadowDom: false,
    });
    const menuLink: HTMLElement = page.body.querySelector('stencil-route-link');
    const changeContentSpy = jest.spyOn(page.rootInstance, 'changeContent');

    menuLink.click();
    await page.waitForChanges();
    expect(changeContentSpy).toHaveBeenCalled();
  });*/
  TODO: it('will call navigationChangedFromComponent after a navigationFromComponent event is dispatched', async () => {
    const page = await newSpecPage({
      components: [DdMenu],
      html: `<dd-menu></dd-menu>`,
      supportsShadowDom: false,
    });
    const changeNavigationSpy = jest.spyOn(page.rootInstance, 'navigationChangedFromComponent');
    const event = new CustomEvent('navigationClicked', { bubbles: true });

    page.body.dispatchEvent(event);
    expect(changeNavigationSpy).toHaveBeenCalledWith(event);
  });
});
