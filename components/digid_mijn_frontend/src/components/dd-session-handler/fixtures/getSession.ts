import { addSeconds } from 'date-fns';

export const warningSessionTimeResponse = Promise.resolve({
  timestamp: addSeconds(new Date(), 30),
});

export const timeoutSessionTimeResponse = Promise.resolve({
  timestamp: addSeconds(new Date(), -30),
});

export const staticSessionTimeResponse = Promise.resolve({
  timestamp: new Date('2022-02-25T12:31:07.563Z'),
});
