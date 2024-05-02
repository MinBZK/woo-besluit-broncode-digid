import { Event, EventEmitter, Component, h, Host, Prop} from '@stencil/core';
import { DropdownItem } from '../interfaces/DropdownItem';

@Component({tag: 'logius-dropdown', styleUrl: 'dropdown.css'})
export class Dropdown {

  @Prop() _id: string;
  @Prop() header: string = null;
  @Prop() hint: string = null;
  @Prop() options: Array<DropdownItem> = [];
  @Prop() selected: string;

  @Event() changeCallback: EventEmitter;
  triggerCallBack(event: Event) {
    this.changeCallback && this.changeCallback.emit(event);
  }

  render() {
    return (
      <Host>
        { this.header ? <h5>{ this.header }</h5> : null }
        { this.hint ? <p class="hint">{ this.hint }</p> : null }
        <select id={this._id} onInput={(e) => this.triggerCallBack(e)}>
          { this.options.map(item =>
            <option value={item.value} selected={this.selected === item.value}>{item.name}</option>
          )}
        </select>
      </Host>
    );
  }
}
