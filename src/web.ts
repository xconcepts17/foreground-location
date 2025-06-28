import { WebPlugin } from '@capacitor/core';

import type { ForeGroundLocationPlugin } from './definitions';

export class ForeGroundLocationWeb extends WebPlugin implements ForeGroundLocationPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
