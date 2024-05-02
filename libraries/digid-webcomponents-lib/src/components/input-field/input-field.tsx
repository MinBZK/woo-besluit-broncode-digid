import { Event, EventEmitter, Component, h, Host, Prop} from '@stencil/core';

@Component({tag: 'logius-input-field', styleUrl: 'input-field.css'})
export class InputField {

  @Prop() _id: string;
  @Prop() header: string = null;
  @Prop() hint: string = null;
  @Prop() currentValue: string;

  @Event() changeCallback: EventEmitter;
  triggerCallBack(event: Event) {
    this.changeCallback && this.changeCallback.emit(event);
  }

  render() {
    return (
      <Host>
        { this.header ? <h5>{ this.header }</h5> : null }
        { this.hint ? <p class="hint">{ this.hint }</p> : null }
        <input id={this._id} value={this.currentValue} onChange={(e) => this.triggerCallBack(e)}></input>
      </Host>
    );
  }
}
