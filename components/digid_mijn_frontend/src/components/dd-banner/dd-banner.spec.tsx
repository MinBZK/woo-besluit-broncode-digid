import { newSpecPage } from '@stencil/core/testing';
import { DdBanner } from './dd-banner';
import { h } from '@stencil/core';

describe('dd-banner', () => {
  it('should renders as two factor banner with the theme property set to twoFactor', async () => {
    const page = await newSpecPage({
      components: [DdBanner],
      template: () => {
        return <dd-banner theme="twoFactor" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toMatchSnapshot();
  });
  it('should renders as app banner with the theme property set to app', async () => {
    const page = await newSpecPage({
      components: [DdBanner],
      template: () => {
        return <dd-banner theme="app" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toMatchSnapshot();
  });
  it('should renders as ID check banner with the theme property set to idCheck', async () => {
    const page = await newSpecPage({
      components: [DdBanner],
      template: () => {
        return <dd-banner theme="idCheck" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should call the closing method and add the hidden class to the banner after the cross is clicked', async () => {
    const page = await newSpecPage({
      components: [DdBanner],
      template: () => {
        return <dd-banner theme="app" />;
      },
      supportsShadowDom: false,
    });

    const crossBtn: HTMLElement = page.body.querySelector('.dd-banner__cross');
    const closeBannerSpy = jest.spyOn(page.rootInstance, 'closeBanner');

    crossBtn.click();
    await page.waitForChanges();

    expect(closeBannerSpy).toHaveBeenCalled();
    expect(page.rootInstance.el).toHaveClass('dd-banner--hidden');
  });

  it('should call the closing clickBtn method after the button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdBanner],
      template: () => {
        return <dd-banner theme="twoFactor" />;
      },
      supportsShadowDom: false,
    });

    const btn: HTMLElement = page.body.querySelector('dd-button');
    const clickBtnSpy = jest.spyOn(page.rootInstance, 'clickBtn');

    btn.click();
    await page.waitForChanges();

    expect(clickBtnSpy).toHaveBeenCalled();
  });
});
