import { newSpecPage } from '@stencil/core/testing';
import { DdAlert } from './dd-alert';

describe('dd-alert', () => {
  it('renders an info alert', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert  body="body text" type="info"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--info" body="body text" type="info">
      	<dd-icon class="dd-alert__type-icon" name="info"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('renders an error alert', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="error"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--error" body="body text"  type="error">
      	<dd-icon class="dd-alert__type-icon" name="error"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('renders a success alert', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="success"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--success" body="body text"  type="success">
      	<dd-icon class="dd-alert__type-icon" name="success"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('renders a warning alert', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="warning"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--warning" body="body text"  type="warning">
      	<dd-icon class="dd-alert__type-icon" name="warning"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('renders an alert that can be closed', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="info" allow-close></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--info" body="body text"  type="info" allow-close>
      	<dd-icon class="dd-alert__type-icon" name="info"></dd-icon>
      		<p class="dd-alert__body">body text</p>
				 <button aria-label="Sluit melding" class="dd-alert__close-btn">
					 <dd-icon name="cross"></dd-icon>
				 </button>
      </dd-alert>
    `);
  });

  it('renders a slotted body', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert  type="info"><span slot="body">custom body</span></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--info"  type="info">
      	<dd-icon class="dd-alert__type-icon" name="info"></dd-icon>
      		<p class="dd-alert__body"><span slot="body">custom body</span></p>
      </dd-alert>
    `);
  });

  it('dispatched a dismiss event when close is clicked', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="info"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--info" body="body text"  type="info">
      	<dd-icon class="dd-alert__type-icon" name="info"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('should render an error alert when the message type is passed', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="message"></dd-alert>`,
    });
    expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--error" body="body text"  type="message">
      	<dd-icon class="dd-alert__type-icon" name="error"></dd-icon>
      		<p class="dd-alert__body">body text</pc>
      </dd-alert>
    `);
  });

  it('should have a mutable type', async () => {
    const page = await newSpecPage({
      components: [DdAlert],
      html: `<dd-alert body="body text" type="error"></dd-alert>`,
    });
    page.rootInstance.type = 'info';
    expect(page.root).toMatchSnapshot();
  });

	it('should render text on a new line then the \n is passed into the body', async () => {
		const page = await newSpecPage({
			components: [DdAlert],
			html: `<dd-alert body="title text \n body" type="message"></dd-alert>`,
		});
		expect(page.root).toEqualHtml(`
      <dd-alert class="dd-alert dd-alert--error" body="title text \n body"  type="message">
      	<dd-icon class="dd-alert__type-icon" name="error"></dd-icon>
      		<p class="dd-alert__body">title text body</p>
      </dd-alert>
    `);
	});
});
