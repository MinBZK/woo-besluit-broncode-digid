export default class BackdropManager {
  private readonly backdrop: HTMLDdBackdropElement;

  private constructor() {
    this.backdrop = document.createElement('dd-backdrop');
    document.body.appendChild(this.backdrop);
  }

  static getInstance(): BackdropManager {
    let backdropEl: any = document.querySelector('dd-backdrop');

    if (!backdropEl) {
      const backdropManager = new BackdropManager();

      backdropEl = document.querySelector('dd-backdrop');
      backdropEl.instance = backdropManager;
    }

    return backdropEl.instance;
  }

  public showElement(element: HTMLElement) {
    return this.backdrop.showElement(element);
  }

  public hideElement(element: HTMLElement) {
    return this.backdrop.hideElement(element);
  }
}
