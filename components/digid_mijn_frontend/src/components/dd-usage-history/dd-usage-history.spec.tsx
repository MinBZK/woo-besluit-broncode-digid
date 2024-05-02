import getHistory from './fixtures/getHistory';

jest.mock('../../api/services/dd.history.service.ts', () => ({
  getHistory: jest.fn().mockImplementation(() => getHistory),
}));

jest.mock('../../api/services/dd.session.service.ts', () => ({
  updateSession: jest.fn().mockImplementation(() => {}),
}));

import { newSpecPage } from '@stencil/core/testing';
import { DdUsageHistory } from './dd-usage-history';
import { h } from '@stencil/core';
import { mockedUserHistoryResponse } from '../../global/dev-data';
import historyService from '../../api/services/dd.history.service';

describe('dd-usage-history', () => {
  const mockedArray = mockedUserHistoryResponse.account_logs;

  fit('Should call the navigation method after a button click', async () => {
    const page = await newSpecPage({
      components: [DdUsageHistory],
      template: () => {
        return <dd-usage-history />;
      },
    });

    const btn: HTMLElement = page.body.querySelector('dd-button');
    //const navigateLoginOptionsSpy = jest.spyOn(page.rootInstance, 'navigateLoginOptions');
    const navigateLoginOptionsSpy = jest.spyOn(page.rootInstance.navigationClicked, 'emit');

    btn.click();
    await page.waitForChanges();

    expect(navigateLoginOptionsSpy).toHaveBeenCalled();
  });

  it('Should call the usage history and assign it to the historyArray', async () => {
    return historyService.getHistory('1', '').then(data => expect(data).toEqual(mockedUserHistoryResponse));
  });

  it('Should render a table with the mocked array', async () => {
    const page = await newSpecPage({
      components: [DdUsageHistory],
      template: () => {
        return <dd-usage-history />;
      },
    });
    page.rootInstance.tempArray = mockedArray;
    await page.waitForChanges();
    expect(page.root).toMatchSnapshot();
  });

  it('Should parse the ISO string into a data and time value', async () => {
    const page = await newSpecPage({
      components: [DdUsageHistory],
      template: () => {
        return <dd-usage-history />;
      },
    });
    const parsedMockedArray = page.rootInstance.parseHistoryArray(mockedArray);
    expect(parsedMockedArray[0].date).toBe('do 25 mrt 2021');
    expect(parsedMockedArray[0].time).toBe('18:02:56');
  });

  // it('Should filter the array when a filter is given', async () => {
  //   const page = await newSpecPage({
  //     components: [DdUsageHistory],
  //     template: () => {
  //       return <dd-usage-history />;
  //     },
  //   });
  //   const mockedResponse = { id: 7, name: 'Inloggen beheermodule gelukt', created_at: '2021-03-25T18:03:08.000+01:00' };
  //   // (historyService.filterHistory as jest.MockedFunction<typeof historyService.filterHistory>).mockResolvedValueOnce(
  //   //   mockedResponse,
  //   // );
  //
  //   const filterEvent: CustomEvent = { detail: 'Inloggen beheermodule gelukt' } as any;
  //   const actualValue = await page.rootInstance.filterContent(filterEvent);
  //   const filterSpy = jest.spyOn(page.rootInstance, 'filterContent');
  //
  //   expect(actualValue).toBeUndefined();
  //   expect(filterSpy).toHaveBeenCalledWith(mockedResponse);
  // });

  // it('Should call the listen functions after an event has been emitted', async () => {
  //   const page = await newSpecPage({
  //     components: [DdUsageHistory],
  //     template: () => {
  //       return <dd-usage-history />;
  //     },
  //   });
  //   const array = mockedArray;
  //   page.rootInstance.paginationChanged({ detail: 1 } as CustomEvent);
  //   page.rootInstance.filterContent({ detail: 'logging' } as CustomEvent);
  //
  //   expect(page.rootInstance.sliceArray).toHaveBeenCalled();
  //   expect(page.rootInstance.filterArray).toHaveBeenCalled();
  // });
});
