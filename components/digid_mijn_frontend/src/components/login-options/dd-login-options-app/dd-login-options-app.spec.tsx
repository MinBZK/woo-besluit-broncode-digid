import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsApp } from './dd-login-options-app';
import { h } from '@stencil/core';

describe('dd-login-options-app', () => {
  const mockApp = {
    device_name: 'PPPPPPPPPPPPPPP',
    instance_id: 'SO945M',
    last_sign_in_at: '2021-09-28T07:25:01Z',
    status: 'active',
    activated_at: '2021-09-28T07:25:01Z',
    idCheck: true,
    substantieel_document_type: 'Nationale identiteitskaart',
    docNum: 'PPPPPPPPPP',
  };

  it('should render a card when correct data is provided as input', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-app__card')).toBeTruthy();
  });

  it('should render an accordion when view is set to accordion', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} view="accordion" />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-app__accordion')).toBeTruthy();
  });

  it('accordionOpen is false by default and becomes true after calling the toggle method', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} view="accordion" />;
      },
    });
    expect(page.rootInstance.accordionOpen).toBe(false);
    page.rootInstance.toggleAccordion();
    expect(page.rootInstance.accordionOpen).toBe(true);
  });

  it('should set accordionOpen to false after closeAccordion is called', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} view="accordion" accordion-open="true" />;
      },
    });

    expect(page.rootInstance.accordionOpen).toBe(true);
    page.rootInstance.closeAccordion();
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should close the accordion after the Enter key is pressed', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} view="accordion" accordion-open="true" />;
      },
    });
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    page.root.querySelector('.dd-login-options-app__accordion__content__footer__content').dispatchEvent(enterEvent);
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should call closeAccordion after the close button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsApp],
      template: () => {
        return <dd-login-options-app appData={mockApp as any} view="accordion" accordion-open="true" />;
      },
    });
    page.rootInstance.el.scrollIntoView = jest.fn();
    const closeBtn: HTMLElement = page.root.querySelector('.dd-login-options-app__accordion__content__footer__content');
    const closeAccordionSpy = jest.spyOn(page.rootInstance, 'closeAccordion');
    closeBtn.click();
    expect(closeAccordionSpy).toHaveBeenCalled();
  });
});
