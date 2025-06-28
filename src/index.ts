import { registerPlugin } from '@capacitor/core';

import type { ForeGroundLocationPlugin } from './definitions';

const ForeGroundLocation = registerPlugin<ForeGroundLocationPlugin>('ForeGroundLocation', {
  web: () => import('./web').then((m) => new m.ForeGroundLocationWeb()),
});

export * from './definitions';
export { ForeGroundLocation };
