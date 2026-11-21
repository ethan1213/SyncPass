import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import Navbar from './Navbar';

const useSessionMock = vi.fn();
const useUserRolesMock = vi.fn();
const logout = vi.fn();
const loginWithRedirect = vi.fn();

vi.mock('../../auth/SessionProvider', () => ({ useSession: () => useSessionMock() }));
vi.mock('../../hooks/useRoles', () => ({
  useUserRoles: () => useUserRolesMock(),
  useHasRole: (...slugs: string[]) => useUserRolesMock().some((r: string) => slugs.includes(r)),
}));
vi.mock('wouter', () => ({
  Link: ({ children, href, className }: { children: React.ReactNode; href: string; className?: string }) =>
    React.createElement('a', { href, className }, children),
  useLocation: () => ['/'],
}));

beforeEach(() => {
  useSessionMock.mockReset();
  useUserRolesMock.mockReset();
  logout.mockReset();
  loginWithRedirect.mockReset();
  useUserRolesMock.mockReturnValue([]);
});
afterEach(() => cleanup());

it('oculta enlaces con rol y muestra botón de inicio de sesión cuando no hay usuario', () => {
  useSessionMock.mockReturnValue({ isAuthenticated: false, isLoading: false, user: null, loginWithRedirect, logout });
  render(React.createElement(Navbar, null));
  expect(screen.getByText('Inicio')).toBeTruthy();
  expect(screen.queryByText('Gestionar talleres')).toBeNull();
  expect(screen.queryByText('Gestión')).toBeNull();
  const entrar = screen.getByRole('button', { name: 'Iniciar sesión' });
  fireEvent.click(entrar);
  expect(loginWithRedirect).toHaveBeenCalled();
});

it('muestra enlaces de administrador y permite cerrar sesión', () => {
  useUserRolesMock.mockReturnValue(['admin']);
  useSessionMock.mockReturnValue({ isAuthenticated: true, isLoading: false, user: { name: 'Ethan', email: 'e@x.com' }, loginWithRedirect, logout });
  render(React.createElement(Navbar, { offlineCount: 2, isOfflineMode: true }));
  expect(screen.getByText('Gestionar talleres')).toBeTruthy();
  expect(screen.getByText('Ethan')).toBeTruthy();
  expect(screen.getByText('Sin conexión')).toBeTruthy();
  expect(screen.getByText('Gestión')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Salir' }));
  expect(logout).toHaveBeenCalled();
});

it('alterna el menú móvil al hacer clic en el botón de menú', () => {
  useSessionMock.mockReturnValue({ isAuthenticated: false, isLoading: false, user: null, loginWithRedirect, logout });
  render(React.createElement(Navbar, null));
  const boton = screen.getByRole('button', { name: 'Abrir menú' });
  expect(boton).toHaveProperty('ariaExpanded', 'false');
  fireEvent.click(boton);
  expect(screen.getByRole('button', { name: 'Cerrar menú' })).toBeTruthy();
});

it('muestra estado de carga de la sesión', () => {
  useSessionMock.mockReturnValue({ isAuthenticated: false, isLoading: true, user: null, loginWithRedirect, logout });
  render(React.createElement(Navbar, null));
  expect(screen.getByText('Cargando…')).toBeTruthy();
});
