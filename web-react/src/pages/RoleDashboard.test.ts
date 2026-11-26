import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import RoleDashboard from './RoleDashboard';

const useUserRolesMock = vi.fn();
vi.mock('../hooks/useRoles', () => ({ useUserRoles: () => useUserRolesMock() }));
vi.mock('wouter', () => ({
  Link: ({ children, href, className }: { children: React.ReactNode; href: string; className?: string }) =>
    React.createElement('a', { href, className }, children),
}));

beforeEach(() => useUserRolesMock.mockReset());
afterEach(() => cleanup());

it('muestra las vistas del rol admin', () => {
  useUserRolesMock.mockReturnValue(['admin']);
  render(React.createElement(RoleDashboard));
  expect(screen.getByText('Administración')).toBeTruthy();
  expect(screen.getByText('Gestionar proyectos')).toBeTruthy();
});

it('muestra al participante sus talleres y no los mantenedores', () => {
  useUserRolesMock.mockReturnValue(['participante']);
  render(React.createElement(RoleDashboard));
  expect(screen.getByText('Mis talleres pendientes')).toBeTruthy();
  expect(screen.queryByText('Gestionar proyectos')).toBeNull();
});

it('indica perfil pendiente cuando no hay rol conocido', () => {
  useUserRolesMock.mockReturnValue([]);
  render(React.createElement(RoleDashboard));
  expect(screen.getByText('Perfil pendiente')).toBeTruthy();
  expect(screen.getByText(/Solicita al administrador/)).toBeTruthy();
});
