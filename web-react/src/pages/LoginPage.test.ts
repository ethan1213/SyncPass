import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import LoginPage from './LoginPage';

const login = vi.fn();
const logout = vi.fn();
const useSessionMock = vi.fn();
vi.mock('../auth/SessionProvider', () => ({ useSession: () => useSessionMock() }));
vi.mock('../hooks/useRoles', () => ({ useUserRoles: () => ['admin'] }));
vi.mock('wouter', () => ({
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => React.createElement('a', { href }, children),
}));

beforeEach(() => {
  login.mockReset();
  logout.mockReset();
  useSessionMock.mockReset();
});
afterEach(() => cleanup());

it('envía el formulario de acceso con usuario y contraseña', async () => {
  login.mockResolvedValue(undefined);
  useSessionMock.mockReturnValue({ isAuthenticated: false, login, logout, configured: true, user: null });
  render(React.createElement(LoginPage));
  fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ethan' } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'secret123' } });
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
  await waitFor(() => expect(login).toHaveBeenCalledWith('ethan', 'secret123'));
});

it('muestra el error cuando el login falla', async () => {
  login.mockRejectedValue(new Error('Credenciales inválidas.'));
  useSessionMock.mockReturnValue({ isAuthenticated: false, login, logout, configured: true, user: null });
  render(React.createElement(LoginPage));
  fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ethan' } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'badpass' } });
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Credenciales inválidas.');
});

it('muestra el estado de sesión iniciada y permite cerrar sesión', () => {
  useSessionMock.mockReturnValue({ isAuthenticated: true, login, logout, configured: true, user: { email: 'ethan@x.com' } });
  render(React.createElement(LoginPage));
  expect(screen.getByText('Sesión iniciada correctamente.')).toBeTruthy();
  fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
  expect(logout).toHaveBeenCalled();
});
