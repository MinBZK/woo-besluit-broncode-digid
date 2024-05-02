import { Component, h } from '@stencil/core';

@Component({ tag: 'logius-spinner', styleUrl: 'spinner.css' })
export class Spinner {
    render() {
      return (
      <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <circle cx="50" cy="50" r="45"/>
      </svg>
      );
    }
}
