import { Config } from '@stencil/core';

export const config: Config = {
  namespace: 'digid-webcomponents-lib',
  outputTargets: [
    {
      type: 'dist',
      esmLoaderPath: '../loader',
    },
    {
      type: 'dist-custom-elements-bundle',
    },
    {
      type: 'docs-readme',
      strict: true
    }
  ],
};
