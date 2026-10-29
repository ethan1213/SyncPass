import React from 'react';
import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionProvider, useSession } from './SessionProvider';
import { request } from '../clients/api';

vi.mock('../clients/api', () => ({ request: vi.fn() }));
const requestMock = vi.mocked(request);
const idToken = `a.${btoa(JSON.stringify({ sub: 'usuario-demo', 'cognito:groups': ['monitor'] }))}.c`;
const tokens = { accessToken: 'acceso-demo', idToken, refreshToken: 'refresco-demo', expiresIn: 3600 };
const wrapper = ({ children }: { children: React.ReactNode }) => React.createElement(SessionProvider, null, children);

beforeEach(() => { sessionStorage.clear(); requestMock.mockReset(); });
describe('sesión web mediante Gateway', () => {
  it('inicia sesión, conserva tokens y expone el grupo del usuario', async () => {
    requestMock.mockResolvedValueOnce(tokens);
    const { result } = renderHook(() => useSession(), { wrapper });
    await act(async () => result.current.login('ethan', 'clave-demo'));
    expect(requestMock).toHaveBeenCalledWith('/api/auth/login',
      { method: 'POST', body: { username: 'ethan', password: 'clave-demo' } });
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.['cognito:groups']).toEqual(['monitor']);
    expect(await result.current.getAccessTokenSilently()).toBe('acceso-demo');
    expect(sessionStorage.getItem('syncpass.session')).toContain('acceso-demo');
  });
  it('renueva por Gateway cuando expira el token', async () => {
    requestMock.mockResolvedValueOnce({ ...tokens, expiresIn: 0 })
      .mockResolvedValueOnce({ ...tokens, accessToken: 'acceso-renovado' });
    const { result } = renderHook(() => useSession(), { wrapper });
    await act(async () => result.current.login('ethan', 'clave-demo'));
    let access = '';
    await act(async () => { access = await result.current.getAccessTokenSilently(); });
    expect(access).toBe('acceso-renovado');
    expect(requestMock).toHaveBeenLastCalledWith('/api/auth/refresh',
      { method: 'POST', body: { refreshToken: 'refresco-demo' } });
  });
  it('borra la sesión cuando falla la renovación', async () => {
    requestMock.mockResolvedValueOnce({ ...tokens, expiresIn: 0 }).mockRejectedValueOnce(Error('sin red'));
    const { result } = renderHook(() => useSession(), { wrapper });
    await act(async () => result.current.login('ethan', 'clave-demo'));
    await act(async () => { await expect(result.current.getAccessTokenSilently()).rejects.toThrow('Tu sesión expiró'); });
    expect(sessionStorage.getItem('syncpass.session')).toBeNull();
  });
  it('exige login antes de consultar recursos protegidos', async () => {
    const { result } = renderHook(() => useSession(), { wrapper });
    await expect(result.current.getAccessTokenSilently()).rejects.toThrow('Inicia sesión');
  });
  it('envía registro, confirmación y reenvío por la API', async () => {
    requestMock.mockResolvedValue({ userConfirmed: false, destination: 'e***@demo.cl' });
    const { result } = renderHook(() => useSession(), { wrapper });
    await result.current.register({ username: 'demo', email: 'e@demo.cl', nickname: 'Demo', password: 'Clave123#' });
    await result.current.confirm('demo', '123456');
    await result.current.resend('demo');
    expect(requestMock.mock.calls.map(call => call[0])).toEqual(['/api/auth/register', '/api/auth/confirm', '/api/auth/resend']);
  });
});
