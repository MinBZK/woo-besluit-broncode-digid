import { Component, Host, h, Prop, Event, EventEmitter, Method } from '@stencil/core';
import uniqueId from 'lodash.uniqueid';
import i18next from '../../../utils/i18next';

@Component({
  tag: 'dd-input',
})
export class DdInput {
  /**
   * The unique id used for the 'for' attribute on label and 'id' attribute on the input.
   */
  private readonly id;
  private inputEl: HTMLInputElement;

  private numbersOnlyRegex = new RegExp('^[0-9]*$');

  /**
   * Toggles whether or not to display the close icon.
   * @default false
   */
  @Prop() allowClear: boolean = false;

  /**
   * Toggles whether or not to add the '.dd-input--has-error' class.
   * @default false
   */
  @Prop() hasError: boolean = false;

  /**
   * Whether the built in browser's autocomplete should show or not.
   */
  @Prop() disableAutoComplete: boolean = false;

  /**
   * The label to show above the input field.
   */
  @Prop() label: string;

  /**
   * The secondary, slightly lighter, label to show next to the primary label.
   */
  @Prop() secondaryLabel: string;

  /**
   * The name to apply to the input field.
   */
  @Prop() name: string;

  /**
   * The hint text to display between the label and input.
   */
  @Prop() hint: string;

  /**
   * The placeholder to display when no input is showing.
   */
  @Prop() placeholder: string;

  /**
   * The type of the input.
   */
  @Prop() type: string = 'text';

  /**
   * The maximum character allowed for the input.
   */
  @Prop() maxLength: number;

  /**
   * The aria label to show, as a fallback the label and secondary label will be used or the placeholder text.
   */
  @Prop({ mutable: true }) inputAriaLabel: string;

  /**
   * The value of the input.
   */
  @Prop({ reflect: true, mutable: true }) value;

  /**
   * Dispatched the clear event when the X is clicked.
   */
  @Event() clear: EventEmitter;

  constructor() {
    this.id = uniqueId();
  }

  /**
   * Clears the input internally and for the nested input element.
   */
  @Method()
  async clearInput() {
    this.value = '';
    this.inputEl.value = '';
  }

  private inputInputHandler(event) {
    this.value = event.target.value;
  }

  private onKeyDownHandler(event) {
    if (this.type === 'numbersOnly') {
      const keyIsNumber = !this.numbersOnlyRegex.test(event.key);
      const keyIsAllowed =
        ['ArrowLeft', 'Tab', 'Shift', 'ArrowRight', 'Alt', 'Delete', 'Control', 'Meta', 'Backspace'].indexOf(
          event.key,
        ) >= 0;
      const isPastingOrCopying = (event.metaKey || event.ctrlKey) && (event.key === 'v' || event.key === 'c');

      if (keyIsNumber && !keyIsAllowed && !isPastingOrCopying) {
        event.preventDefault();
      }
    }
  }

  private onPasteHandler(event) {
    if (this.type === 'numbersOnly') {
      let pasteValue = (event.clipboardData || (window as any).clipboardData).getData('text');

      if (pasteValue && !this.numbersOnlyRegex.test(pasteValue.trim())) {
        event.preventDefault();
      }
    }
  }

  componentWillLoad() {
    if (!this.inputAriaLabel) {
      const ariaLabel = `${this.label || ''} ${this.secondaryLabel || ''}`.trim();

      if (ariaLabel) {
        this.inputAriaLabel = ariaLabel;
      } else if (this.placeholder) {
        this.inputAriaLabel = this.placeholder;
      }
    }

    if (this.type === 'numbersOnly') {
      this.disableAutoComplete = true;
    }
  }

  private async handleClearClicked() {
    const event = new window.Event('input', {
      bubbles: true,
      cancelable: true,
    });

    await this.clearInput();

    this.clear.emit();
    this.inputEl.dispatchEvent(event);
  }

  render() {
    return (
      <Host
        class={{
          'dd-input': true,
          'dd-input--has-hint': !!this.hint,
          'dd-input--has-error': !!this.hasError,
          'dd-input--allow-clear': !!this.allowClear,
        }}
      >
        {(this.label || this.secondaryLabel) && (
          <label htmlFor={`dd-input-${this.id}`}>
            {this.label}
            {this.secondaryLabel && <span class="dd-input__secondary-label"> {this.secondaryLabel}</span>}
          </label>
        )}
        {this.hint && <span class="dd-input__hint">{this.hint}</span>}
        <input
          id={`dd-input-${this.id}`}
          class="dd-input__input"
          placeholder={this.placeholder}
          type={this.type === 'numbersOnly' ? 'text' : this.type}
          value={this.value}
          onInput={event => this.inputInputHandler(event)}
          onKeyDown={event => this.onKeyDownHandler(event)}
          onPaste={event => this.onPasteHandler(event)}
          name={this.name}
          autocomplete={this.disableAutoComplete ? 'off' : null}
          maxlength={this.maxLength}
          aria-label={this.inputAriaLabel}
          ref={el => (this.inputEl = el as HTMLInputElement)}
        />
        {this.allowClear && !!this.value && (
          <button
            type="button"
            class="dd-input__clear-btn"
            onClick={() => this.handleClearClicked()}
            aria-label={i18next.t('input.clear-aria-label')}
          >
            <dd-icon class="dd-input__clear-btn__icon" name="cross" showMargin={false} />
          </button>
        )}
      </Host>
    );
  }
}
