import { EnvironmentConfig } from './environment-config';

/* Service used to call the environment variables defined in app.ts and app-dev.ts

Call the variables with this logic:
import { EnvironmentConfigService } from '../../environment-config.service';
const dev: boolean = EnvironmentConfigService.getInstance().get('dev');
* */

export class EnvironmentConfigService {
  private static instance: EnvironmentConfigService;

  private m: Map<keyof EnvironmentConfig, any>;

  private constructor() {
    this.init();
  }

  static getInstance() {
    if (!EnvironmentConfigService.instance) {
      EnvironmentConfigService.instance = new EnvironmentConfigService();
    }
    return EnvironmentConfigService.instance;
  }

  private init() {
    if (!window) {
      return;
    }

    const win = window as any;
    const DigiD = win.DigiD;

    if (!DigiD) {
      return;
    }

    this.m = new Map<keyof EnvironmentConfig, any>(Object.entries(DigiD.config) as any);
  }

  get(key: keyof EnvironmentConfig, fallback?: any): any {
    if (!this.m) {
      return;
    }
    const value = this.m.get(key);
    return value !== undefined ? value : fallback;
  }
}
