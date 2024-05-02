import { Component, Host, h, Method, State } from '@stencil/core';

@Component({
  tag: 'dd-toast',
})
export class DdToast {
  //Message sent to create a toast with
  @State() toast: { message: string; type: string };
  //Array of toast messages
  @State() toastArray: Array<any> = [];

  //Array of toast messages
  @State() methods: Array<any> = [];

  @Method()
  async pushMessage(body, methods?) {
    //Assign the body to a state to trigger a rerender
    this.toast = body;

    this.methods = methods;

    //Only push the message if it's not in the array already
    if (this.toastArray.length === 0 || this.toastArray.some(toast => toast.message !== this.toast.message)) {
      this.toastArray.push(this.toast);
    }
  }

  @Method()
  async clearMessages() {
    //Assign the body to a state to trigger a rerender
    this.toastArray = [];
  }

  render() {
    return (
      <Host class="dd-toast" role="alert">
        {this.toastArray.map(item => (
          <dd-alert
            class="dd-toast__msg"
            type={item.type}
            body={item.message}
            alert-buttons={this.methods}
            allow-close={true}
          />
        ))}
      </Host>
    );
  }
}
