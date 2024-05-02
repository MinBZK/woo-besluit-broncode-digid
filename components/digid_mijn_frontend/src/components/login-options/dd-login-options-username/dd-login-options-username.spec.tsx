import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsUsername } from './dd-login-options-username';
import { h } from '@stencil/core';

describe('dd-login-options-app', () => {
  const mockUser1 = {
    username: 'PPPPPPPPP',
    sms: true,
    phone_number: 'PPPPPPPPPP',
    gesproken_sms: false,
    two_faactivated_date: null,
    zekerheidsniveau: 10,
  };
  const mockUser2 = {
    username: 'PPPPPPPPP',
    sms: true,
    phone_number: 'PPPPPPPPPP',
    gesproken_sms: false,
    two_faactivated_date: '26 mei 2020',
    zekerheidsniveau: 20,
  };

  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      template: () => {
        return <dd-login-options-username accountData={mockUser1 as any} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render a card when correct data is provided as input', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      template: () => {
        return <dd-login-options-username accountData={mockUser1 as any} />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-username__card')).toBeTruthy();
  });

  it('renders an accordion when view is set to accordion', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      template: () => {
        return <dd-login-options-username accountData={mockUser1 as any} view="accordion" />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-username__accordion')).toBeTruthy();
  });

  it('should show a warning in the accordion view when 2FA is turned off', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser1 as any} view="accordion" />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should show no warning in the accordion view when 2FA is turned on', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser2 as any} view="accordion" />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('accordionOpen is false by default and becomes true after calling the toggle method', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser2 as any} view="accordion" />;
      },
    });
    expect(page.rootInstance.accordionOpen).toBe(false);
    page.rootInstance.toggleAccordion();
    expect(page.rootInstance.accordionOpen).toBe(true);
  });

  it('should set accordionOpen to false after closeAccordion is called', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser2 as any} view="accordion" accordion-open="true" />;
      },
    });

    expect(page.rootInstance.accordionOpen).toBe(true);
    page.rootInstance.closeAccordion();
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should close the accordion after the Enter key is pressed', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser2 as any} view="accordion" accordion-open="true" />;
      },
    });
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    page.root
      .querySelector('.dd-login-options-username__accordion__content__footer__content')
      .dispatchEvent(enterEvent);
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should call closeAccordion after the close button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsUsername],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-username accountData={mockUser2 as any} view="accordion" />;
      },
    });
    page.rootInstance.el.scrollIntoView = jest.fn();

    const closeBtn: HTMLElement = page.root.querySelector(
      '.dd-login-options-username__accordion__content__footer__content',
    );
    const closeAccordionSpy = jest.spyOn(page.rootInstance, 'closeAccordion');
    closeBtn.click();

    expect(closeAccordionSpy).toHaveBeenCalled();
  });
});
