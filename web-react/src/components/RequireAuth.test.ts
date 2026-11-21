import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import RequireAuth from './RequireAuth';

const loginWithRedirect = vi.fn();
const useSessionMock = vi.fn();
const useUserRolesMock = vi.fn();
vi.mock('../auth/SessionProvider', () => ({ useSession: () => useSessionMock() }));
vi.mock('../hooks/useRoles', () => ({ useUserRoles: () => useUserRolesMock() }));

beforeEach(() => {
  loginWithRedirect.mockReset();
  useSessionMock.mockReset();
  useUserRolesMock.mockReset();
  useUserRolesMock.mockReturnValue([]);
});
afterEach(() => cleanup());

it('muestra estado de carga mientras verifica la sesión', () => {
  useSessionMock.mockReturnValue({ isLoading: true, isAuthenticated: false, loginWithRedirect });
  render(React.createElement(RequireAuth, null, React.createElement('p', null, 'secreto')));
  expect(screen.getByText('Verificando tu sesión…')).toBeTruthy();
});

it('pide iniciar sesión cuando no hay usuario autenticado', () => {
  useSessionMock.mockReturnValue({ isLoading: false, isAuthenticated: false, loginWithRedirect });
  render(React.createElement(RequireAuth, null, React.createElement('p', null, 'secreto')));
  expect(screen.getByText('Necesitas iniciar sesión')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
  expect(loginWithRedirect).toHaveBeenCalled();
});

it('bloquea por rol cuando el usuario no tiene el perfil requerido', () => {
  useSessionMock.mockReturnValue({ isLoading: false, isAuthenticated: true, loginWithRedirect });
  useUserRolesMock.mockReturnValue([]);
  render(React.createElement(RequireAuth, { roles: ['admin'] }, React.createElement('p', null, 'secreto')));
  expect(screen.getByText('Acceso denegado')).toBeTruthy();
  expect(screen.getByText(/ningún rol asignado/)).toBeTruthy();
});

it('muestra el contenido protegido cuando el usuario cumple el rol', () => {
  useSessionMock.mockReturnValue({ isLoading: false, isAuthenticated: true, loginWithRedirect });
  useUserRolesMock.mockReturnValue(['admin']);
  render(React.createElement(RequireAuth, { roles: ['admin'] }, React.createElement('p', null, 'secreto')));
  expect(screen.getByText('secreto')).toBeTruthy();
});
