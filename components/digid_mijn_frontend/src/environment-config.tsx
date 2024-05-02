export interface EnvironmentConfig {
  dev: boolean;
}

export function setupConfig(config: EnvironmentConfig) {
  if (!window) {
    return;
  }

  const win = window as any;
  const DigiD = win.DigiD;

  if (DigiD && DigiD.config && DigiD.config.constructor.name !== 'Object') {
    console.error('DigiD config was already initialized');
    return;
  }

  win.DigiD = win.DigiD || {};
  win.DigiD.config = {
    ...win.DigiD.config,
    ...config,
  };

  return win.DigiD.config;
}
