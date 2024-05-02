import { newSpecPage } from '@stencil/core/testing';
import { DdStatus } from './dd-status';
import { h } from '@stencil/core';

describe('dd-status', () => {
  it('renders an active status with icon', async () => {
    const page = await newSpecPage({
      components: [DdStatus],
      html: `<dd-status></dd-status>`,
      template: () => {
        return <dd-status active={true} />;
      },
    });
    expect(page.root).toEqualHtml(`
      <dd-status class="dd-status">
      	<dd-icon class="dd-status__icon" name="safety-on"></dd-icon>
      	<p class="status-active"></p>
      </dd-status>
    `);
  });

  it('renders without icon', async () => {
    const page = await newSpecPage({
      components: [DdStatus],
      html: `<dd-status></dd-status>`,
      template: () => {
        return <dd-status active={false} icon={false} />;
      },
    });
    expect(page.root).toEqualHtml(`
      <dd-status class="dd-status">
      	<p class="status-inactive"></p>
      </dd-status>
    `);
  });

  it('shows completed text and safety off badge', async () => {
    const page = await newSpecPage({
      components: [DdStatus],
      html: `<dd-status></dd-status>`,
      template: () => {
        return <dd-status active={false} completed={true} />;
      },
    });
    expect(page.root).toEqualHtml(`
      <dd-status class="dd-status">
      <dd-icon class="dd-status__icon" name="safety-off" ></dd-icon>
      	<p class="status-inactive"></p>
      </dd-status>
    `);
  });
});
