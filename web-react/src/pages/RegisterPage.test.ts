import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import RegisterPage from './RegisterPage';

const register = vi.fn();
const confirm = vi.fn();
const resend = vi.fn();
const login = vi.fn();
const navigate = vi.fn();
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ register, confirm, resend, login, configured: true }) }));
vi.mock('wouter', () => ({
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => React.createElement('a', { href }, children),
  useLocation: () => ['/registro', navigate],
}));

beforeEach(() => {
  register.mockReset();
  confirm.mockReset();
  resend.mockReset();
  login.mockReset();
  navigate.mockReset();
});
afterEach(() => cleanup());

function fillForm() {
  fireEvent.change(screen.getByLabelText('Usuario'), { target: { value: 'ethan' } });
  fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'ethan@x.com' } });
  fireEvent.change(screen.getByLabelText('Apodo'), { target: { value: 'Ethan' } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'Secret123!' } });
}

it('pide un código de verificación cuando el usuario no queda confirmado', async () => {
  register.mockResolvedValue({ userConfirmed: false, destination: 'e***@x.com' });
  render(React.createElement(RegisterPage));
  fillForm();
  fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
  await waitFor(() => expect(register).toHaveBeenCalled());
  expect(await screen.findByLabelText('Código')).toBeTruthy();
  expect(screen.getByText(/e\*\*\*@x.com/)).toBeTruthy();
});

it('confirma el código y navega al login', async () => {
  register.mockResolvedValue({ userConfirmed: false, destination: 'ethan@x.com' });
  confirm.mockResolvedValue(undefined);
  login.mockResolvedValue(undefined);
  render(React.createElement(RegisterPage));
  fillForm();
  fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
  await screen.findByLabelText('Código');
  fireEvent.change(screen.getByLabelText('Código'), { target: { value: '123456' } });
  fireEvent.click(screen.getByRole('button', { name: 'Confirmar e ingresar' }));
  await waitFor(() => expect(confirm).toHaveBeenCalledWith('ethan', '123456'));
  await waitFor(() => expect(navigate).toHaveBeenCalledWith('/login'));
});

it('inicia sesión directo y navega cuando el registro queda confirmado', async () => {
  register.mockResolvedValue({ userConfirmed: true, destination: 'ethan@x.com' });
  login.mockResolvedValue(undefined);
  render(React.createElement(RegisterPage));
  fillForm();
  fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
  await waitFor(() => expect(login).toHaveBeenCalledWith('ethan', 'Secret123!'));
  await waitFor(() => expect(navigate).toHaveBeenCalledWith('/login'));
});

it('muestra el error cuando el registro falla', async () => {
  register.mockRejectedValue(new Error('El usuario ya existe.'));
  render(React.createElement(RegisterPage));
  fillForm();
  fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'El usuario ya existe.');
});
