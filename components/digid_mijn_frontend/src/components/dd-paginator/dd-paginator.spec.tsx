import { newSpecPage } from '@stencil/core/testing';
import { DdPaginator } from './dd-paginator';

window.addEventListener = jest.fn();
window.removeEventListener = jest.fn();

describe('dd-paginator', () => {
  it('renders', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="7" current-page="3"></dd-paginator>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('renders with ellipses at the end', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="12" current-page="4"></dd-paginator>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('renders with ellipses at the start', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="12" current-page="9"></dd-paginator>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('renders with ellipses at the start and end', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="12" current-page="7"></dd-paginator>`,
    });
    expect(page.root).toMatchSnapshot();
  });

  it('emits pageChange event when paging back and forward', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="12" current-page="7"></dd-paginator>`,
    });
    page.rootInstance.el.addEventListener = jest.fn();
    const prevBtn = page.body.querySelector('li:first-of-type button');
    const nextBtn = page.body.querySelector('li:last-of-type button');
    const pageChangeSpy = jest.spyOn(page.rootInstance.pageChange, 'emit');

    expect(page.root.currentPage).toBe(7);

    prevBtn.dispatchEvent(new MouseEvent('click'));

    expect(page.root.currentPage).toBe(6);
    expect(pageChangeSpy).toHaveBeenCalledWith(6);

    nextBtn.dispatchEvent(new MouseEvent('click'));
    nextBtn.dispatchEvent(new MouseEvent('click'));

    expect(page.root.currentPage).toBe(8);
    expect(pageChangeSpy).toHaveBeenCalledWith(8);
  });

  it('emits pageChange event when clicking on a specific page', async () => {
    const page = await newSpecPage({
      components: [DdPaginator],
      html: `<dd-paginator total-pages="12" current-page="4"></dd-paginator>`,
    });
    page.rootInstance.el.addEventListener = jest.fn();
    const secondPageBtn = page.body.querySelector('li:nth-child(3) button');
    const pageChangeSpy = jest.spyOn(page.rootInstance.pageChange, 'emit');

    expect(page.root.currentPage).toBe(4);

    secondPageBtn.dispatchEvent(new MouseEvent('click'));

    expect(pageChangeSpy).toHaveBeenCalledWith(2);

    expect(page.root.currentPage).toBe(2);
  });
});
