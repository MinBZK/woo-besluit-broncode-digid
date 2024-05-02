import { newSpecPage } from '@stencil/core/testing';
import { DdInput } from './dd-input';

describe('dd-input', () => {
  it('renders the placeholder', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" placeholder="Tijdelijke aanduiding"></dd-input>`,
    });
    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input" type="text" placeholder="Tijdelijke aanduiding">
				<input aria-label="Tijdelijke aanduiding" id="dd-input-1" class="dd-input__input" placeholder="Tijdelijke aanduiding" type="text" />
			</dd-input>
    `);
  });

  it('renders the label', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" label="Input label"></dd-input>`,
    });
    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input" type="text" label="Input label">
				<label htmlfor="dd-input-2">Input label</label>
				<input aria-label="Input label" id="dd-input-2" class="dd-input__input" type="text" />
			</dd-input>
    `);
  });

  it('renders the label with a hint', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" hint="Hint text" label="Input label"></dd-input>`,
    });
    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input dd-input--has-hint" type="text" hint="Hint text" label="Input label">
			  <label htmlfor="dd-input-3">
          Input label
        </label>
        <span class="dd-input__hint">
          Hint text
        </span>
				<input aria-label="Input label" id="dd-input-3" class="dd-input__input" type="text" />
			</dd-input>
    `);
  });

  it('renders the clear button when input has a value', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" allow-clear="true" value="dummy"></dd-input>`,
    });

    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input dd-input--allow-clear" type="text" allow-clear="true" value="dummy">
				<input id="dd-input-4" class="dd-input__input" type="text" value="dummy" />
			  <button aria-label="Invoer verwijderen" type="button" class="dd-input__clear-btn">
          <dd-icon class="dd-input__clear-btn__icon" name="cross"></dd-icon>
        </button>
			</dd-input>
    `);
  });

  it('renders the error class', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" has-error="true"></dd-input>`,
    });
    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input dd-input--has-error" type="text" has-error="true">
				<input id="dd-input-5" class="dd-input__input" type="text" />
			</dd-input>
    `);
  });

  it('emits input and clear event when clicking on clear', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" placeholder="Tijdelijke aanduiding" value="abba" allow-clear="true"></dd-input>`,
    });
    const clearButton = page.body.querySelector('button');
    const clearEventSpy = jest.spyOn(page.rootInstance.clear, 'emit');
    const inputEventHandlerSpy = jest.fn();

    page.body.addEventListener('input', inputEventHandlerSpy);

    expect(page.root.value).toBe('abba');

    await clearButton.dispatchEvent(new MouseEvent('click'));

    expect(clearEventSpy).toHaveBeenCalled();
    expect(inputEventHandlerSpy).toHaveBeenCalled();
    expect(page.root.value).toBe('');
  });

  it('does not render the clear button when input has an empty value', async () => {
    const page = await newSpecPage({
      components: [DdInput],
      html: `<dd-input type="text" allow-clear="true" value=""></dd-input>`,
    });

    expect(page.root).toEqualHtml(`
			<dd-input class="dd-input dd-input--allow-clear" type="text" allow-clear="true" value="">
				<input id="dd-input-7" class="dd-input__input" type="text" value="" />
			</dd-input>
    `);
  });
});
