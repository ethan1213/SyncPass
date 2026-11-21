import { describe, expect, it } from 'vitest';
import { isStaleVersionConflict } from './errors';

describe('isStaleVersionConflict', () => {
  it('reconoce el mensaje de conflicto que devuelve la API', () => {
    expect(isStaleVersionConflict('El registro cambió o ya existe. Recarga y revisa los datos.')).toBe(true);
  });

  it('ignora otros mensajes de error', () => {
    expect(isStaleVersionConflict('Tu sesión expiró. Inicia sesión.')).toBe(false);
  });
});
