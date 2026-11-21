import { expect, it } from 'vitest';
import { SessionProvider, useSession } from './index';

it('re-exporta el contexto de sesión real', () => {
  expect(SessionProvider).toBeDefined();
  expect(useSession).toBeDefined();
  expect(typeof useSession).toBe('function');
});
