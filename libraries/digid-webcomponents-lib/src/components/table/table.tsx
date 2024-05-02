import { Component, Host, Prop, h } from '@stencil/core';

@Component({tag: 'logius-table', styleUrl: 'table.css'})
export class Table {

  @Prop() tableTitle: string = "Table";
  @Prop() rows: Array<object> = [];
  @Prop() mapping: Object = {}

  renderString = (string: String) => string
  renderObject = (object: object) => (<ul> { Object.keys(object).map(key => <li> { key + ": " + object[key]} </li>) } </ul>)
  renderArray = (object: Array<object>) => ( <ul> { object.map(item => <li> { this.renderItem(item)} </li>) } </ul> )

  renderItem = (object: object) => (
    {
      "[object String]": this.renderString,
      "[object Object]": this.renderObject,
      "[object Array]": this.renderArray
    }[Object.prototype.toString.call(object)](object)
  )

  renderHead() {
    return (
      <thead>
        <tr> { Object.keys(this.mapping).map(header => (
          <th>{ header }</th> )) }
        </tr>
    </thead>)
  }

  renderBody(){
    return(
      <tbody>
        { this.rows.map((row) => (
          <tr>
            { Object.values(this.mapping).map(column => (
                <td>{this.renderItem(row[column])}</td>
              ))
            }
          </tr>
        ))}
      </tbody>
    )
  }

  render() {
    return (
      <Host>
        <h4> { this.tableTitle } </h4>
        <table>
           { this.renderHead() }
           { this.renderBody() }
        </table>
      </Host>
    );
  }
}
