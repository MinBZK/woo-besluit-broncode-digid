import { newSpecPage } from '@stencil/core/testing';
import { DdLoginOptionsId } from './dd-login-options-id';
import { h } from '@stencil/core';

describe('dd-login-options-app', () => {
  const mockID = {
    status: 'OK',
    driving_licences: [],
    identity_cards: [
      { doc_num: PPPPPPPPP, sequence_no: PPPPPPPPPPPP, activated_at: '2021-11-29 15:43:00 +0100', active: false },
    ],
  };

  const mockLicence = {
    status: 'OK',
    driving_licences: [
      { doc_num: PPPPPPPPP, sequence_no: PPPPPPPPPPPP, activated_at: '2021-11-29 15:43:00 +0100', active: false },
    ],
    identity_cards: [],
  };

  it('should render', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockID as any} />;
      },
    });
    expect(page.root).toMatchSnapshot();
  });

  it('should render a card when correct data is provided as input', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockID as any} />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-id__card')).toBeTruthy();
  });

  it('should render an accordion when view is set to accordion', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockID as any} view="accordion" />;
      },
    });
    expect(page.root.querySelector('.dd-login-options-id__accordion')).toBeTruthy();
  });

  it('accordionOpen is false by default and becomes true after calling the toggle method', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      supportsShadowDom: false,
      template: () => {
        return <dd-login-options-id document={mockLicence as any} view="accordion" />;
      },
    });
    expect(page.rootInstance.accordionOpen).toBe(false);
    page.rootInstance.toggleAccordion();
    expect(page.rootInstance.accordionOpen).toBe(true);
  });

  it('should set accordionOpen to false after closeAccordion is called', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockLicence as any} view="accordion" accordion-open="true" />;
      },
    });

    expect(page.rootInstance.accordionOpen).toBe(true);
    page.rootInstance.closeAccordion();
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should close the accordion after the Enter key is pressed', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockID as any} view="accordion" accordion-open="true" />;
      },
    });
    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    page.root.querySelector('.dd-login-options-id__accordion__content__footer__content').dispatchEvent(enterEvent);
    expect(page.rootInstance.accordionOpen).toBe(false);
  });

  it('should call closeAccordion after the close button is clicked', async () => {
    const page = await newSpecPage({
      components: [DdLoginOptionsId],
      template: () => {
        return <dd-login-options-id document={mockID as any} view="accordion" accordion-open="true" />;
      },
    });
    page.rootInstance.el.scrollIntoView = jest.fn();

    const closeBtn: HTMLElement = page.root.querySelector('.dd-login-options-id__accordion__content__footer__content');
    const closeAccordionSpy = jest.spyOn(page.rootInstance, 'closeAccordion');
    closeBtn.click();

    expect(closeAccordionSpy).toHaveBeenCalled();
  });
});
