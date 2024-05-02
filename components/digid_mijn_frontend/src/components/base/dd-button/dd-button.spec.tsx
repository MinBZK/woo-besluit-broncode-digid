import { newSpecPage } from '@stencil/core/testing';
import { DdButton } from './dd-button';
import { h } from '@stencil/core';

describe('dd-chevron', () => {
  it('should throw an Error when no text and arrow attribute is provided', async () => {
    await expect(
      newSpecPage({
        components: [DdButton],
        html: `<dd-button ></dd-button>`,
        template: () => {
          return <dd-button />;
        },
        supportsShadowDom: false,
      }),
    ).rejects.toThrow(new Error('Button needs a text attribute!'));
  });

  it('should default to the primary theme', async () => {
    const page = await newSpecPage({
      components: [DdButton],
      html: `<dd-button ></dd-button>`,
      template: () => {
        return <dd-button text="test" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-button class="dd-button--primary">
        <button class="dd-button" tabindex="0">
            <span class="dd-button__text">test</span>
        </button>
      </dd-button>
    `);
  });

  it('Should have the secondary theme when provided as attribute', async () => {
    const page = await newSpecPage({
      components: [DdButton],
      html: `<dd-button ></dd-button>`,
      template: () => {
        return <dd-button text="test" theme="secondary" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-button class="dd-button--secondary">
        <button class="dd-button" tabindex="0">
            <span class="dd-button__text">test</span>
        </button>
      </dd-button>
    `);
  });

  it('Should have tertiary theme when provided as attribute and should show an arrow', async () => {
    const page = await newSpecPage({
      components: [DdButton],
      html: `<dd-button ></dd-button>`,
      template: () => {
        return <dd-button text="test" arrow="after" theme="tertiary" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-button class="dd-button--tertiary">
        <button class="dd-button" tabindex="0">
            <span class="dd-button__text">test</span>
            <dd-chevron  class="dd-button__chevron-after" direction="right"></dd-chevron>
        </button>
      </dd-button>
    `);
  });

  it('Should add a role attribute when the roleOverride property is used', async () => {
    const page = await newSpecPage({
      components: [DdButton],
      html: `<dd-button ></dd-button>`,
      template: () => {
        return <dd-button text="test" role-override="link" />;
      },
      supportsShadowDom: false,
    });
    expect(page.root).toEqualHtml(`
      <dd-button class="dd-button--primary" role-override="link">
        <button class="dd-button" tabindex="0" role='link'>
            <span class="dd-button__text">test</span>
        </button>
      </dd-button>
    `);
  });
});
