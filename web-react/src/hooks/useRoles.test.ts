import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useHasRole, useUserRoles } from './useRoles';
import { useSession } from '../auth/SessionProvider';
import { roleLabel } from '../config/auth';

vi.mock('../auth/SessionProvider', () => ({ useSession: vi.fn() }));
const session = vi.mocked(useSession);
beforeEach(() => session.mockReset());
describe('grupos de Cognito', () => {
  it('muestra solo nombres de rol válidos', () => {
    session.mockReturnValue({ user: { 'cognito:groups': ['monitor', 42, 'participante'] } } as ReturnType<typeof useSession>);
    expect(renderHook(() => useUserRoles()).result.current).toEqual(['monitor', 'participante']);
    expect(renderHook(() => useHasRole('admin', 'monitor')).result.current).toBe(true);
  });
  it('no concede permisos si faltan grupos', () => {
    session.mockReturnValue({ user: null } as ReturnType<typeof useSession>);
    expect(renderHook(() => useUserRoles()).result.current).toEqual([]);
    expect(renderHook(() => useHasRole('admin')).result.current).toBe(false);
  });
  it('ignora una claim que no sea lista', () => {
    session.mockReturnValue({ user: { 'cognito:groups': 'admin' } } as ReturnType<typeof useSession>);
    expect(renderHook(() => useUserRoles()).result.current).toEqual([]);
  });
  it('usa la etiqueta del perfil y conserva nombres desconocidos', () => {
    expect(roleLabel('monitor')).toContain('Monitor');
    expect(roleLabel('otro')).toBe('otro');
  });
});
