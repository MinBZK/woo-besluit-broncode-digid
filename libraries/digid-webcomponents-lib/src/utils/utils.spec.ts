import { format } from './utils';

describe('format', () => {
  it('returns empty string for no names defined', () => {
    expect(format(undefined, undefined, undefined)).toEqual('');
  });

  it('formats just first names', () => {
    expect(format('PPPPPP', undefined, undefined)).toEqual('PPPPPP');
  });

  it('formats first and last names', () => {
    expect(format('PPPPPP', undefined, 'PPPPPPPP')).toEqual('PPPPPPPPPPPPPPP');
  });

  it('formats first, middle and last names', () => {
    expect(format('PPPPPP', 'PPPPPP', 'PPPPPPPP')).toEqual('PPPPPPPPPPPPPPPPPPPPPP');
  });
});
