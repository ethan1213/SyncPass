import { describe, expect, it } from 'vitest';
import { isStaleVersionConflict } from './errors';

describe('isStaleVersionConflict', () => {
  it('reconoce el mensaje de conflicto que devuelve la API', () => {
    expect(isStaleVersionConflict(new Error('El registro cambió o ya existe. Recarga y revisa los datos.'))).toBe(true);
    expect(isStaleVersionConflict({ name: 'ApiHttpError', message: 'Otra sesión guardó cambios.', code: 'STALE_VERSION' })).toBe(true);
  });

  it('ignora otros mensajes de error', () => {
    expect(isStaleVersionConflict(new Error('Tu sesión expiró. Inicia sesión.'))).toBe(false);
    expect(isStaleVersionConflict({ name: 'ApiHttpError', message: 'El registro cambió.', code: 'CONFLICT' })).toBe(false);
  });
});
