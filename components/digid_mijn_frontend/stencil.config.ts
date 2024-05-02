import { Config } from '@stencil/core';
import { sass } from '@stencil/sass';
import { postcss } from '@stencil/postcss';
import autoprefixer from 'autoprefixer';

let globalScript: string = 'src/global/app.ts';

//Load a different globalScript when in DEV mode
const dev: boolean = process.argv && process.argv.indexOf('--dev') > -1;
if (dev) {
  globalScript = 'src/global/app-dev.ts';
}

export const config: Config = {
  invisiblePrehydration: false,
  globalStyle: 'src/scss/styles.scss',
  globalScript: globalScript,
  taskQueue: 'async',
  plugins: [
    sass(),
    postcss({
      plugins: [autoprefixer()],
    }),
  ],
  outputTargets: [
    {
      type: 'www',
      // comment the following line to disable service workers in production
      serviceWorker: null,
      baseUrl: 'https://myapp.local/',
    },
  ],
};
