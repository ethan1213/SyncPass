import { describe, expect, it } from 'vitest';
import { formatRoleList } from './roles';

describe('formatRoleList', () => {
  it('traduce los roles crudos a etiquetas y los une con el separador por defecto', () => {
    expect(formatRoleList(['admin', 'monitor'])).toBe('Administrador · Estudiante voluntario / Monitor');
  });

  it('acepta un separador distinto', () => {
    expect(formatRoleList(['admin', 'coordinador'], ' o ')).toBe('Administrador o Docente / Coordinador(a)');
  });

  it('devuelve cadena vacía cuando no hay roles', () => {
    expect(formatRoleList([])).toBe('');
  });

  it('conserva el rol crudo cuando no hay etiqueta conocida', () => {
    expect(formatRoleList(['invitado'])).toBe('invitado');
  });
});
